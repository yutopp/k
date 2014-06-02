package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.automaton.AutomatonInterface;
import org.kframework.backend.pdmc.automaton.StateLetterPair;
import org.kframework.backend.pdmc.automaton.Transition;
import org.kframework.backend.pdmc.pda.ConfigurationHead;
import org.kframework.backend.pdmc.pda.PostStar;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.graph.TarjanSCC;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

import java.util.*;

/**
 * Implements some pushdown model-checking related algorithms from  Stefan Schwoon's Phd Thesis:
 * S. Schwoon.  Model-Checking Pushdown Systems.  Ph.D. Thesis, Technische Universitaet Muenchen, June 2002.
 *
 * @author TraianSF
 */
public class BuchiPushdownSystemTools<Control, Alphabet> {

    BuchiPushdownSystemInterface<Control, Alphabet> bps;
    BuchiTrackingLabelFactory<Control, Alphabet> labelFactory;

    public BuchiPushdownSystemTools(BuchiPushdownSystemInterface<Control, Alphabet> bps) {
        this.bps = bps;
    }

    private org.kframework.backend.pdmc.pda.PostStar<Pair<Control, BuchiState>, Alphabet> postStar = null;
    TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
            BuchiTrackingLabel<Control, Alphabet>> repeatedHeadsGraph = null;

    TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>> counterExample = null;

    /**
     * Computes (if not already computed) and returns the <b>post</b> automaton corresponding to this BPDS.
     * <ul>
     *    <li>
     *        The states of this automaton include all states of the BPDS reachable from the initial configuration;
     *    </li>
     *    <li>
     *        Any path q -w->* f from a BPDS state to a final state encodes the reachable configuration (q,w).
     *    </li>
     * </ul>
     * @return the value of the {@code postStar} field after computing it.
     */
    public AutomatonInterface<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> getPostStar() {
        if (postStar == null)
            compute();
        return postStar;
    }

    /**
     * Computes (if not already computed) and returns the repeated heads graph of the BPDS.
     * This graph is comprised of:
     * <ul>
     *     <li> Vertices represent <b>all</b> configuration heads reachable from the original configuration</li>
     *     <li> Edges indicate reachability (using rules of the BPDS) between configuration heads  </li>
     *     <li> Labels contain: <ul>
     *          <li> information about passing through final states of the Buchi Automaton</li>
     *          <li> </li>
     *     </ul></li>
     * </ul>
     * This is represented as a strongly connected component of the * (reachable) repeated heads graph
     * containing at least one final buchi state.
     * @return the value of the {@code counterExample} field after computing it.
     */
    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>> getRepeatedHeadsGraph() {
        if (repeatedHeadsGraph == null)
            compute();
        return repeatedHeadsGraph;
    }

    /**
     * Computes (if not already computed) and returns a witness of an accepting run of the BPDS.
     * This is represented as a strongly connected component of the * (reachable) repeated heads graph
     * containing at least one final buchi state.
     * @return the value of the {@code counterExample} field after computing it.
     */
    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>> getCounterExampleGraph() {
        if (repeatedHeadsGraph == null)
            compute();
        return counterExample;
    }

    /**
     * Main method of the class. Implements the post* algorithm instrumented to also produce the repeated heads graph.
     * The post* algorithm implemented is presented in Figure 3.4, Section 3.1.4 of S. Schwoon's PhD thesis (p. 48)
     * The modification to compute the repeated heads graph is explained in Section 3.2.3 of Schwoon's thesis
     * (see also Algorithm 4 in Figure 3.9, p. 81)
     */
    private void compute() {

        labelFactory = new BuchiTrackingLabelFactory<>();
        postStar = new PostStar<>(bps, labelFactory);

        repeatedHeadsGraph = new TarjanSCC<>();

        Collection<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> reachableHeads = getReachableHeads(postStar);

        for (ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head : reachableHeads) {
            for (Rule<Pair<Control,BuchiState>,Alphabet> rule : bps.getRules(head)) {
                ConfigurationHead<Pair<Control, BuchiState>, Alphabet> endHead = rule.endConfiguration().getHead();
                if (!endHead.isProper()) continue;
                BuchiTrackingLabel<Control, Alphabet> label = new BuchiTrackingLabel<>(bps.isFinal(head.getState()));
                label.setRule(rule);
                repeatedHeadsGraph.addEdge(head, endHead, label);
                Stack<Alphabet> endStack = rule.endStack();
                if (endStack.size() == 2) {
                    Alphabet gamma1 = endStack.get(1);
                    Alphabet gamma2 = endStack.get(0);
                    PAutomatonState<Pair<Control, BuchiState>, Alphabet> qPPrimeGamma1
                            = PAutomatonState.of(endHead.getState(), gamma1);
                    for (Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> t
                            : postStar.getBackEpsilonTransitions(qPPrimeGamma1)) {
                        BuchiTrackingLabel<Control, Alphabet> oldLabel = (BuchiTrackingLabel<Control, Alphabet>) labelFactory.get(t);
                        BuchiTrackingLabel<Control, Alphabet> labelledLetter = new BuchiTrackingLabel<>(oldLabel.isRepeated());
                        labelledLetter.setRule(rule);
                        labelledLetter.setBackState(qPPrimeGamma1);
                        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> endV =
                                ConfigurationHead.of(t.getStart().getState(), gamma2);
                        repeatedHeadsGraph.addEdge(head, endV, labelledLetter);
                    }
                }
            }
        }

        computeCounterExample();

    }

