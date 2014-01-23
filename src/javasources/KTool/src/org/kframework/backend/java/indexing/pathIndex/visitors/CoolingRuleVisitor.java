package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.kil.loader.Context;

/**
 * Author: OwolabiL
 * Date: 1/20/14
 * Time: 12:40 PM
 */
public class CoolingRuleVisitor extends RuleVisitor{
    private Rule rule;


    public CoolingRuleVisitor(Rule rule, Context context) {
        super(context);
        this.rule = rule;
    }

    @Override
    public void visit(KSequence kSequence) {
        kSequence.get(0).accept(this);
        ((KItem)kSequence.get(1)).kLabel().accept(this);
    }

    //TODO(OwolabiL): This method can be greatly improved!
    @Override
    public void visit(Variable variable) {
        String requiredKResult = "isKResult(" + variable + ")";
        String firstSort;
        //TODO(OwolabiL): Remove this check and use concrete sort instead
        if (rule.requires().toString().contains(requiredKResult)) {
            firstSort = "KResult";
        } else {
            //TODO(OwolabiL): this should never happen!! throw exception?
            firstSort = variable.sort();
        }
        pString = pString.concat(firstSort+".1.");
    }

    @Override
    public void visit(KLabelFreezer kLabelFreezer) {
        kLabelFreezer.term().accept(this);
    }

    @Override
    public void visit(KItem kItem) {
        visit(kItem.kLabel());
        visit(kItem.kList());
        this.proceed = false;
    }

    @Override
    public void visit(KLabel kLabel) {
        pString = pString.concat(kLabel.toString()+".");
    }

    @Override
    public void visit(KList kList) {
        Term frozenTerm;
        for (int i = 0; i < kList.size(); i++) {
            frozenTerm = kList.get(i);
            //TODO(OwolabiL): remove instanceof!!
            if (frozenTerm instanceof Hole) {
                pStrings.add(pString+(i+1)+".HOLE");
            } else {
                //is it always a variable?
                pStrings.add(pString+(i+1)+SEPARATOR+ ((Variable) frozenTerm).sort());
            }
        }
    }
}
