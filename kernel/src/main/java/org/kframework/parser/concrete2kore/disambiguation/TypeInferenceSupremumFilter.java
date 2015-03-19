// Copyright (c) 2012-2015 K Team. All Rights Reserved.
package org.kframework.parser.concrete2kore.disambiguation;

import org.kframework.POSet;
import org.kframework.definition.NonTerminal;
import org.kframework.definition.Production;
import org.kframework.definition.ProductionItem;
import org.kframework.definition.RegexTerminal;
import org.kframework.definition.Terminal;
import org.kframework.kore.Sort;
import org.kframework.parser.Ambiguity;
import org.kframework.parser.ProductionReference;
import org.kframework.parser.SafeTransformer;
import org.kframework.parser.Term;
import org.kframework.utils.errorsystem.ParseFailedException;
import scala.collection.Iterator;

import java.util.HashSet;
import java.util.Set;

/**
 * Find the most general production from a list of ambiguities.
 * A production is considered a to be overloaded if all Terminals are equal and in the same place,
 * and NonTerminals are subsorted.
 */
// TODO: (radum) not sure if it is safe to use
public class TypeInferenceSupremumFilter extends SafeTransformer {

    private POSet<Sort> subsorts;
    public TypeInferenceSupremumFilter(POSet<Sort> subsorts) {
        this.subsorts = subsorts;
    }

    @Override
    public org.kframework.parser.Term apply(Ambiguity amb) throws ParseFailedException {
        Set<Term> maxterms = new HashSet<>();
        for (Term t : amb.items()) {
            boolean max = true;
            if (t instanceof ProductionReference) {
                for (Term t2 : amb.items()) {
                    if (t != t2 && (t2 instanceof ProductionReference)
                            && isSubsorted(((ProductionReference) t2).production(), ((ProductionReference) t).production())) {
                        max = false;
                    }
                }
            }
            if (max)
                maxterms.add(t);
        }

        if (maxterms.size() == 1) {
            return this.apply(maxterms.iterator().next());
        } else if (maxterms.size() > 1)
            amb.replaceChildren(maxterms);

        return super.apply(amb);
    }

    private boolean isSubsorted(Production big, Production small) {
        if (big == small)
            return false;
        if (big.items().size() != small.items().size())
            return false;
        if (!subsorts.greaterThenEq(big.sort(), small.sort()))
            return false;
        if (big.att().contains("token") || small.att().contains("token"))
            return false;
        Iterator<ProductionItem> itBig = big.items().toIterable().iterator();
        Iterator<ProductionItem> itSmall = small.items().toIterable().iterator();
        while (itBig.hasNext()) {
            ProductionItem bigItem = itBig.next();
            assert(itSmall.hasNext());
            ProductionItem smallItem = itSmall.next();

            if (!bigItem.getClass().equals(smallItem.getClass())) return false;
            if (bigItem instanceof Terminal || bigItem instanceof RegexTerminal) return bigItem.equals(smallItem);
            if (!(bigItem instanceof NonTerminal))
                throw new AssertionError("Did not expect ProductionItem of type: " + bigItem.getClass().toString());
            if (!subsorts.greaterThenEq(((NonTerminal)bigItem).sort(), ((NonTerminal)smallItem).sort())) return false;
        }
        return true;
    }
}
