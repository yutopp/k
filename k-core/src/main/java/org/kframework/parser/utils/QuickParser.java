package org.kframework.parser.utils;

import org.kframework.compile.sharing.TokenSortCollector;
import org.kframework.compile.transformers.FlattenTerms;
import org.kframework.compile.transformers.RemoveBrackets;
import org.kframework.kil.ASTNode;
import org.kframework.kil.GeneratedSource;
import org.kframework.kil.Location;
import org.kframework.kil.Module;
import org.kframework.kil.ProductionReference;
import org.kframework.kil.Sort;
import org.kframework.kil.Term;
import org.kframework.kil.loader.CollectPrioritiesVisitor;
import org.kframework.kil.loader.Context;
import org.kframework.kil.loader.UpdateReferencesVisitor;
import org.kframework.kil.visitors.exceptions.ParseFailedException;
import org.kframework.kompile.KompileOptions;
import org.kframework.main.GlobalOptions;
import org.kframework.parser.concrete.disambiguate.AmbFilter;
import org.kframework.parser.concrete.disambiguate.PreferAvoidFilter;
import org.kframework.parser.concrete.disambiguate.PriorityFilter;
import org.kframework.parser.concrete2.Grammar;
import org.kframework.parser.concrete2.KSyntax2GrammarStatesFilter;
import org.kframework.parser.concrete2.MakeConsList;
import org.kframework.parser.concrete2.Parser;
import org.kframework.parser.concrete2.TreeCleanerVisitor;
import org.kframework.parser.concrete2.Parser.ParseError;
import org.kframework.parser.generator.CollectTerminalsVisitor;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;

public class QuickParser {
    public static ProductionReference parse(String program, Sort startSymbol, ASTNode definition) throws ParseFailedException {
        Context context = new Context();
        context.kompileOptions = new KompileOptions();
        context.globalOptions = new GlobalOptions();

        UpdateReferencesVisitor updateReferencesVisitor = new UpdateReferencesVisitor(context);
        updateReferencesVisitor.visitNode(definition);
        context.setTokenSorts(TokenSortCollector.collectTokenSorts(definition, context));
        
        // collect the syntax from those modules
        CollectTerminalsVisitor ctv = new CollectTerminalsVisitor(context);
        // visit all modules to collect all Terminals first
        ctv.visitNode(definition);
        KSyntax2GrammarStatesFilter ks2gsf = new KSyntax2GrammarStatesFilter(context, ctv);
        ks2gsf.visitNode(definition);
        Grammar grammar = ks2gsf.getGrammar();
        
        CollectPrioritiesVisitor collectPrioritiesVisitor = new CollectPrioritiesVisitor(context);
        collectPrioritiesVisitor.visitNode(definition);

        Parser parser = new Parser(program);
        ASTNode out = parser.parse(grammar.get(startSymbol.toString()), 0);

        try {
            // only the unexpected character type of errors should be checked in this block
            out = new TreeCleanerVisitor(context).visitNode(out);
        } catch (ParseFailedException te) {
            ParseError perror = parser.getErrors();

            String msg = program.length() == perror.position ? "Parse error: unexpected end of file."
                    : "Parse error: unexpected character '" + program.charAt(perror.position) + "'.";
            Location loc = new Location(perror.line, perror.column, perror.line, perror.column + 1);
            throw new ParseFailedException(
                    new KException(ExceptionType.ERROR, KExceptionGroup.INNER_PARSER, msg,
                            new GeneratedSource(QuickParser.class), loc));
        }
        out = new MakeConsList(context).visitNode(out);
        if (context.globalOptions.debug)
            System.err.println("Clean: " + out + "\n");
        out = new PriorityFilter(context).visitNode(out);
        out = new PreferAvoidFilter(context).visitNode(out);
        if (context.globalOptions.debug)
            System.err.println("Filtered: " + out + "\n");
//            out = new AmbFilter(context).visitNode(out);
        out = new RemoveBrackets(context).visitNode(out);

        return (ProductionReference) out;
    }
}
