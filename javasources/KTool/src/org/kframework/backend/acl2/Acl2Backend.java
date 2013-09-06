package org.kframework.backend.acl2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.kframework.backend.BasicBackend;
import org.kframework.backend.unparser.UnparserFilter;
import org.kframework.compile.FlattenModules;
import org.kframework.compile.ResolveConfigurationAbstraction;
import org.kframework.compile.checks.CheckConfigurationCells;
import org.kframework.compile.checks.CheckRewrite;
import org.kframework.compile.checks.CheckVariables;
import org.kframework.compile.transformers.AddEmptyLists;
import org.kframework.compile.transformers.AddHeatingConditions;
import org.kframework.compile.transformers.AddKCell;
import org.kframework.compile.transformers.AddTopCellConfig;
import org.kframework.compile.transformers.AddTopCellRules;
import org.kframework.compile.transformers.ContextsToHeating;
import org.kframework.compile.transformers.FreezeUserFreezers;
import org.kframework.compile.transformers.RemoveBrackets;
import org.kframework.compile.transformers.RemoveSyntacticCasts;
import org.kframework.compile.transformers.ResolveAnonymousVariables;
import org.kframework.compile.transformers.ResolveFunctions;
import org.kframework.compile.transformers.ResolveOpenCells;
import org.kframework.compile.transformers.ResolveRewrite;
import org.kframework.compile.transformers.ResolveSyntaxPredicates;
import org.kframework.compile.transformers.SortCells;
import org.kframework.compile.transformers.StrictnessToContexts;
import org.kframework.compile.utils.CheckVisitorStep;
import org.kframework.compile.utils.CompileDataStructures;
import org.kframework.compile.utils.CompilerSteps;
import org.kframework.compile.utils.InitializeConfigurationStructure;
import org.kframework.kil.Definition;
import org.kframework.kil.loader.Context;
import org.kframework.main.FirstStep;
import org.kframework.main.LastStep;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.file.KPaths;
import org.kframework.utils.general.GlobalSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class Acl2Backend extends BasicBackend {
    public Acl2Backend(Stopwatch sw, Context context) {
        super(sw, context);
    }

    @Override
    public void run(Definition definition) throws IOException {
        Acl2Definition def = new Acl2Definition(definition);
        Acl2Context acl2Context = new Acl2Context(context);
        acl2Context.setAcl2Package(definition.getMainModule().toUpperCase());

        GenerateAcl2Syntax.generateSyntax(acl2Context, def);
        GenerateAcl2TransitionFunction.generateAcl2TransitionFunction(acl2Context, def);
        
        File kompiledTemplate = new File(KPaths.getKBase(false),"lib/acl2/kompiled-template");
        FileUtils.copyDirectory(kompiledTemplate, context.dotk);

        String base = FilenameUtils.getBaseName(GlobalSettings.mainFile.getPath());

        writeBuildSystem(base, context.dotk);

        FileWriter w = new FileWriter(new File(context.dotk, base + ".lisp"));
        w.append(def.getLispWriter().getBuffer());
        w.close();
    }

    private void writeBuildSystem(String definitionName, File kompiledDir) throws IOException {
        File defpkg = new File(context.dotk, definitionName+"-defpkg.lsp");
        FileWriter w = new FileWriter(defpkg);
        w.append(""
                +"(ld \"coi/util/def-defpkg.lsp\" :dir :system)\n"
                +"(defpkg \""+definitionName.toUpperCase()+"\"\n"
                +"  (append '(seqmatch::seq-match)\n"
                +"    (revappend *acl2-exports*\n"
                +"      *common-lisp-symbols-from-main-lisp-package*)))\n"
                +"(assign ld-okp t)\n"
                );
        w.close();

        File cert = new File(context.dotk, "cert.acl2");
        w = new FileWriter(cert);
        w.append(""
                +"(ld \"seqmatch-defpkg.lsp\")\n"
                +"(ld \"syntax-defpkg.lsp\")\n"
                +"(ld \"k-defpkg.lsp\")\n"
                +"(ld \""+definitionName+"-defpkg.lsp\")\n"
                );
        w.close();

        File makefile = new File(context.dotk, "Makefile");
        w = new FileWriter(makefile);
        w.append(""
                + "include $(ACL2_SYSTEM_BOOKS)/Makefile-generic\n"
                + "\n"
                + "BOOKS = seqmatch syntax k "+ definitionName + "\n");
        w.close();
    }

    @Override
    public String getDefaultStep() {
        return "LastStep";
    }

    public CompilerSteps<Definition> getCompilationSteps() {
        CompilerSteps<Definition> steps = new CompilerSteps<Definition>(context);
        steps.add(new FirstStep(this, context));
        steps.add(new CheckVisitorStep<Definition>(new CheckConfigurationCells(context), context));
        steps.add(new RemoveBrackets(context));
        steps.add(new AddEmptyLists(context));
        steps.add(new ResolveEmpty(context));
        steps.add(new RemoveSyntacticCasts(context));
        steps.add(new CheckVisitorStep<Definition>(new CheckVariables(context), context));
        steps.add(new CheckVisitorStep<Definition>(new CheckRewrite(context), context));
        steps.add(new FlattenModules(context));
        steps.add(new StrictnessToContexts(context));
        steps.add(new FreezeUserFreezers(context));
        steps.add(new ContextsToHeating(context));
        // steps.add(new AddSupercoolDefinition(context));
        steps.add(new AddHeatingConditions(context));
        // steps.add(new AddSuperheatRules(context));
        // steps.add(new DesugarStreams(context));
        steps.add(new ResolveFunctions(context));
        steps.add(new AddKCell(context));
        // steps.add(new AddStreamCells(context));
        // steps.add(new AddSymbolicK(context));
        // steps.add(new AddSemanticEquality(context));
        // steps.add(new ResolveFresh());
        // steps.add(new FreshCondToFreshVar(context));
        // steps.add(new ResolveFreshVarMOS(context));
        steps.add(new AddTopCellConfig(context));
        if (GlobalSettings.addTopCell) {
            steps.add(new AddTopCellRules(context));
        }
        // steps.add(new ResolveBinder(context));
        steps.add(new ResolveAnonymousVariables(context));
        // steps.add(new ResolveBlockingInput(context));
        //steps.add(new AddK2SMTLib(context));
        //steps.add(new AddPredicates(context));
        steps.add(new AddAcl2Predicates(context));
        steps.add(new ResolveSyntaxPredicates(context));
        // steps.add(new ResolveListOfK(context));
        //steps.add(new FlattenSyntax(context));
        //steps.add(new AddKStringConversion(context));

        //steps.add(new AddKLabelConstant(context));
        //steps.add(new ResolveHybrid(context));
        steps.add(new InitializeConfigurationStructure(context));
        steps.add(new ResolveConfigurationAbstraction(context));
        steps.add(new ResolveOpenCells(context));
        steps.add(new ResolveRewrite(context));
        steps.add(new CompileDataStructures(context));

        //if (GlobalSettings.sortedCells) {
            steps.add(new SortCells(context));
        // }
        //steps.add(new ResolveSupercool(context));
        //steps.add(new AddStrictStar(context));
        //steps.add(new AddDefaultComputational(context));
        //steps.add(new AddOptionalTags(context));
        //steps.add(new DeclareCellLabels(context));
        steps.add(new ReassocKSeqTransformer(context));
        steps.add(new LastStep(this, context));
        return steps;
    }
}
