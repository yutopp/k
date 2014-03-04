package org.kframework.backend.symbolic;

import org.kframework.backend.Backend;
import org.kframework.backend.BasicBackend;
import org.kframework.backend.maude.KompileBackend;
import org.kframework.backend.maude.MaudeBackend;
import org.kframework.backend.maude.MaudeBuiltinsFilter;
import org.kframework.backend.unparser.UnparserFilter;
import org.kframework.compile.FlattenModules;
import org.kframework.compile.ResolveConfigurationAbstraction;
import org.kframework.compile.checks.CheckConfigurationCells;
import org.kframework.compile.checks.CheckRewrite;
import org.kframework.compile.checks.CheckVariables;
import org.kframework.compile.sharing.DeclareCellLabels;
import org.kframework.compile.tags.AddDefaultComputational;
import org.kframework.compile.tags.AddOptionalTags;
import org.kframework.compile.tags.AddStrictStar;
import org.kframework.compile.transformers.*;
import org.kframework.compile.utils.CheckVisitorStep;
import org.kframework.compile.utils.CompileDataStructures;
import org.kframework.compile.utils.CompilerSteps;
import org.kframework.compile.utils.InitializeConfigurationStructure;
import org.kframework.kil.Definition;
import org.kframework.kil.loader.Context;
import org.kframework.main.FirstStep;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.file.KPaths;
import org.kframework.utils.general.GlobalSettings;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
/**
 * Compile a K definition symbolically, using both basic
 * and specific compilation steps. 
 * @author andreiarusoaie
 *
 */
public class SymbolicBackend extends KompileBackend {

    public static String SYMBOLIC = "symbolic-kompile";
    public static String NOTSYMBOLIC = "not-symbolic-kompile";

    public SymbolicBackend(Stopwatch sw, Context context) {
        super(sw, context);
    }

    @Override
    public CompilerSteps<Definition> getCompilationSteps() {
        CompilerSteps<Definition> steps = new CompilerSteps<Definition>(context);
        steps.add(new FirstStep(this, context));
        steps.add(new CheckVisitorStep<Definition>(new CheckConfigurationCells(context), context));
        steps.add(new RemoveBrackets(context));
        steps.add(new AddEmptyLists(context));
        steps.add(new RemoveSyntacticCasts(context));
        steps.add(new CheckVisitorStep<Definition>(new CheckVariables(context), context));
        steps.add(new CheckVisitorStep<Definition>(new CheckRewrite(context), context));
        steps.add(new FlattenModules(context));
        steps.add(new StrictnessToContexts(context));
        steps.add(new FreezeUserFreezers(context));
        steps.add(new ContextsToHeating(context));
        steps.add(new AddSupercoolDefinition(context));
        steps.add(new AddHeatingConditions(context));
        steps.add(new AddSuperheatRules(context));
        steps.add(new ResolveSymbolicInputStream(context)); // symbolic step
        steps.add(new DesugarStreams(context, false));
        steps.add(new ResolveFunctions(context));
        steps.add(new TagUserRules(context)); // symbolic step
        steps.add(new ReachabilityRuleToKRule(context)); // symbolic step 
        steps.add(new AddKCell(context));
        steps.add(new AddSymbolicK(context));

        steps.add(new AddSemanticEquality(context));
        steps.add(new FreshCondToFreshVar(context));
        steps.add(new ResolveFreshVarMOS(context));
        steps.add(new AddTopCellConfig(context));
        steps.add(new AddConditionToConfig(context)); // symbolic step
        steps.add(new AddTopCellRules(context));
        steps.add(new ResolveBinder(context));
        steps.add(new ResolveAnonymousVariables(context));
        steps.add(new ResolveBlockingInput(context, false));
        steps.add(new AddK2SMTLib(context));
        steps.add(new AddPredicates(context));
        steps.add(new ResolveSyntaxPredicates(context));
        steps.add(new ResolveBuiltins(context));
        steps.add(new ResolveListOfK(context));
        steps.add(new FlattenSyntax(context));
        steps.add(new InitializeConfigurationStructure(context));
        steps.add(new AddKStringConversion(context));
        steps.add(new AddKLabelConstant(context));
        steps.add(new ResolveHybrid(context));
        steps.add(new ResolveConfigurationAbstraction(context));
        steps.add(new ResolveOpenCells(context));
        steps.add(new ResolveRewrite(context));
        steps.add(new CompileDataStructures(context));

        // steps.add(new LineariseTransformer()); // symbolic step
        steps.add(new ReplaceConstants(context)); // symbolic step
        steps.add(new AddPathCondition(context)); // symbolic step
        steps.add(new AddPathConditionToReachabilityKRule(context)); // symbolic step
        steps.add(new ResolveLtlAttributes(context)); // symbolic step (special case for ltl)

        steps.add(new ResolveSupercool(context));
        steps.add(new AddStrictStar(context));
        steps.add(new AddDefaultComputational(context));
        steps.add(new AddOptionalTags(context));
        steps.add(new DeclareCellLabels(context));

        return steps;
    }
}
