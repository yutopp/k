package org.kframework.backend.java.kil;

import java.math.BigInteger;

import org.kframework.krun.ioserver.filesystem.portable.PortableFileSystem;

/**
 * State represents a state in the rewrite system.
 * 
 * The aim is for State to be immutable. It is not transitively immutable
 * because the global state links to the mutable file system.
 */

public class State<T extends TermLike> {
    public final T topTerm;
    public final GlobalContext global;
    public final BigInteger counter;

    public State(T topTerm, GlobalContext global,
            BigInteger counter) {
        this.topTerm = topTerm;
        this.global = global;
        this.counter = counter;
    }
    
    public State<T> incrementCounter() {
        return new State<T>(topTerm, global, counter.add(BigInteger.ONE));
    }
    
    public <U extends TermLike> State<U> copy(U topTerm) {
        return new State<U>(topTerm, global, counter);
    }
}