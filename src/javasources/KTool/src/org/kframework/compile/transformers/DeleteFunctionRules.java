package org.kframework.compile.transformers;

import org.kframework.kil.*;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;

import java.util.Set;
import java.util.List;

/**
 *
 * Delete all rules which define the hooked functions specified in the given set of hooks.
 *
 * @author TraianSF
 */
public class DeleteFunctionRules extends CopyOnWriteTransformer {
    private final Set<String> hooks;

    public DeleteFunctionRules(Context context) {
        super("Delete function rules for given function symbols", context);
        this.hooks = context.getHookedProperties().stringPropertyNames();
    }

    @Override
    public ASTNode transform(Rule node) throws TransformerException {
        if (node.containsAttribute(Attribute.SIMPLIFICATION_KEY)) {
            return node;
        }
        Term body = node.getBody();
        if (body instanceof Rewrite) {
            body = ((Rewrite) body).getLeft();
        }
        Production prod = null;
        if (body instanceof TermCons) {
            prod = context.conses.get(((TermCons) body).getCons());
        } else if (body instanceof KApp) {
            Term l = ((KApp) body).getLabel();
            if (!(l instanceof KLabelConstant)) return node;
            String label = ((KLabelConstant) l).getLabel();
            List<Production> prods = context.productionsOf(label);
            if (prods.size() != 1) {
                return node;
//                GlobalSettings.kem.register(new KException(
//                        KException.ExceptionType.ERROR,
//                        KException.KExceptionGroup.CRITICAL,
//                        "Hooked functions for label " + label + " should not be overloaded: " + prods.toString(),
//                        getName(),
//                        node.getFilename(),
//                        node.getLocation()));
            } // Hooked functions should not be overloaded
            prod = prods.get(0);
        }
        if (prod == null) {
            return node;
        }
        if (prod.containsAttribute(Attribute.HOOK_KEY)) {
            final String hook = prod.getAttribute(Attribute.HOOK_KEY);
            if (hooks.contains(hook)) {
                return null;
            }
        }
        if (prod.containsAttribute(Attribute.AUX_HOOK_KEY)) {
            final String hook = prod.getAttribute(Attribute.AUX_HOOK_KEY);
            if (hooks.contains(hook)) {
                return null;
            }
        }
        return node;
    }
}
