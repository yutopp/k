package org.kframework.backend.pdmc.pda;

import com.google.common.collect.Multimap;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.JavaSymbolicKRun;
import org.kframework.backend.java.symbolic.LocalTransformer;
import org.kframework.backend.java.symbolic.PrePostTransformer;
import org.kframework.kil.ASTNode;
import org.kframework.krun.KRunExecutionException;
import org.kframework.krun.ioserver.filesystem.portable.PortableFileSystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Created by Traian on 11.06.2014.
 */
public class JavaKRunPushdownSystem  implements PushdownSystemInterface<Term, KItem> {

    private final JavaSymbolicKRun runner;
    private final Configuration<Term, KItem> initial;
    final TermContext termContext;

    public JavaKRunPushdownSystem(JavaSymbolicKRun runner, Term cfg) {

        this.runner = runner;
        initial = configurationSplit(cfg);
        termContext  = TermContext.of(runner.getDefinition(), new PortableFileSystem());
    }

    private Configuration<Term, KItem> configurationSplit(Term cfg) {
        KCellContentsReplacer replacer = new KCellContentsReplacer(null);
        Term state = (Term)cfg.accept(replacer);
        Term content = replacer.getContents();
        assert content instanceof KSequence : "Expecting KSequence but got " + content.getClass();
        KSequence sequence = (KSequence) content;
        Stack<KItem> kItems = new Stack<>();
        for (int i = sequence.size() - 1; i >= 0 ; i--) {
            Term term = sequence.get(i);
            assert term instanceof KItem : "Only accepting KItems as elements of K Sequences";
            kItems.push((KItem) term);
        }
        return new Configuration(state, kItems);
    }

    @Override
    public Configuration<Term, KItem> initialConfiguration() {
        return initial;
    }

    @Override
    public Set<Rule<Term, KItem>> getRules(ConfigurationHead<Term, KItem> configurationHead) {
        Term state = configurationHead.getState();
        KCellContentsReplacer replacer = new KCellContentsReplacer(configurationHead.getLetter());
        Term cfg = (Term) state.accept(replacer);
        assert replacer.getContents() == KSequence.EMPTY;
        ConstrainedTerm constrainedTerm = new ConstrainedTerm(cfg, termContext);
        Set<Rule<Term, KItem>> rules = new HashSet<>();
        try {
            Collection<ConstrainedTerm> nextTerms = runner.steps(constrainedTerm);
            for (ConstrainedTerm nextTerm : nextTerms) {
                Configuration<Term, KItem> nextCfg = configurationSplit(nextTerm.term());
                Rule<Term, KItem> rule = new Rule<>(configurationHead, nextCfg, nextCfg.getHead().getLetter());
                rules.add(rule);
            }

        } catch (KRunExecutionException e) {
            e.printStackTrace();
        }
        return rules;
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
        public KCellContentsReplacer(Term contents) {
            replacer = new LocalKCellContentsReplacer(contents);
            preTransformer.addTransformer(replacer);
        }

        public Term getContents() {
            return replacer.contents;
        }
    }
  
}
