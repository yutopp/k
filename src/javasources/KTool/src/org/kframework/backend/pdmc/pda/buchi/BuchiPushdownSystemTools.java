package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.automaton.AutomatonInterface;
import org.kframework.backend.pdmc.automaton.StateLetterPair;
import org.kframework.backend.pdmc.automaton.Transition;
import org.kframework.backend.pdmc.pda.ConfigurationHead;
import org.kframework.backend.pdmc.pda.PostStar;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.TrackingLabel;
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
            TrackingLabel<Pair<Control, BuchiState>, Alphabet>> repeatedHeadsGraph = null;

    TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> counterExample = null;
    public final static TarjanSCC NONE = new TarjanSCC();

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
     *     </ul></li>
     * </ul>
     * @return the value of the {@code repeatedHeadsGraph} field after computing it.
     */
    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> getRepeatedHeadsGraph() {
        if (repeatedHeadsGraph == null)
            computeRepeatedHeadsGraph();
        return repeatedHeadsGraph;
    }

    /**
     * Computes (if not already computed) and returns a witness of an accepting run of the BPDS.
     * This is represented as a strongly connected component of the * (reachable) repeated heads graph
     * containing at least one final buchi state.
     * @return the value of the {@code counterExample} field after computing it.
     */
    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> getCounterExampleGraph() {
        if (counterExample == null)
            computeCounterExample();
        return counterExample;
    }

    private void compute() {

        computeRepeatedHeadsGraph();

        computeCounterExample();

    }

    /**
     * Main method of the class. The post* algorithm {@link org.kframework.backend.pdmc.pda.PostStar} is instrumented
     * using the {@link org.kframework.backend.pdmc.pda.buchi.BuchiTrackingLabelFactory} to provide labels
     * to track information (passing through a final state) needed to produce the repeated heads graph.
     * The modification to compute the repeated heads graph is explained in Section 3.2.3 of Schwoon's thesis.
     */
    private void computeRepeatedHeadsGraph() {
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
                        TrackingLabel<Pair<Control, BuchiState>, Alphabet> tLabel = labelFactory.get(t);
                        TrackingLabel<Pair<Control, BuchiState>, Alphabet> backEpsilonLabel = labelFactory.newLabel();
                        backEpsilonLabel.update(tLabel);
                        backEpsilonLabel.setRule(rule);
                        backEpsilonLabel.update(bps, rule.getHead().getState());
                        backEpsilonLabel.setBackState(qPPrimeGamma1);
                        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> endV =
                                ConfigurationHead.of(t.getStart().getState(), gamma2);
                        repeatedHeadsGraph.addEdge(head, endV, backEpsilonLabel);
                        /*
                                    // This block is line 18 of Algorithm 2 (plus tracking information in labels)
                                    TrackingLabel<Control, Alphabet> tLabel = labelFactory.get(t);
                                    TrackingLabel<Control, Alphabet> backEpsilonLabel = labelFactory.newLabel();
                                    Transition<PAutomatonState<Control, Alphabet>, Alphabet> newBackEpsilonTransition =
                                            Transition.of(t.getStart(), gamma2, q);
                                    trans.add(newBackEpsilonTransition);
                                    labelFactory.updateLabel(newBackEpsilonTransition, backEpsilonLabel);
                                }
                         */

                    }
                }
            }
        }
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

    @SuppressWarnings("unchecked")
    private void computeCounterExample() {
        TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> repeatedHeadsGraph = getRepeatedHeadsGraph();
        Collection<Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>>.TarjanSCCVertex>> sccs = repeatedHeadsGraph.getStronglyConnectedComponents();
        for (Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>>.TarjanSCCVertex> scc : sccs) {
            TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> sccSubGraph = repeatedHeadsGraph.getSubgraph(scc);
            for (Map<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>> values : sccSubGraph.getEdgeSet().values()) {
                for (TrackingLabel<Pair<Control, BuchiState>, Alphabet> label: values.values()) {
                    assert label instanceof BuchiTrackingLabel;
                    if (((BuchiTrackingLabel<Control, Alphabet>) label).isRepeated()) {
                        counterExample = sccSubGraph;
                        return;
                    }
                }
            }
        }
        counterExample = NONE;
    }

    public Deque<Rule<Pair<Control, BuchiState>, Alphabet>> getRepeatingCycle(ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head) {
        TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>>
                counter = getCounterExampleGraph();
        if (counter == NONE) return null;
        Deque<DFSFrame> stack = new ArrayDeque<>();
        Set<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>> visited = new HashSet<>();
        Map<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, Map<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, TrackingLabel<Pair<Control, BuchiState>, Alphabet>>> edgeSet = counter.getEdgeSet();
        stack.add(new DFSFrame(head, null, edgeSet.get(head).entrySet().iterator(), false));
        visited.add(head);
        while (!stack.isEmpty()) {
            DFSFrame top = stack.peekLast();
            if (!top.nextEntry.hasNext()) {
                stack.pop(); visited.remove(head);
            } else {
                Map.Entry<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                TrackingLabel<Pair<Control, BuchiState>, Alphabet>> nextEntry = top.nextEntry.next();
                ConfigurationHead<Pair<Control, BuchiState>, Alphabet> nextHead = nextEntry.getKey();
                boolean repeated = ((BuchiTrackingLabel<Control,Alphabet>) nextEntry.getValue()).isRepeated();
                if (!visited.contains(nextHead)) {
                    stack.add(new DFSFrame(nextHead, nextEntry.getValue(), edgeSet.get(nextHead).entrySet().iterator(), repeated));
                    visited.add(nextHead);
                } else {
                    if (repeated || top.repeating) {
                        stack.add(new DFSFrame(nextHead, nextEntry.getValue(), edgeSet.get(nextHead).entrySet().iterator(), repeated));
                        break;
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            Deque<Rule<Pair<Control, BuchiState>, Alphabet>> rules = new ArrayDeque<>();
            for (DFSFrame frame : stack) {
                if (frame.label != null) {
                    rules.add(frame.label.getRule());
                }
            }
            return rules;
        }
        return null;
    }

    public Deque<Rule<Pair<Control, BuchiState>, Alphabet>> getReachableConfiguration(ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head) {
        return postStar.getReachableConfiguration(head);
    }

    public class DFSFrame {
        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> data;
        TrackingLabel<Pair<Control, BuchiState>, Alphabet> label;
        Iterator<Map.Entry<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                TrackingLabel<Pair<Control, BuchiState>, Alphabet>>> nextEntry;
        boolean repeating;

        public DFSFrame(ConfigurationHead<Pair<Control, BuchiState>, Alphabet> data,
                        TrackingLabel<Pair<Control, BuchiState>, Alphabet> label,
                        Iterator<Map.Entry<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                                TrackingLabel<Pair<Control, BuchiState>, Alphabet>>> nextEntry,
                        boolean repeating) {
            this.data = data;
            this.label = label;
            this.nextEntry = nextEntry;
            this.repeating = repeating;
        }
    }
}
