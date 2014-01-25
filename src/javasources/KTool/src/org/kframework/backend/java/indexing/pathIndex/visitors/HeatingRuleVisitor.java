package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.kil.Production;
import org.kframework.kil.loader.Context;

import java.util.*;

/**
 * Author: OwolabiL
 * Date: 1/20/14
 * Time: 10:25 AM
 */
public class HeatingRuleVisitor extends RuleVisitor {
    private final Rule rule;
    private final Context context;
    private String currentLabel = null;

    private int counter = 0;

    public HeatingRuleVisitor(Rule rule, Context context) {
        super(context);
        this.rule = rule;
        this.context = context;
    }

    @Override
    public void visit(KSequence kSequence) {
        kSequence.get(0).accept(this);
    }

    @Override
    public void visit(KItem kItem) {
        visit(kItem.kLabel());
        visit(kItem.kList());
    }

    @Override
    public void visit(KLabel kLabel) {
        currentLabel = kLabel.toString();
        pString = pString.concat(kLabel.toString()+SEPARATOR);
    }

    @Override
    public void visit(KList kList) {
        for (int i = 0; i < kList.size(); i++) {
            counter = i+1;
//            visit((Variable)kList.get(i));
            kList.get(i).accept(this);
        }
        this.proceed = false;
    }

    @Override
    public void visit(Variable variable) {
        String sort;
        if(isRequiredToBeKResult(variable, rule)){
            sort = getKResultSort(variable);
        }else{
//            sort = variable.sort();
            ArrayList<Production> productions = (ArrayList<Production>) context.productionsOf(currentLabel);
            Production p = productions.get(0);
//            System.out.println("variable: "+variable);
//            System.out.println("child sort: "+p.getChildSort(counter-1));
            sort = p.getChildSort(counter-1);
        }
        pStrings.add(pString+counter+"."+sort);
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

    //TODO(OwolabiL): Use visitor for traversing the rule instead
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

}
