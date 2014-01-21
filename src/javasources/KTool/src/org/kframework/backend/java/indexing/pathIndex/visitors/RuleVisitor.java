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
public class RuleVisitor extends LocalVisitor {
    public static final String SEPARATOR = ".";
    public static final String START_STRING = "@.";
    protected String pString;
    protected List<String> pStrings;
    boolean isKSequence = false;

    private int position = 0;

    public RuleVisitor() {
        this.pString = START_STRING;
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
        pString = pString.concat(kLabel.toString());
    }

    @Override
    public void visit(KList kList) {
        String base = pString;
        if (kList.size() == 0) {
            pStrings.add(pString + SEPARATOR + (position) + SEPARATOR + "#ListOf#Bot{\",\"}");
        }
        for (int i = 0; i < kList.size(); i++) {
            position = i + 1;
            if (!isKSequence) {
                String pending = pString + SEPARATOR + (position);
                //TODO(OwolabiL): instanceof must be removed!
                if (kList.get(i) instanceof KItem) {
                    pStrings.add(pending + SEPARATOR + ((KItem) kList.get(i)).sort());
                } else {
                    pStrings.add(pending + SEPARATOR + ((Variable) kList.get(i)).sort());
                }
            } else {
                pString = base + SEPARATOR + position + SEPARATOR;
                kList.get(i).accept(this);
            }
        }
    }

    @Override
    public void visit(Variable variable) {
        pStrings.add(pString + variable.sort());
    }

    @Override
    public void visit(BoolToken boolToken) {
        pStrings.add(pString + boolToken.value());
    }

    public List<String> getpStrings() {
        return pStrings;
    }
}
