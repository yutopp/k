package org.kframework.backend.pdmc.k;

import org.kframework.backend.java.kil.Term;
import org.kframework.backend.pdmc.pda.*;
import org.kframework.backend.pdmc.k.JavaKRunPushdownSystem;
import org.kframework.backend.pdmc.pda.buchi.Evaluator;
import org.kframework.backend.pdmc.pda.buchi.Identifier;

/**
 * Created by Traian on 08.07.2014.
 */
public class JavaKRunPromelaEvaluator implements Evaluator<ConfigurationHead<Term, Term>> {

    JavaKRunPushdownSystem pds;

    public JavaKRunPromelaEvaluator(JavaKRunPushdownSystem pds) {
        this.pds = pds;
    }

    Term state;

    @Override
    public void setState(ConfigurationHead<Term, Term> head) {
       state = pds.getKConfig(head.getState(), head.getLetter());
    }

    @Override
    public boolean evaluate(Identifier id) {
        System.err.println(state.toString() + " |= " + id);
        return false;
    }
}
