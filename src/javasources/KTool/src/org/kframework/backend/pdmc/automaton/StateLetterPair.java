package org.kframework.backend.pdmc.automaton;

/**
 * Interface for a pair between states
 * @author TraianSF
 */
public interface StateLetterPair<State, Alphabet> {
    Alphabet getLetter();

    State getState();
}
