package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.builtins.BoolToken;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: owolabi
 * Date: 1/20/14
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class RuleVisitor extends LocalVisitor{
    private Rule rule;
    private String pString;
    private List<String> pStrings;
    boolean isKSequence = false;

    private int position = 0;
    private int level = 1;

    public RuleVisitor(Rule rule) {
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
        cell.getContent().accept(this);
    }

    @Override
    public void visit(KSequence kSequence) {
        isKSequence = true;
        kSequence.get(0).accept(this);
    }

    @Override
    public void visit(KItem kItem) {
            visit(kItem.kLabel());
                visit(kItem.kList());

    }

    @Override
    public void visit(KLabel kLabel) {
        pString =  pString.concat(kLabel.toString());
    }

    @Override
    public void visit(KList kList) {
        System.out.println("kList###");
        if (kList.size() == 0){
            pStrings.add(pString+"."+(position)+"."+"#ListOf#Bot{\",\"}");
        }
        for (int i = 0; i < kList.size(); i++) {
            position = i+1;
            String pending = pString+"."+(position);
            if (!isKSequence){
                //TODO(OwolabiL): instanceof must be removed!
                if(kList.get(i) instanceof KItem){
                    pStrings.add(pending+"."+((KItem)kList.get(i)).sort());
                } else{
                    pStrings.add(pending+"."+((Variable)kList.get(i)).sort());
                }
            } else{
                System.out.println("&&& P-String: "+pString);
                pString = pending+".";
                kList.get(i).accept(this);
            }
        }
    }

    @Override
    public void visit(Variable variable) {
        System.out.println("var###");
        pStrings.add(pString+variable.sort());
    }

    @Override
    public void visit(BoolToken boolToken) {
        pStrings.add(pString+boolToken.value());
    }

    public List<String> getpStrings() {
        return pStrings;
    }

}
