package org.kframework.backend.pdmc.k;

import com.google.common.collect.ImmutableList;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.kil.Rule;
import org.kframework.backend.java.symbolic.JavaSymbolicKRun;
import org.kframework.backend.java.symbolic.LocalTransformer;
import org.kframework.backend.java.symbolic.PrePostTransformer;
import org.kframework.backend.pdmc.automaton.AutomatonInterface;
import org.kframework.backend.pdmc.pda.*;
import org.kframework.backend.pdmc.pda.buchi.BuchiPushdownSystem;
import org.kframework.backend.pdmc.pda.buchi.BuchiPushdownSystemTools;
import org.kframework.backend.pdmc.pda.buchi.BuchiState;
import org.kframework.backend.pdmc.pda.buchi.PromelaBuchi;
import org.kframework.backend.pdmc.pda.graph.TarjanSCC;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;
import org.kframework.kil.ASTNode;
import org.kframework.kil.loader.Context;
import org.kframework.krun.KRunExecutionException;
import org.kframework.krun.api.KRunProofResult;
import org.kframework.krun.api.KRunState;
import org.kframework.krun.api.Transition;
import org.kframework.krun.ioserver.filesystem.portable.PortableFileSystem;

import java.util.*;
import java.util.Collection;

/**
 * Created by Traian on 11.06.2014.
 */
public class JavaKRunPushdownSystem  implements PushdownSystemInterface<Term, Term> {

    private final JavaSymbolicKRun runner;
    private final Configuration<Term, Term> initial;
    final TermContext termContext;

    public JavaKRunPushdownSystem(JavaSymbolicKRun runner, Term cfg) {

        this.runner = runner;
        runner.initialSimulationRewriter();
        GlobalContext globalContext = new GlobalContext(runner.getDefinition(), new PortableFileSystem());
        termContext = TermContext.of(globalContext);
        initial = configurationSplit(cfg);
    }

    public KRunProofResult<DirectedGraph<KRunState, Transition>> pdmc(PromelaBuchi automaton) throws KRunExecutionException {
        JavaKRunPromelaEvaluator evaluator = new JavaKRunPromelaEvaluator(this);
        BuchiPushdownSystem<Term, Term> buchiPushdownSystem = new BuchiPushdownSystem<>(this, automaton, evaluator);

        BuchiPushdownSystemTools<Term, Term> bpsTool = new BuchiPushdownSystemTools<>(buchiPushdownSystem);

        if (runner.getDefinition().context().globalOptions.verbose) {
            AutomatonInterface<PAutomatonState<Pair<Term, BuchiState>, Term>, Term> post = bpsTool.getPostStar();
            System.err.println("\n\n\n----Post Automaton----");
            System.err.println(post.toString());

            TarjanSCC repeatedHeads = bpsTool.getRepeatedHeadsGraph();
            System.err.println("\n\n\n----Repeated Heads----");
            System.err.println(repeatedHeads.toString());

            System.err.println("\n\n\n----Strongly Connected Components----");
            System.err.println(repeatedHeads.getSCCSString());
        }

        TarjanSCC<ConfigurationHead<Pair<Term, BuchiState>, Term>, TrackingLabel<Pair<Term, BuchiState>, Term>> counterExampleGraph
                = bpsTool.getCounterExampleGraph();
        if (counterExampleGraph == BuchiPushdownSystemTools.NONE) {
            System.out.println("No counterexample found. Property holds.");
        } else {
            System.out.println("Counterexample found for the given property");
            ConfigurationHead<Pair<Term, BuchiState>, Term> head = counterExampleGraph.getVertices().iterator().next();
            System.out.println("\n\n\n--- Prefix path ---");


            Term kStartConfig = this.getKConfig(head.getState().getKey(), head.getLetter());
            System.out.println(runner.getKRunResult(new ConstrainedTerm(kStartConfig, termContext)));
            Deque<org.kframework.backend.pdmc.pda.Rule<Pair<Term, BuchiState>, Term>> reachableConfigurationPath = bpsTool.getReachableConfiguration(head);
            for(org.kframework.backend.pdmc.pda.Rule<Pair<Term, BuchiState>, Term> rule : reachableConfigurationPath) {
                printRuleInfo(rule);
            }
            System.out.println("---START CYCLE---");
            Deque<org.kframework.backend.pdmc.pda.Rule<Pair<Term, BuchiState>, Term>> repeatingCycle = bpsTool.getRepeatingCycle(head);
            for (org.kframework.backend.pdmc.pda.Rule<Pair<Term, BuchiState>, Term> rule : repeatingCycle) {
                printRuleInfo(rule);
            }
            System.out.println("---END CYCLE---");
        }
        DirectedGraph<KRunState, Transition> kRunStateTransitionDirectedSparseGraph = new DirectedSparseGraph<>();
        return new KRunProofResult<>(false, kRunStateTransitionDirectedSparseGraph, runner.getDefinition().context());

    }

