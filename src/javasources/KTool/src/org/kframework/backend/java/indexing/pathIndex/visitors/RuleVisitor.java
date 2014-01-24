package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.builtins.BoolToken;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;
import org.kframework.kil.loader.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: OwolabiL
 * Date: 1/20/14
 * Time: 1:50 PM
 */
public class RuleVisitor extends LocalVisitor {
    static final String SEPARATOR = ".";
    private static final String START_STRING = "@.";
    private final Context context;
    protected String pString;
    protected List<String> pStrings;
    boolean isKSequence = false;
    private String currentKLabel;

    private int position = 0;

    public RuleVisitor(Context context) {
        this.context = context;
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
        currentKLabel = kLabel.toString();
        pString = pString.concat(currentKLabel);
    }

    @Override
    public void visit(KList kList) {
        String base = pString;
        if (kList.size() == 0) {
            //TODO(OwolabiL): adding 1 to position for now seems to work. may need to change
//            pStrings.add(pString + SEPARATOR + (position+1) + SEPARATOR + "#ListOf#Bot{\",\"}");
            pStrings.add(pString);
        }
        for (int i = 0; i < kList.size(); i++) {
            position = i + 1;
            if (!isKSequence) {
                String pending = pString + SEPARATOR + (position);
                //TODO(OwolabiL): instanceof must be removed!
                if (kList.get(i) instanceof KItem) {
                    pStrings.add(pending + SEPARATOR + ((KItem) kList.get(i)).sort());
                } else {
                    if (context.isSubsorted("KResult",((Variable)kList.get(i)).sort())){
                        pStrings.add(pending + SEPARATOR + ((Variable) kList.get(i)).sort());
                    } else {
                        pStrings.add(pending + SEPARATOR + "KItem");
                    }
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