    private Collection<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> getReachableHeads(AutomatonInterface<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> postStar) {
        Collection<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> heads = new ArrayList<>();
        for (StateLetterPair<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> index : postStar.getTransitionHeads()) {
            PAutomatonState<Pair<Control, BuchiState>, Alphabet> pState = index.getState();
            if (pState.isControlState()) {
                heads.add(ConfigurationHead.of(pState.getState(), index.getLetter()));
            }
        }
        return heads;

    }

    private void computeCounterExample() {
        Collection<Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>>.TarjanSCCVertex>> sccs = repeatedHeadsGraph.getStronglyConnectedComponents();
        for (Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>>.TarjanSCCVertex> scc : sccs) {
            TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>> sccSubGraph = repeatedHeadsGraph.getSubgraph(scc);
            for (Map<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, BuchiTrackingLabel<Control, Alphabet>> values : sccSubGraph.getEdgeSet().values()) {
                for (BuchiTrackingLabel<Control, Alphabet> label: values.values()) {
                    if (label.isRepeated()) {
                        counterExample = sccSubGraph;
                        return;
                    }
                }
            }
        }
    }

    /**
     * Implements Witness generation algorithm from Schwoon's thesis, Section 3.1.6
     * @param head A reachable configuration head
     * @return The path (of rules) from the initial configuration to {@code head}
     */
    public Deque<Rule<Pair<Control, BuchiState>, Alphabet>> getReachableConfiguration(
           ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head
    ) {
        Deque<Rule<Pair<Control, BuchiState>, Alphabet>> result = new ArrayDeque<>();
        //Step 1
        Deque<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> path = postStar.getPath(
                PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                head.getLetter(),
                postStar.getFinalStates().iterator().next());
        //Step 2
        Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition = getUncompressedTransition(path);
        BuchiTrackingLabel<Control, Alphabet> label = (BuchiTrackingLabel<Control, Alphabet>) labelFactory.get(transition);
        Rule<Pair<Control, BuchiState>, Alphabet> rule = label.getRule();
        assert label.getBackState() == null;
        while (rule != null) {
            if (rule.endStack().size() == 2) { // reduce step 3.2 to step 3.1 by shifting transition & rules
                // First transitions: p -a-> q<p,a> -b-> q            [...  -w->* fin  ]
                // labeling rule: <p',c>  => <p, a b>
                // then add  p' -c-> q              [... -w->* fin
                assert transition.getLetter()!=null;
                assert transition.getEnd().getLetter()!= null; // this is an intermediate stare
                transition = getUncompressedTransition(path);
                head = rule.getHead();
                transition = Transition.of(
                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                        head.getLetter(),
                        transition.getEnd()
                );
                assert transition.getLetter()!=null;
                path.addFirst(transition);
            } else {
                // First transition: p -w-> q
                // labeling rule: <p',c> => <p,w>
                // then add p' -c-> q
                head = rule.getHead();
                transition = Transition.of(
                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()),
                        head.getLetter(),
                        transition.getEnd()
                );
                path.addFirst(transition);
            }

            // Add rule to list of rules
            result.addFirst(rule);
            transition = getUncompressedTransition(path);
            assert labelFactory.get(transition) != null;
            label = (BuchiTrackingLabel<Control, Alphabet>) labelFactory.get(transition);
            rule = label.getRule();
            assert label.getBackState() == null;
        }
        return result;
    }

    private Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> getUncompressedTransition(Deque<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> path) {
        Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition = path.removeFirst();
        BuchiTrackingLabel<Control, Alphabet> label;
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState;
        label = (BuchiTrackingLabel<Control, Alphabet>) labelFactory.get(transition);
        backState = label.getBackState();
        if (backState != null) {
            Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition2 =
                    Transition.of(backState, transition.getLetter(), transition.getEnd());
            assert labelFactory.get(transition2) != null;
            path.addFirst(transition2);
            transition = Transition.of(transition.getStart(), null, backState);
            assert labelFactory.get(transition) != null;
        }
        return transition;
    }

}
