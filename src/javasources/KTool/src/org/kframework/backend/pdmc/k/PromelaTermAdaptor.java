package org.kframework.backend.pdmc.k;

import org.kframework.backend.pdmc.pda.buchi.PromelaBuchi;
import org.kframework.kil.Term;
import org.kframework.kil.visitors.Visitor;

/**
 * Created by Traian on 08.07.2014.
 */
public class PromelaTermAdaptor extends Term {

    final PromelaBuchi automaton;

    public PromelaTermAdaptor(PromelaBuchi automaton) {
        this.automaton = automaton;
    }

    @Override
    public Term shallowCopy() {
        return new PromelaTermAdaptor(automaton);
    }

    @Override
    protected <P, R, E extends Throwable> R accept(Visitor<P, R, E> visitor, P p) throws E {
        return null;
    }

    @Override
    public int hashCode() {
        return automaton.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PromelaTermAdaptor that = (PromelaTermAdaptor) o;

        if (!automaton.equals(that.automaton)) return false;

        return true;
    }

    public PromelaBuchi getAutomaton() {
        return automaton;
    }
}
