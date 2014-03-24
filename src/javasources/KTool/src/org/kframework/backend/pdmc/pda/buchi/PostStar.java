package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.automaton.BasicAutomaton;
import org.kframework.backend.pdmc.automaton.StateLetterPair;
import org.kframework.backend.pdmc.automaton.Transition;
import org.kframework.backend.pdmc.pda.Configuration;
import org.kframework.backend.pdmc.pda.ConfigurationHead;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomaton;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;
import org.kframework.backend.pdmc.pda.pautomaton.util.IndexedTransitions;

import java.util.*;

/**
 * Class wrapping (and computing) the <b>post</b> automaton corresponding to a BPDS.
 * <ul>
 *    <li>
 *        The states of this automaton include all states of the BPDS reachable from the initial configuration;
 *    </li>
 *    <li>
 *        Any path q -w->* f from a BPDS state to a final state encodes the reachable configuration (q,w).
 *    </li>
 * </ul>
 *
 * Implements some pushdown model-checking related algorithms from  Stefan Schwoon's Phd Thesis:
 * S. Schwoon.  Model-Checking Pushdown Systems.  Ph.D. Thesis, Technische Universitaet Muenchen, June 2002.
 *
 * @author TraianSF
 */
public class PostStar<Control, Alphabet> extends BasicAutomaton<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>  {

    final BuchiPushdownSystemInterface<Control, Alphabet> bps;
    private BasicAutomaton<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> automaton = null;

    final Map<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>, LabelledAlphabet<Control, Alphabet>>
            transitionLabels;

    public PostStar(BuchiPushdownSystemInterface<Control, Alphabet> bps, Map<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>, LabelledAlphabet<Control, Alphabet>> transitionLabels) {
        this.transitionLabels = transitionLabels;
        this.bps = bps;
        compute();

    }

