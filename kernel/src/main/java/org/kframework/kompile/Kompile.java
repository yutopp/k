package org.kframework.kompile;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.kframework.Collections;
import org.kframework.attributes.Att;
import org.kframework.builtin.Sorts;
import org.kframework.compile.ConfigurationInfo;
import org.kframework.compile.ConfigurationInfoFromModule;
import org.kframework.compile.StrictToHeatingCooling;
import org.kframework.definition.Bubble;
import org.kframework.definition.Configuration;
import org.kframework.definition.Definition;
import org.kframework.definition.Module;
import org.kframework.definition.ProductionItem;
import org.kframework.definition.Sentence;
import org.kframework.attributes.Source;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.compile.GenerateSentencesFromConfigDecl;
import org.kframework.parser.Term;
import org.kframework.parser.TreeNodesToKORE;
import org.kframework.parser.concrete2kore.ParseInModule;
import org.kframework.parser.concrete2kore.ParserUtils;
import org.kframework.parser.concrete2kore.generator.RuleGrammarGenerator;
import org.kframework.utils.StringUtil;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.errorsystem.ParseFailedException;
import org.kframework.utils.file.FileUtil;
import scala.Option;
import scala.Tuple2;
import scala.collection.Seq;
import scala.collection.immutable.Set;
import scala.util.Either;

