package org.kframework.backend.pdmc.pda;

import com.google.common.collect.ImmutableList;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.JavaSymbolicKRun;
import org.kframework.backend.java.symbolic.LocalTransformer;
import org.kframework.backend.java.symbolic.PrePostTransformer;
import org.kframework.kil.ASTNode;
import org.kframework.krun.KRunExecutionException;
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
        termContext  = TermContext.of(runner.getDefinition(), new PortableFileSystem());
        initial = configurationSplit(cfg);
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
    public Set<Rule<Term, Term>> getRules(ConfigurationHead<Term, Term> configurationHead) {
        Term cfg = getKConfig(configurationHead.getState(), configurationHead.getLetter());
        ConstrainedTerm constrainedTerm = new ConstrainedTerm(cfg, termContext);
        Set<Rule<Term, Term>> rules = new HashSet<>();
        try {
            Collection<ConstrainedTerm> nextTerms = runner.steps(constrainedTerm);
            for (ConstrainedTerm nextTerm : nextTerms) {
                Configuration<Term, Term> nextCfg = configurationSplit(nextTerm.term());
                Rule<Term, Term> rule = new Rule<>(configurationHead, nextCfg, nextCfg.getHead().getLetter());
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
