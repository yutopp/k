package org.kframework.backend.acl2;

import java.io.StringWriter;
import java.util.List;
// import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import org.kframework.backend.provers.ast.BoolBuiltin;
import org.kframework.backend.provers.ast.FromTerm;
import org.kframework.backend.provers.ast.KApp;
import org.kframework.backend.provers.ast.KItem;
import org.kframework.backend.provers.ast.Cell;
import org.kframework.backend.provers.ast.Render;
import org.kframework.kil.Attribute;
import org.kframework.kil.Configuration;
import org.kframework.kil.ModuleItem;
import org.kframework.kil.Production;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;

public final class GenerateAcl2TransitionFunction {
    private GenerateAcl2TransitionFunction() {
    }

    public static void generateAcl2TransitionFunction(Acl2Context context,
            Acl2Definition def) {
        StringWriter w = def.getLispWriter();

        w.write("(include-book \"seqmatch\")\n"
                + "(make-event (K::define-sort-predicates '|| "
                  + "("+context.getGrammarName()+")))\n");
        final String modName = def.getDefinition().getMainModule();
        w.write(";; Return a successor in the semantics, or nil if the input is illegal/stuck\n");
        w.write("(defun " + modName + "-step (cfg)\n"
               +"  (declare (xargs :normalize nil))\n"
               +"  (seq-match cfg\n");

        Configuration cfg = null;
        for (ModuleItem mi : def.getDefinition().getSingletonModule()
                .getItems()) {
            if (mi instanceof Rule) {
                Rule r = (Rule) mi;
                Rewrite rew = (Rewrite) r.getBody();
                Cell plhs = FromTerm.convertCell((org.kframework.kil.Cell)rew.getLeft());
                Cell prhs = FromTerm.convertCell((org.kframework.kil.Cell)rew.getRight());
                w.append("    ((");
                String label = r.getLabel();
                if (!"".equals(label)) {
                    w.append("; "+label+"\n      ");
                }
                generateCellPattern(context, w, plhs);
                if (r.getRequires() != null) {
                    w.append("\n     (");
                    emitPredicate(context, def,
                            FromTerm.convertKItem(r.getRequires()));
                    w.append(" test ('|'TRUE|))");
                }
                w.append(")\n     ");
                w.append(Render.cellExpression(context, prhs));
                w.append(")\n");
            } else if (mi instanceof Configuration) {
                cfg = (Configuration)mi;
            }
        }
        if (cfg == null) {
            throw new IllegalArgumentException("Configuration not defined in definition "+def);
        }

        w.append("   ))\n");
        // Generate a function to run a maximum number of steps
        // The hint prevents the prover from unfolding the step functions
        // while trying to establish termination.
        w.write(";; Take up to n steps in the semantics, returning the last configuration seen\n"
                +";; The hint prevents termination checking from wasting time unfolding the step function\n");
        w.write(""
                + "(defun " + modName + "-steps (n cfg)\n"
                + "  (declare (xargs :hints ((\"Goal\" :in-theory (disable "+modName+"-step)))))\n"
                + "  (if (or (not (natp n)) (zp n))\n" + "    cfg\n"
                + "    (let ((next-cfg (" + modName + "-step cfg)))\n"
                + "      (if next-cfg\n"
                + "        (" + modName
                + "-steps (1- n) next-cfg)\n" + "        cfg))))\n");
        // Define a :program mode function that runs the configuration as long as
        // possible, perhaps diverging.
        w.write(";; A possibly-diverging function that runs the configuration until it terminates\n");
        w.write(""
                + "(defun " + modName+"-run (cfg)\n"
                + "  (declare (xargs :mode :program))\n"
                + "    (let ((next-cfg (" + modName + "-step cfg)))\n"
                + "      (if next-cfg\n"
                + "        (" + modName + "-run next-cfg)\n"
                + "        cfg)))");
        w.write(";; Wraps an initial program in the rest of the starting configuration. Used by krun\n");
           w.write("(defun " + modName + "-start (|$PGM|)\n");
           w.append("  ");
           w.append(Render.cellExpression(context, FromTerm.convertCell((org.kframework.kil.Cell)cfg.getBody())));
           w.append(")\n");
    }

    private static void generateCellPattern(Acl2Context context, StringWriter w, Cell rawPat) {
        Pair<Cell, List<Acl2SeqmatchItem>> extractedPatterns = ExtractMapPatterns.extractMapPatterns(rawPat);
        Cell pat = extractedPatterns.getLeft();
        w.append(Render.cellPattern(pat));
        for (Acl2SeqmatchItem item : extractedPatterns.getRight()) {
            w.append("\n     ((K::map-lookup ");
            w.append(Render.kExpression(context, item.key));
            w.append(" "+item.matched+") "+item.tempName+" ((& . ");
            w.append(Render.kPattern(item.value));
            w.append(")");
            if (item.rest != null) {
                w.append(" . "+item.rest);
            }
            w.append("))");
        }
    }

    /**
     * Convert side conditions to ACL2
     */
    private static void emitPredicate(Acl2Context context, Acl2Definition def,
            KItem t) {
        if (t instanceof KApp) {
            KApp app = (KApp) t;
            if (app.klabel.equals("'#andBool")) {
                def.getLispWriter().append("(predicate-and ");
                for (KItem child : app.args) {
                    emitPredicate(context, def, child);
                }
                def.getLispWriter().append(")");
            } else if (app.klabel.equals("'_=/=K_")) {
                if (app.args.size() != 2) {
                    throw new IllegalArgumentException(
                            "Found application of '_=/=K_ to "
                                    + app.args.size()
                                    + " arguments, expected 2");
                }
                KItem pred = app.args.get(0);
                KItem expected = app.args.get(1);
                if (!(expected instanceof BoolBuiltin)) {
                    throw new IllegalArgumentException("Found application of '_=/=K_ with non-constant RHS");
                } else {
                    boolean value = ((BoolBuiltin)expected).value;
                    if (value) {
                        def.getLispWriter().append("(not-bool ");
                        emitPredicate(context, def, pred);
                        def.getLispWriter().append(")");
                    } else {
                        emitPredicate(context, def, pred);                        
                    }
                }
            } else {
                Set<Production> productions = context.context.productions
                        .get(app.klabel);
                if (productions == null) {
                    throw new IllegalArgumentException("Unknown label "
                            + app.klabel);
                } else if (productions.size() > 1) {
                    throw new IllegalArgumentException(
                            "Label with multiple productions " + app.klabel);
                }
                Production p = productions.iterator().next();
                String hook = p.getAttribute(Attribute.HOOK_KEY);
                if (hook == null) {
                    throw new IllegalArgumentException(
                            "Unhooked predicate label " + app.klabel);
                }
                String impl = context.getHookImplementation(hook);
                if (impl == null) {
                    throw new IllegalArgumentException(
                            "No implementation for hook " + hook + " of label "
                                    + app.klabel);
                } else {
                    def.getLispWriter().append(Render.kExpression(context, t));
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Can't handle non-KApp condition " + t);
        }
    }
}
