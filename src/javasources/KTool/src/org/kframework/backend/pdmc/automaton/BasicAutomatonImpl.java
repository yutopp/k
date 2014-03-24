package org.kframework.backend.pdmc.automaton;

import com.google.common.base.Joiner;

import java.util.*;

/**
 * This class defines a nondeterministic finite automaton (NFA).  It keeps the transition function of the automatonas a
 * map indexed on pairs of states and alphabet letters having as values the set of transitions corresponding to that
 * index.
 * It is parameterized on the class for states and the one for the alphabet.
 *
 * @param <State>  represents the states of the automaton
 * @param <Alphabet>  represents the alphabet of the automaton
 * @author Traian
 */
public class BasicAutomatonImpl<State, Alphabet> extends BasicAutomaton<State, Alphabet> {
    /**
     * Indexing datastructure for the automaton's transition relation.
     * Given a {@see TransitionIndex} as a pair between a state and a letter, it yields all
     * transitions originating in that pair (note this is a NFA).
     */
    private final Map<TransitionIndex<State, Alphabet>,
                Set<Transition<State, Alphabet>>> deltaIndex;

    /**
     * Indexing datastructure for the reverse of the automaton's transition relation.
     * Given a {@see TransitionIndex} as a pair between a state and a letter, it yields all
     * transitions labelled with the letter and ending in the state.
     */
    private final Map<TransitionIndex<State, Alphabet>,
                Set<Transition<State, Alphabet>>> reverseDeltaIndex;

    /**
     * Accessor for the {@code deltaIndex}
     * @return deltaIndex representing the delta mapping of the automaton.
     */
    public Map<TransitionIndex<State, Alphabet>, Set<Transition<State, Alphabet>>> getDeltaIndex() {
        return deltaIndex;
    }


    private final State initialState;
    private final Set<State> finalStates;
    private final Set<Alphabet> letters;


    /**
     * Retrieves the set of automaton transitions corresponding to a  state-letter pair.
     * @param state the first argument of the NFA transition function
     * @param letter the second argument of the NFA transition function
     * @return the (possibly empty) set of transitions corresponding to given state and letter
     */
    @Override
    public Set<Transition<State, Alphabet>> getTransitions(State state, Alphabet letter) {
        Set<Transition<State, Alphabet>> transitions = deltaIndex.get(
                TransitionIndex.of(state, letter) );
        if (transitions == null) transitions = Collections.emptySet();
        return transitions;
    }

    /**
     * Retrieves the set of automaton transitions labelled with a letter and ending in a state
     * @param state the state transitions should end with
     * @param letter the letter transitions should be labelled with
     * @return the (possibly empty) set of transitions corresponding to given state and letter
     */
    @Override
    public Set<Transition<State, Alphabet>> getBackTransitions(State state, Alphabet letter) {
        Set<Transition<State, Alphabet>> transitions = reverseDeltaIndex.get(
                TransitionIndex.of(state, letter) );
        if (transitions == null) transitions = Collections.emptySet();
        return transitions;
    }

    /**
     * Retrieves the set of automaton epsilon transitions ending in a state
     * @param state the state transitions should end with
     * @return the (possibly empty) set of epsilon transitions corresponding to given state
     */
    @Override
    public Set<Transition<State, Alphabet>> getBackEpsilonTransitions(State state) {
        return getBackTransitions(state, null);
    }

     @Override
    public State initialState() {
        return initialState;
    }

    @Override
    public Set<State> getFinalStates() {
        return finalStates;
    }

    public BasicAutomatonImpl(Collection<Transition<State, Alphabet>> delta,
                              State initialState,
                              Collection<State> finalStates) {
        this.initialState = initialState;
        this.finalStates = new HashSet<>(finalStates);
        this.letters = new HashSet<>();
        deltaIndex = new HashMap<>();
        reverseDeltaIndex = new HashMap<>();
        for (Transition<State, Alphabet> transition : delta) {
            Alphabet letter = transition.getLetter();
            if(letter != null) {
                letters.add(letter);
            }
            addToIndex(deltaIndex, transition, transition.getIndex());
            addToIndex(reverseDeltaIndex, transition, TransitionIndex.of(transition.getEnd(), letter));
        }

    }

    private void addToIndex(Map<TransitionIndex<State, Alphabet>, Set<Transition<State, Alphabet>>> deltaIndex, Transition<State, Alphabet> transition, TransitionIndex<State, Alphabet> index) {
        Set<Transition<State, Alphabet>> transitions = deltaIndex.get(index);
        if (transitions == null) {
            transitions = new HashSet<>();
            deltaIndex.put(index, transitions);
        }
        transitions.add(transition);
    }

    public Collection<Set<Transition<State, Alphabet>>> getTransitions() {
        return deltaIndex.values();
    }
 
    @Override
    public String toString() {
        Joiner joiner = Joiner.on(";\n");
        List<StringBuilder> builders = new ArrayList<>();
        for (Set<Transition<State, Alphabet>> transitions : deltaIndex.values()) {
            StringBuilder builder = new StringBuilder();
            joiner.appendTo(builder, transitions);
            builders.add(builder);
        }
        builders.add(new StringBuilder(initialState.toString()));
        Joiner joiner1 = Joiner.on(" ");
        StringBuilder builder = new StringBuilder();
        joiner1.appendTo(builder, finalStates);
        builders.add(builder);
        builder = new StringBuilder();
        joiner.appendTo(builder, builders);
        return builder.toString();
    }

    @Override
    public Collection<Alphabet> getLetters() {
        return letters;
    }

    @Override
    public Collection<? extends StateLetterPair<State, Alphabet>> getTransitionHeads() {
        return deltaIndex.keySet();
    }

    /**
     * @param state the state for which forward transitions are needed
     * @return the collection of all sets of transitions originating in state.
     */
    private Collection<Set<Transition<State, Alphabet>>> getFrontTransitions(State state) {
        ArrayList<Set<Transition<State, Alphabet>>> result = new ArrayList<>();
        for (Alphabet letter : getLetters()) {
            Set<Transition<State, Alphabet>> transitions = getTransitions(state, letter);
            if (!transitions.isEmpty()) {
                result.add(transitions);
            }
        }
        return result;
    }
}
