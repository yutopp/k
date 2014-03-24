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
public abstract class BasicAutomaton<State, Alphabet> implements AutomatonInterface<State, Alphabet> {


    @Override
    public Set<Transition<State, Alphabet>> getBackEpsilonTransitions(State state) {
        return getBackTransitions(state, null);
    }

    @Override
    public Deque<Transition<State, Alphabet>> getPath(State initialState, State finalState) {
        if (initialState.equals(finalState)) {
            return new ArrayDeque<>();
        }
        // This algorithm is a simple BFS traversal of the transition graph.
        // toProcess keeps the queue of the states which have yet to be processed
        Deque<State> toProcess = new ArrayDeque<>();
        // considered remembers states which have already been seen.
        // To save the return path, is organized as a map, assigning to each state the transition through which it was
        // reached.
        Map<State, Transition<State, Alphabet>> considered = new HashMap<>();
        toProcess.add(initialState);
        considered.put(initialState, null);
        return getPathBFS(finalState, toProcess, considered);
    }


    @Override
    public Deque<Transition<State, Alphabet>> getPath(State initialState, Alphabet initialLetter, State finalState) {
        // We seed the algorithm with the initialState and those reachable  from it following initialLetter
        Deque<State> toProcess = new ArrayDeque<>();
        Map<State, Transition<State, Alphabet>> considered = new HashMap<>();
        considered.put(initialState, null);

        // Use only states reachable from initialState via initialLetter for the first step.
        Deque<Transition<State, Alphabet>> result =
                getPathBFSStep(getTransitions(initialState, initialLetter), finalState, considered, toProcess);
        // If path was found in one step, return it
        if (result != null) return result;
        // If path was not found yet, continue with usual BFS algorithm
        return getPathBFS(finalState, toProcess, considered);
    }

    private Deque<Transition<State, Alphabet>> getPathBFS(State finalState,
                                                          Deque<State> toProcess,
                                                          Map<State, Transition<State, Alphabet>> considered) {
        Deque<Transition<State, Alphabet>> result;
        State next;
        while (!toProcess.isEmpty()) {
            next = toProcess.remove();
            for (Set<Transition<State, Alphabet>> transitions: getFrontTransitions(next)) {
                result = getPathBFSStep(transitions, finalState, considered, toProcess);
                if (result !=null) return result;
            }
        }
        // If this point was reached, it means finalState is not reachable from initialState.
        return null;
    }

    private Deque<Transition<State, Alphabet>> getPathBFSStep(Set<Transition<State, Alphabet>> transitions, State finalState, Map<State, Transition<State, Alphabet>> considered, Deque<State> toProcess) {
        for (Transition<State, Alphabet> transition : transitions) {
            State endState = transition.getEnd();
            if (endState.equals(finalState)) {
                // if the final state is reached, compute the path to it by walking back on the transitions.
                Deque<Transition<State, Alphabet>> result = new ArrayDeque<>();
                while (transition != null) {
                    result.push(transition);
                    transition = considered.get(transition.getStart());
                }
                return result;
            }
            if (considered.containsKey(endState)) continue;
            considered.put(endState, transition);
            toProcess.add(endState);
        }
        return null;
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