    /**
     * Main method of the class. Implements the post* algorithm
     * The post* algorithm implemented is presented in Figure 3.4, Section 3.1.4 of S. Schwoon's PhD thesis (p. 48)
     * The modification to compute the repeated heads graph is explained in Section 3.2.3 of Schwoon's thesis
     * (see also Algorithm 4 in Figure 3.9, p. 81)
     */
    private void compute() {
        if (automaton != null) return;
        Set<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> trans =
                new HashSet<>();
        IndexedTransitions<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> rel =
                new IndexedTransitions<>();

        Configuration<Pair<Control, BuchiState>, Alphabet> initial = bps.initialConfiguration();
        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> initialHead = initial.getHead();
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> initialState =
                PAutomatonState.of(initialHead.getState());
        Stack<Alphabet> initialStack = initial.getFullStack();
        assert !initialStack.empty() : "We must have something to process";
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> finalState = null;
        for (Alphabet letter : initialStack) {
            finalState = new PAutomatonState<>();
            Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition1 =
                    Transition.of(initialState, letter, finalState);
            LabelledAlphabet<Control, Alphabet> newLabel = LabelledAlphabet.of(false);
            updateLabel(transition1, newLabel);
            if (initialState.isControlState()) {
                trans.add(transition1);
            } else {
                rel.add(transition1);
            }
            initialState = finalState;
        }
        LabelledAlphabet<Control, Alphabet> labelledLetter;

        while (!trans.isEmpty()) {
            Iterator<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> iterator
                    = trans.iterator();
            Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition = iterator.next();
            iterator.remove();
            Alphabet gamma = transition.getLetter();
            LabelledAlphabet<Control, Alphabet> oldLabel = transitionLabels.get(transition);
            assert oldLabel != null : "Each transition must have a label";
            if (!rel.contains(transition)) {
                Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> newTransition;
                rel.add(transition);
                boolean b = oldLabel.isRepeated();
                PAutomatonState<Pair<Control, BuchiState>, Alphabet> tp = transition.getStart();
                PAutomatonState<Pair<Control, BuchiState>, Alphabet> q = transition.getEnd();
                if (gamma != null) {
                    assert tp.isControlState() : "Expecting PDS state on the lhs of " + transition;
                    Pair<Control, BuchiState> p = tp.getState();
                    final ConfigurationHead<Pair<Control, BuchiState>, Alphabet> configurationHead
                            = ConfigurationHead.of(p, gamma);
                    Set<Rule<Pair<Control, BuchiState>, Alphabet>> rules =
                            bps.getRules(configurationHead);
                    for (Rule<Pair<Control, BuchiState>, Alphabet> rule : rules) {
                        Pair<Control, BuchiState> pPrime = rule.endState();
                        Stack<Alphabet> stack = rule.endStack();
                        assert stack.size() <= 2 : "At most 2 elements are allowed in the stack for now";
                        Alphabet gamma1, gamma2;
                        switch (stack.size()) {
                            case 0:
                                labelledLetter = LabelledAlphabet.of(b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        null, q);
                                trans.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                break;
                            case 1:
                                gamma1 = stack.peek();
                                labelledLetter = LabelledAlphabet.of(b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(
                                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        gamma1, q);
                                trans.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                break;
                            case 2:
                                gamma1 = stack.get(1);
                                gamma2 = stack.get(0);
                                PAutomatonState<Pair<Control, BuchiState>, Alphabet> qPPrimeGamma1
                                        = PAutomatonState.of(pPrime, gamma1);
                                labelledLetter = LabelledAlphabet.of(b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(
                                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        gamma1, qPPrimeGamma1);
                                trans.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                labelledLetter = LabelledAlphabet.of(false);
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(qPPrimeGamma1, gamma2, q);
                                rel.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                for (Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> t
                                        : rel.getBackEpsilonTransitions(qPPrimeGamma1)) {
                                    oldLabel = transitionLabels.get(t);
                                    labelledLetter = LabelledAlphabet.of(oldLabel.isRepeated());
                                    labelledLetter.setRule(rule);
                                    labelledLetter.setBackState(qPPrimeGamma1);
                                    newTransition = Transition.of(t.getStart(), gamma2, q);
                                    trans.add(newTransition);
                                    updateLabel(newTransition, labelledLetter);
                                }
                        }
                    }
                } else { // gamma == null --- epsilon transitions p - eps -> q   t :  q - gamma -> q' => p - gamma -> q'
                    for (Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> t
                            : rel.getFrontTransitions(q)) {
                        if (t.getLetter() == null) continue;
//                        if (t.getEnd().getLetter() != null) continue;
                        LabelledAlphabet<Control, Alphabet> tLetter = transitionLabels.get(t);
                        labelledLetter = LabelledAlphabet.of(
                                tLetter.isRepeated() || b);
                        labelledLetter.setBackState(q);
                        labelledLetter.setRule(tLetter.getRule());
                        newTransition = Transition.of(tp, t.getLetter(), t.getEnd());
                        trans.add(newTransition);
                        updateLabel(newTransition, labelledLetter);
                    }
                }
            }
        }

        automaton = new PAutomaton<>(
                rel.getTransitions(),
                initialState,
                Collections.singleton(finalState));

    }

    private Collection<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> getReachableHeads() {
        Collection<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> heads = new ArrayList<>();
        for (StateLetterPair<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> index : automaton.getTransitionHeads()) {
            PAutomatonState<Pair<Control, BuchiState>, Alphabet> pState = index.getState();
            if (pState.isControlState()) {
                heads.add(ConfigurationHead.of(pState.getState(), index.getLetter()));
            }
        }
        return heads;

    }

    private LabelledAlphabet<Control, Alphabet> updateLabel(Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition, LabelledAlphabet<Control, Alphabet> label) {
        LabelledAlphabet<Control, Alphabet> labelledLetter = transitionLabels.get(transition);
        if (labelledLetter == null) {
            transitionLabels.put(transition, label);
            labelledLetter = label;
        } else {
            labelledLetter.update(label);
        }
        transition.setLabel(labelledLetter);
        return labelledLetter;
    }

    /**
     * Implements Witness generation algorithm from Schwoon's thesis, Section 3.1.6
     * @param head A reachable configuration head
     * @return The path (of rules) from the initial configuraiton to {@code head}
     */
    public Deque<Rule<Pair<Control, BuchiState>, Alphabet>> getReachableConfiguration(
           ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head
    ) {
        Deque<Rule<Pair<Control, BuchiState>, Alphabet>> result = new ArrayDeque<>();
        //Step 1
        Deque<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> path = automaton.getPath(
                PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                head.getLetter(),
                automaton.getFinalStates().iterator().next());
        //Step 2
        Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition = path.removeFirst();
        LabelledAlphabet<Control, Alphabet> label = transitionLabels.get(transition);
        Rule<Pair<Control, BuchiState>, Alphabet> rule = label.getRule();
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState;
        while (rule != null) {
            if (rule.endStack().size() == 2) { // reduce step 3.2 to step 3.1 by shifting transition & rules
                transition = path.removeFirst();
                label = transitionLabels.get(transition);
                rule = label.getRule();
                assert rule != null && rule.endStack().size() == 2;
            }
            // Step 3.1
            result.addFirst(rule);
            head = rule.getHead();
            backState = label.getBackState();
            if (backState == null) {
                transition = Transition.of(
                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                        head.getLetter(),
                        transition.getEnd()
                );
                assert transitionLabels.containsKey(transition);
            } else {
                transition = Transition.of(
                        backState,
                        head.getLetter(),
                        transition.getEnd()
                );
                assert transitionLabels.containsKey(transition);
                path.addFirst(transition);
                transition = Transition.of(
                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                        null,
                        backState
                );
                assert transitionLabels.containsKey(transition);
            }

            label = transitionLabels.get(transition);
            rule = label.getRule();
        }
        return result;
    }

    @Override
    public Set<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> getTransitions(PAutomatonState<Pair<Control, BuchiState>, Alphabet> state, Alphabet letter) {
        return automaton.getTransitions(state, letter);
    }

    @Override
    public Set<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> getBackTransitions(PAutomatonState<Pair<Control, BuchiState>, Alphabet> state, Alphabet letter) {
        return automaton.getBackTransitions(state, letter);
    }

    @Override
    public PAutomatonState<Pair<Control, BuchiState>, Alphabet> initialState() {
        return automaton.initialState();
    }

    @Override
    public Set<PAutomatonState<Pair<Control, BuchiState>, Alphabet>> getFinalStates() {
        return automaton.getFinalStates();
    }

    @Override
    public Collection<Alphabet> getLetters() {
        return automaton.getLetters();
    }

    @Override
    public Collection<? extends StateLetterPair<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> getTransitionHeads() {
        return automaton.getTransitionHeads();
    }
}
