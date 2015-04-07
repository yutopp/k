package org.kframework.kore;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.kframework.kore.compile.GenerateSentencesFromConfigDecl;
import org.kframework.parser.TreeNodesToKORE;
import org.kframework.parser.concrete2kore.ParseInModule;
import org.kframework.parser.concrete2kore.ParserUtils;
import org.kframework.parser.concrete2kore.generator.RuleGrammarGenerator;
import org.kframework.tiny.*;
import org.kframework.utils.StringUtil;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.errorsystem.ParseFailedException;
import scala.Option;
import scala.Tuple2;
import scala.collection.Seq;
import scala.collection.immutable.Set;

import static org.kframework.Collections.*;
import static org.kframework.kore.KORE.*;
import static org.kframework.definition.Constructors.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
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

    private static RuleGrammarGenerator makeRuleGrammarGenerator() throws URISyntaxException, IOException {
        String definitionText;
        File definitionFile = new File(BUILTIN_DIRECTORY.toString() + "/kast.k");
        try {
            definitionText = FileUtils.readFileToString(definitionFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Definition baseK = ParserUtils.parseMainModuleOuterSyntax(definitionText, mainModule);
        java.util.Set<Module> modules =
                ParserUtils.loadModules(definitionText,
                        Source.apply(definitionFile.getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY));

        Definition baseK = Definition(immutable(modules));
        return new RuleGrammarGenerator(baseK);
    }

    public static org.kframework.tiny.Rewriter getRewriter(Module module) throws IOException, URISyntaxException {

        return new org.kframework.tiny.Rewriter(module, KIndex$.MODULE$);
    }

    // todo: rename and refactor this
    public static Tuple2<Module, BiFunction<String, Source, K>> getStuff(File definitionFile, String mainModuleName, String mainProgramsModule) throws IOException, URISyntaxException {
        String definitionString = FileUtils.readFileToString(definitionFile);

//        Module mainModuleWithBubble = ParserUtils.parseMainModuleOuterSyntax(definitionString, "TEST");

        java.util.Set<Module> modules =
                ParserUtils.loadModules(REQUIRE_KAST_K + definitionString,
                        Source.apply(definitionFile.getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY));

        Definition definition = Definition(immutable(modules));

        Module mainModuleWithBubble = stream(definition.modules()).filter(m -> m.name().equals(mainModuleName)).findFirst().get();

        RuleGrammarGenerator gen = makeRuleGrammarGenerator();
        ParseInModule configParser = gen.getConfigGrammar(mainModuleWithBubble);

        Set<Bubble> configDecls = stream(mainModuleWithBubble.sentences())
                .filter(s -> s instanceof Bubble)
                .map(b -> (Bubble) b)
                .filter(b -> b.sentenceType().equals("config"))
                .collect(Collections.toSet());
        if (configDecls.size() > 1) {
            throw KExceptionManager.compilerError("Found more than one configuration in definition: " + configDecls);
        }
        if (configDecls.size() == 0) {
            configDecls = Set(Bubble("config", "<k> $PGM:K </k>", Att().add("Source", "<generated>").add("contentStartLine", 1).add("contentStartColumn", 1)));
        }

        java.util.Set<ParseFailedException> errors = Sets.newHashSet();

        Optional<Configuration> configDeclOpt = stream(configDecls)
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
                        return Configuration(items.get(0), Or.apply(), Att.apply());
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
        Module mainModuleBubblesWithConfig = Module(mainModuleName, Set(),
                (Set<Sentence>) mainModuleWithBubble.sentences().$bar(configDeclProductions), Att());

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
                            return Rule(items.get(0), And.apply(), Or.apply());
                        case "#ruleRequires":
                            return Rule(items.get(0), items.get(1), Or.apply());
                        case "#ruleEnsures":
                            return Rule(items.get(0), And.apply(), items.get(1));
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
                ParserUtils.loadModules(REQUIRE_KAST_K,
                        Source.apply(BUILTIN_DIRECTORY.toPath().resolve("kast.k").toFile().getAbsolutePath()),
                        definitionFile.getParentFile(),
                        Lists.newArrayList(BUILTIN_DIRECTORY))));

        ConfigurationInfoFromModule configInfo = new ConfigurationInfoFromModule(afterHeatingCooling);


        Module withKSeq = Module("EXECUTION",
                Set(afterHeatingCooling, kastDefintion.getModule("KSEQ").get()),
                Collections.<Sentence>Set(), Att());

        Module moduleForPrograms = definition.getModule(mainProgramsModule).get();
        ParseInModule parseInModule = RuleGrammarGenerator.getProgramsGrammar(moduleForPrograms);

        final BiFunction<String, Source, K> pp = (s, source) -> {
            return TreeNodesToKORE.down(TreeNodesToKORE.apply(parseInModule.parseString(s, "K", source)._1().right().get()));
        };

        return Tuple2.apply(withKSeq, pp);
    }
}