import static org.kframework.Collections.*;
import static org.kframework.kore.KORE.*;
import static org.kframework.definition.Constructors.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Kompile {

    public static final File BUILTIN_DIRECTORY = new File(new File("k-distribution/include/builtin").getAbsoluteFile()
            .toString().replace("k-distribution" + File.separator + "k-distribution", "k-distribution"));
    private static final String REQUIRE_KAST_K = "requires \"kast.k\"\n";
    private static final String mainModule = "K";
    private static final String startSymbol = "RuleContent";

    private final FileUtil files;
    private final ParserUtils parser;

    public RuleGrammarGenerator makeRuleGrammarGenerator() {
        String definitionText;
        File definitionFile = new File(BUILTIN_DIRECTORY.toString() + "/kast.k");
        definitionText = files.loadFromWorkingDirectory(definitionFile.getPath());

        //Definition baseK = ParserUtils.parseMainModuleOuterSyntax(definitionText, mainModule);
        java.util.Set<Module> modules =
                parser.loadModules(definitionText,
                        Source.apply(definitionFile.getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY));

        Definition baseK = Definition(immutable(modules));
        return new RuleGrammarGenerator(baseK);
    }

    @Inject
    public Kompile(FileUtil files) {
        this.files = files;
        this.parser = new ParserUtils(files);
    }

    // todo: rename and refactor this
    public Tuple2<Module, BiFunction<String, Source, K>> run(File definitionFile, String mainModuleName, String mainProgramsModule, String programStartSymbol) {
        String definitionString = files.loadFromWorkingDirectory(definitionFile.getPath());

        java.util.Set<Module> modules =
                parser.loadModules(REQUIRE_KAST_K + definitionString,
                        Source.apply(definitionFile.getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY));

        Definition definition = Definition(immutable(modules));

        Module mainModuleWithBubble = stream(definition.modules()).filter(m -> m.name().equals(mainModuleName)).findFirst().get();

        boolean hasConfigDecl = stream(mainModuleWithBubble.sentences())
                .filter(s -> s instanceof Bubble)
                .map(b -> (Bubble) b)
                .filter(b -> b.sentenceType().equals("config"))
                .findFirst().isPresent();

        if (!hasConfigDecl) {
            mainModuleWithBubble = Module(mainModuleName, (Set<Module>)mainModuleWithBubble.imports().$plus(definition.getModule("DEFAULT-CONFIGURATION").get()), mainModuleWithBubble.localSentences(), mainModuleWithBubble.att());
        }

        RuleGrammarGenerator gen = makeRuleGrammarGenerator();
        ParseInModule configParser = gen.getConfigGrammar(mainModuleWithBubble);

        Map<Bubble, Module> configDecls = new HashMap<>();
        Optional<Bubble> configBubbleMainModule = stream(mainModuleWithBubble.localSentences())
                .filter(s -> s instanceof Bubble)
                .map(b -> (Bubble) b)
                .filter(b -> b.sentenceType().equals("config"))
                .findFirst();
        if (configBubbleMainModule.isPresent()) {
            configDecls.put(configBubbleMainModule.get(), mainModuleWithBubble);
        }
        for (Module mod : iterable(mainModuleWithBubble.importedModules())) {
            Optional<Bubble> configBubble = stream(mod.localSentences())
                    .filter(s -> s instanceof Bubble)
                    .map(b -> (Bubble) b)
                    .filter(b -> b.sentenceType().equals("config"))
                    .findFirst();
            if (configBubble.isPresent()) {
                configDecls.put(configBubble.get(), mod);
            }
        }
        if (configDecls.size() > 1) {
            throw KExceptionManager.compilerError("Found more than one configuration in definition: " + configDecls);
        }
        if (configDecls.size() == 0) {
            throw KExceptionManager.compilerError("Unexpected lack of default configuration and no configuration present: bad prelude?");
        }

        Map.Entry<Bubble, Module> configDeclBubble = configDecls.entrySet().iterator().next();

        java.util.Set<ParseFailedException> errors = Sets.newHashSet();

        K _true = KToken(Sort("Bool"), "true");

        Optional<Configuration> configDeclOpt = configDecls.keySet().stream()
                .parallel()
                .map(b -> {
                    int startLine = b.att().<Integer>get("contentStartLine").get();
                    int startColumn = b.att().<Integer>get("contentStartColumn").get();
                    String source = b.att().<String>get("Source").get();
                    return configParser.parseString(b.contents(), startSymbol, Source.apply(source), startLine, startColumn);
                })
                .flatMap(result -> {
                    System.out.println("warning = " + result._2());
                    if (result._1().isRight())
                        return Stream.of(result._1().right().get());
                    else {
                        errors.addAll(result._1().left().get());
                        return Stream.empty();
                    }
                })
                .map(TreeNodesToKORE::apply)
                .map(TreeNodesToKORE::down)
                .map(contents -> {
                    KApply ruleContents = (KApply) contents;
                    List<org.kframework.kore.K> items = ruleContents.klist().items();
                    switch (ruleContents.klabel().name()) {
                    case "#ruleNoConditions":
                        return Configuration(items.get(0), _true, Att.apply());
                    case "#ruleEnsures":
                        return Configuration(items.get(0), items.get(1), Att.apply());
                    default:
                        throw new AssertionError("Wrong KLabel for rule content");
                    }
                })
                .findFirst();

        if (!errors.isEmpty()) {
            throw new AssertionError("Had " + errors.size() + " parsing errors: " + errors);
        }

        Configuration configDecl = configDeclOpt.get();

        Set<Sentence> configDeclProductions = GenerateSentencesFromConfigDecl.gen(configDecl.body(), configDecl.ensures(), configDecl.att(), configParser.module())._1();
        Module configurationModule = configDeclBubble.getValue();
        Module configurationModuleWithSentences = Module(configurationModule.name(), configurationModule.imports(), (Set<Sentence>)configurationModule.localSentences().$bar(configDeclProductions), configurationModule.att());
        modules.remove(configurationModule);
        modules.add(configurationModuleWithSentences);
        Definition defWithConfiguration = Definition(immutable(modules));

        Module mainModuleBubblesWithConfig = stream(defWithConfiguration.modules()).filter(m -> m.name().equals(mainModuleName)).findFirst().get();

        gen = new RuleGrammarGenerator(defWithConfiguration);

        ParseInModule ruleParser = gen.getRuleGrammar(mainModuleBubblesWithConfig);

        Set<Sentence> ruleSet = stream(mainModuleBubblesWithConfig.sentences())
                .parallel()
                .filter(s -> s instanceof Bubble)
                .map(b -> (Bubble) b)
                .filter(b -> !b.sentenceType().equals("config"))
                .map(b -> {
                    int startLine = b.att().<Integer>get("contentStartLine").get();
                    int startColumn = b.att().<Integer>get("contentStartColumn").get();
                    String source = b.att().<String>get("Source").get();
                    return ruleParser.parseString(b.contents(), startSymbol, Source.apply(source), startLine, startColumn);
                })
                .flatMap(result -> {
                    if (result._1().isRight()) {
                        System.out.println("warning = " + result._2());
                        return Stream.of(result._1().right().get());
                    } else {
                        errors.addAll(result._1().left().get());
                        return Stream.empty();
                    }
                })
                .map(TreeNodesToKORE::apply)
                .map(TreeNodesToKORE::down)
                .map(contents -> {
                    KApply ruleContents = (KApply) contents;
                    List<org.kframework.kore.K> items = ruleContents.klist().items();
                    switch (ruleContents.klabel().name()) {
                        case "#ruleNoConditions":
                            return Rule(items.get(0), _true, _true);
                        case "#ruleRequires":
                            return Rule(items.get(0), items.get(1), _true);
                        case "#ruleEnsures":
                            return Rule(items.get(0), _true, items.get(1));
                        case "#ruleRequiresEnsures":
                            return Rule(items.get(0), items.get(1), items.get(2));
                        default:
                            throw new AssertionError("Wrong KLabel for rule content");
                    }
                })
                .collect(Collections.toSet());

        if (!errors.isEmpty()) {
            throw new AssertionError("Had " + errors.size() + " parsing errors: " + errors);
        }

        Module mainModule = Module(mainModuleName, Set(),
                (Set<Sentence>) mainModuleBubblesWithConfig.sentences().$bar(ruleSet), Att());

        Module afterHeatingCooling = StrictToHeatingCooling.apply(mainModule);

        Definition kastDefintion = Definition(immutable(
                parser.loadModules(REQUIRE_KAST_K,
                        Source.apply(BUILTIN_DIRECTORY.toPath().resolve("kast.k").toFile().getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY))));

        //ConfigurationInfoFromModule configInfo = new ConfigurationInfoFromModule(afterHeatingCooling);


        Module withKSeq = Module("EXECUTION",
                Set(afterHeatingCooling, kastDefintion.getModule("KSEQ").get()),
                Collections.<Sentence>Set(), Att());

        Module moduleForPrograms = defWithConfiguration.getModule(mainProgramsModule).get();
        Set<Module> imports = Stream.concat(stream(moduleForPrograms.imports()), Stream.of(configurationModuleWithSentences)).collect(Collections.toSet());
        if (moduleForPrograms.importedModules().contains(configurationModule)) {
            moduleForPrograms = Module(moduleForPrograms.name(), imports, moduleForPrograms.localSentences(), moduleForPrograms.att());
        }
        ParseInModule parseInModule = gen.getProgramsGrammar(moduleForPrograms);

        final BiFunction<String, Source, K> pp = (s, source) -> {
            Tuple2<Either<java.util.Set<ParseFailedException>, Term>, java.util.Set<ParseFailedException>> res = parseInModule.parseString(s, programStartSymbol, source);
            if (res._1().isLeft()) {
                throw res._1().left().get().iterator().next();
            }
            return TreeNodesToKORE.down(TreeNodesToKORE.apply(res._1().right().get()));
        };

        return Tuple2.apply(withKSeq, pp);
    }
}