    private void printRuleInfo(org.kframework.backend.pdmc.pda.Rule<Pair<Term, BuchiState>, Term> rule) {
        System.out.println("==[" + rule.getLabel().toString() + "]==>");
        Configuration<Pair<Term, BuchiState>, Term> endConfiguration = rule.endConfiguration();
        Term kEndConfig = this.getKConfig(new Configuration<>(endConfiguration.getHead().getState().getKey(), endConfiguration.getFullStack()));
        System.out.println(runner.getKRunResult(new ConstrainedTerm(kEndConfig, termContext)));
    }

    private Configuration<Term, Term> configurationSplit(Term cfg) {
        KCellContentsReplacer replacer = new KCellContentsReplacer(termContext, null);
        Term state = (Term)cfg.accept(replacer);
        Term content = replacer.getContents();
        Stack<Term> kItems = new Stack<>();
        if (content instanceof KSequence) {
            KSequence sequence = (KSequence) content;
            for (int i = sequence.size() - 1; i >= 0; i--) {
                Term term = sequence.get(i);
                assert term.isGround() : "Only accepting ground terms as elements of the stack";
                kItems.push(term);
            }
        } else {
            assert content.isGround() : "Only accepting ground terms as elements of the stack";
            kItems.push(content);
        }
        return new Configuration<>(state, kItems);
    }

    @Override
    public Configuration<Term, Term> initialConfiguration() {
        return initial;
    }

    @Override
    public Set<org.kframework.backend.pdmc.pda.Rule<Term, Term>> getRules(ConfigurationHead<Term, Term> configurationHead) {
        Term cfg = getKConfig(configurationHead.getState(), configurationHead.getLetter());
        ConstrainedTerm constrainedTerm = new ConstrainedTerm(cfg, termContext);
        Set<org.kframework.backend.pdmc.pda.Rule<Term, Term>> rules = new HashSet<>();
        try {
            Collection<ConstrainedTerm> nextTerms = runner.steps(constrainedTerm);
            for (ConstrainedTerm nextTerm : nextTerms) {
                Configuration<Term, Term> nextCfg = configurationSplit(nextTerm.term());
                org.kframework.backend.pdmc.pda.Rule<Term, Term> rule = new org.kframework.backend.pdmc.pda.Rule<>(configurationHead, nextCfg, nextTerm.getRule().label());
                rules.add(rule);
            }

        } catch (KRunExecutionException e) {
            e.printStackTrace();
        }
        return rules;
    }

    public Term getKConfig(Term state, Term... letter) {
        if (letter.length == 1 && letter[0] == null) return state;
        KCellContentsReplacer replacer = new KCellContentsReplacer(termContext, new KSequence(ImmutableList.copyOf(letter)));

        Term cfg = (Term) state.accept(replacer);
        assert replacer.getContents() == KSequence.EMPTY;
        return cfg;
    }

    public Term getKConfig(Configuration<Term, Term> config) {
        Stack<Term> stack = config.getFullStack();
        Term[] terms = new Term[stack.size()];
        Collections.reverse(stack);
        terms = stack.toArray(terms);

        return getKConfig(config.getHead().getState(), terms);
    }

    class KCellContentsReplacer extends PrePostTransformer {
        class LocalKCellContentsReplacer extends LocalTransformer {
            Term contents;
            public LocalKCellContentsReplacer(Term contents) {
                if (contents == null) contents = KSequence.EMPTY;
                this.contents = contents;
            }
            @Override
            public ASTNode transform(Cell cell) {
                if (! cell.getLabel().equals("k"))
                    return super.transform(cell);
                Term newContents = contents;
                contents = cell.getContent();
                return new DoneTransforming(new Cell<>("k", newContents));
            }
         }

        LocalKCellContentsReplacer replacer;
        public KCellContentsReplacer(TermContext context, Term contents) {
            super(context);
            replacer = new LocalKCellContentsReplacer(contents);
            preTransformer.addTransformer(replacer);
        }

        public Term getContents() {
            return replacer.contents;
        }
    }
  
}
