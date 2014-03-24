package org.kframework.backend.pdmc.automaton;

import org.kframework.backend.pdmc.pda.buchi.BuchiState;

import java.util.Collection;
import java.util.Deque;
import java.util.Set;

/**
 * @author Traian
 */
public interface AutomatonInterface<State, Alphabet> {
    /**
     * Retrieves the set of automaton transitions corresponding to a  state-letter pair.
     * @param state the first argument of the NFA transition function
     * @param letter the second argument of the NFA transition function
     * @return the (possibly empty) set of transitions corresponding to given state and letter
     */
    Set<Transition<State, Alphabet>> getTransitions(State state, Alphabet letter);

    /**
     * Retrieves the set of automaton transitions labelled with a letter and ending in a state
     * @param state the state transitions should end with
     * @param letter the letter transitions should be labelled with
     * @return the (possibly empty) set of transitions corresponding to given state and letter
     */
    Set<Transition<State, Alphabet>> getBackTransitions(State state, Alphabet letter);

    /**
     * Retrieves the set of automaton epsilon transitions ending in a state
     * @param state the state transitions should end with
     * @return the (possibly empty) set of epsilon transitions corresponding to given state
     */
    Set<Transition<State, Alphabet>> getBackEpsilonTransitions(State state);

    /**
     * @return the initial state of the Automaton
     */
    State initialState();

    /**
     * @return the set of final states of the automaton.
     */
    Set<State> getFinalStates();

    /**
     * Given an initial state and a final state, find a path of transitions between them in the automaton
     * @param initialState the state to begin from
     * @param finalState  the final state to reach
     * @return a list of transitions describing a path from initialState to finalState or null if there is no such list.
     */
    Deque<Transition<State, Alphabet>> getPath(State initialState, State finalState);

    /**
     * Given an initial state, a final state and an initial letter, find a path of transitions
     * between them such that first transition is labeled with the initial letter.
     * @param initialState the state to begin from
     * @param initialLetter the letter to start the path on
     * @param finalState  the final state to reach
     * @return a list of transitions describing a path from initialState to finalState or null if there is no such list.
     */
    Deque<Transition<State, Alphabet>> getPath(State initialState, Alphabet initialLetter, State finalState);

    /**
     * @return  a collection of all the letters labeling some transition.
     */
    Collection<Alphabet> getLetters();

    Collection<? extends StateLetterPair<State, Alphabet>> getTransitionHeads();
}
