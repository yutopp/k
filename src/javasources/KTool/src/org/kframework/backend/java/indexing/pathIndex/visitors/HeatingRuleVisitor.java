package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;
import org.kframework.kil.loader.Context;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: owolabi
 * Date: 1/20/14
 * Time: 10:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class HeatingRuleVisitor extends LocalVisitor {
    private String pString;
    private final Rule rule;
    private final Context context;
    private int counter = 0;

    private List<String> pStrings;

    public HeatingRuleVisitor(Rule rule, Context context) {
        this.rule = rule;
        this.context = context;
        this.pStrings = new ArrayList<>();
        pString = "@.";
    }

    @Override
    public void visit(Rule rule) {
        visit(LookupCell.find(rule.leftHandSide(), "k"));
    }

    @Override
    public void visit(Cell cell) {
        visit((KSequence)cell.getContent());
    }

    @Override
    public void visit(KSequence kSequence) {
        visit((KItem)kSequence.get(0));
    }

    @Override
    public void visit(KItem kItem) {
        visit(kItem.kLabel());
        visit(kItem.kList());
    }

    @Override
    public void visit(KLabel kLabel) {
        pString = pString.concat(kLabel.toString()+".");
    }

    @Override
    public void visit(KList kList) {
        for (int i = 0; i < kList.size(); i++) {
            counter = i+1;
            visit((Variable)kList.get(i));
        }
        this.proceed = false;
    }

    @Override
    public void visit(Variable variable) {
        String sort;
        if(isRequiredToBeKResult(variable, rule)){
            sort = getKResultSort(variable);
        }else{
            sort = variable.sort();
        }
        pStrings.add(pString+counter+"."+sort);
    }

    @Override
    public void visit(Term node) {
        super.visit(node);
    }

    private String getKResultSort(Term term) {
        String sort = null;
        Set<String> sorts = new HashSet<>();
        sorts.add(((Variable) term).sort());

        java.util.Collection<String> commonSubsorts = context.getCommonSubsorts(sorts);
        if (commonSubsorts.size() == 1) {
            for (String s : commonSubsorts) {
                sort = s;
            }
        }
        return sort;
    }

    private boolean isRequiredToBeKResult(Term term, Rule rule) {
        boolean required = false;
        for (Term require : rule.requires()){
            if (require instanceof KItem){
                if (((KItem) require).kLabel().toString().equals("isKResult") &&
                        ((KItem) require).kList().size() == 1 &&
                        ((KItem) require).kList().get(0).equals(term)){
                    required = true;
                }
            }
        }
        return required;
    }


    public List<String> getpStrings() {
        return pStrings;
    }
}
