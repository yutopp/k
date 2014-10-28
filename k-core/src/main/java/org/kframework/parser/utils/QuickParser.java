// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.parser.utils;

import org.kframework.compile.sharing.TokenSortCollector;
import org.kframework.kil.ASTNode;
import org.kframework.kil.GeneratedSource;
import org.kframework.kil.ProductionReference;
import org.kframework.kil.Sort;
import org.kframework.kil.loader.CollectPrioritiesVisitor;
import org.kframework.kil.loader.Context;
import org.kframework.kil.loader.UpdateReferencesVisitor;
import org.kframework.kompile.KompileOptions;
import org.kframework.main.GlobalOptions;
import org.kframework.parser.ProgramLoader;
import org.kframework.parser.concrete2.Grammar;
import org.kframework.parser.concrete2.KSyntax2GrammarStatesFilter;
import org.kframework.parser.generator.CollectTerminalsVisitor;
import org.kframework.utils.errorsystem.ParseFailedException;

public class QuickParser {
    public static ProductionReference parse(String program, Sort startSymbol, ASTNode definition) throws ParseFailedException {
        Context context = new Context();
        context.kompileOptions = new KompileOptions();
        context.globalOptions = new GlobalOptions();

        new UpdateReferencesVisitor(context).visitNode(definition);
        context.setTokenSorts(TokenSortCollector.collectTokenSorts(definition, context));
        
        // collect the syntax from those modules
        CollectTerminalsVisitor ctv = new CollectTerminalsVisitor(context);
        // visit all modules to collect all Terminals first
        ctv.visitNode(definition);
        KSyntax2GrammarStatesFilter ks2gsf = new KSyntax2GrammarStatesFilter(context, ctv);
        ks2gsf.visitNode(definition);
        Grammar grammar = ks2gsf.getGrammar();

        new CollectPrioritiesVisitor(context).visitNode(definition);

        ASTNode out = ProgramLoader.newParserParse(program, grammar.get(startSymbol.toString()), new GeneratedSource(QuickParser.class), context);
        return (ProductionReference) out;
    }
}
