package org.kframework.backend.pdmc.k;

import com.google.common.collect.ImmutableList;
import org.kframework.backend.java.builtins.BoolToken;
import org.kframework.backend.java.builtins.StringToken;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.pdmc.pda.*;
import org.kframework.backend.pdmc.k.JavaKRunPushdownSystem;
import org.kframework.backend.pdmc.pda.buchi.Evaluator;
import org.kframework.backend.pdmc.pda.buchi.Identifier;

/**
 * Created by Traian on 08.07.2014.
 */
public class JavaKRunPromelaEvaluator implements Evaluator<ConfigurationHead<Term, Term>> {

    JavaKRunPushdownSystem pds;
    TermContext termContext;
    Definition definition;

    public JavaKRunPromelaEvaluator(JavaKRunPushdownSystem pds) {
        this.pds = pds;
        termContext = pds.termContext;
        definition = termContext.definition();
    }

    Term state;

    @Override
    public void setState(ConfigurationHead<Term, Term> head) {
       state = pds.getKConfig(head.getState(), head.getLetter());
    }

    @Override
    public boolean evaluate(Identifier id) {
        StringToken idString = StringToken.of(id.toString());
        Term term = new KItem(KLabelConstant.of("'_|=_", definition),
                new KList(ImmutableList.copyOf(new Term[]{KLabelInjection.injectionOf(state, termContext), idString})),
                termContext);
        System.out.print(term.toString());
        Term result = term.evaluate(termContext);
        System.out.println( " = " + result.toString());
        assert result instanceof BoolToken;
        return ((BoolToken) result).booleanValue();
    }
}
