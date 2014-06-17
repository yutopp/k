package org.kframework.backend.java.kil;

import java.math.BigInteger;

/**
 * State represents a state in the rewrite system.
 * 
 * The aim is for State to be immutable.
 */

public class State {
    public final TermLike topConstrainedTermData;
    public final GlobalContext global;
    public final BigInteger counter;

    public State(TermLike topConstrainedTermData, GlobalContext global,
            BigInteger counter) {
        this.topConstrainedTermData = topConstrainedTermData;
        this.global = global;
        this.counter = counter;
    }
    
    public State incrementCounter() {
        return new State(topConstrainedTermData, global, counter.add(BigInteger.ONE));
    }
    
    public State copy(TermLike topConstrainedTermData) {
        return new State(topConstrainedTermData, global, counter);
    }
}