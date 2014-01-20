package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: owolabi
 * Date: 1/20/14
 * Time: 12:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoolingRuleVisitor extends LocalVisitor{
    private Rule rule;
    private String pString;

    private List<String> pStrings;

    public CoolingRuleVisitor(Rule rule) {
        this.rule = rule;
        this.pString = "@.";
        this.pStrings = new ArrayList<>();
    }

    @Override
    public void visit(Rule rule) {
        visit(LookupCell.find(rule.leftHandSide(), "k"));
    }

    @Override
    public void visit(Cell cell) {
        visit((KSequence) cell.getContent());
    }

    @Override
    public void visit(KSequence kSequence) {
        visit((Variable)kSequence.get(0));
        visit((KLabelFreezer)((KItem)kSequence.get(1)).kLabel());
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
        visit((KItem)kLabelFreezer.term());
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
            if (frozenTerm instanceof Hole) {
                pStrings.add(pString+(i+1)+".HOLE");
            } else {
                //is it always a variable?
                pStrings.add(pString+(i+1)+"."+ ((Variable) frozenTerm).sort());
            }
        }
    }

    public List<String> getpStrings() {
        return pStrings;
    }
}
