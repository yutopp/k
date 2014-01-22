package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;
import org.kframework.kil.Production;
import org.kframework.kil.loader.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author: OwolabiL
 * Date: 1/21/14
 * Time: 12:05 PM
 */
public class TermVisitor extends LocalVisitor {
    private List<String> pStrings;

    private final Context context;
    private String pString;
    private int currentPosition = 0;
    private boolean inner = false;
    private String currentLabel;

    public TermVisitor(Context context) {
        pStrings = new ArrayList<>();
        this.context = context;
    }

    @Override
    public void visit(Term node) {
        (LookupCell.find(node, "k")).accept(this);
    }


    @Override
    public void visit(Cell cell) {
        cell.getContent().accept(this);
    }

    @Override
    public void visit(KSequence kSequence) {
        if (kSequence.size() > 0) {
            kSequence.get(0).accept(this);
            if (kSequence.get(0) instanceof Token) {
                kSequence.get(1).accept(this);
            }
        }
    }

    @Override
    public void visit(Token token) {
        if (pString == null) {
            if (context.isSubsorted("KResult", token.sort())) {
                pString = "@.KResult";
            } else {
                //TODO(OwolabiL): Use a better check than the nullity of pString
                pStrings.add("@." + token.sort());
            }
        }

        if (inner) {
//
            if (context.isSubsorted("KResult", token.sort())) {
                if (pString != null) {
                    pStrings.add(pString + "." + currentPosition + "." + token.sort());

                }
            } else {
                ArrayList<Production> productions = (ArrayList<Production>) context.productionsOf(currentLabel);
                Production p = productions.get(0);
                pStrings.add(pString + "." + currentPosition + "." + p.getChildSort(0));
            }
        }
    }


    @Override
    public void visit(KItem kItem) {
        if (kItem.kLabel() instanceof KLabelFreezer) {
            if (pString != null) {
                TokenVisitor visitor = new TokenVisitor(context, pString);
                kItem.kLabel().accept(visitor);
                pStrings.addAll(visitor.getCandidates());
            }
        } else {
            if (!inner) {
                inner = true;
                currentLabel = kItem.kLabel().toString();
                kItem.kLabel().accept(this);
                kItem.kList().accept(this);
            } else {
                pStrings.add(pString + "." + currentPosition + "." + kItem.sort());
            }
        }
    }

    @Override
    public void visit(KList kList) {
        if (kList.size() == 0) {
            pStrings.add(pString + ".1." + "#ListOf#Bot{\",\"}");
        } else {

            for (int i = 0; i < kList.size(); i++) {
                currentPosition = i + 1;
                kList.get(i).accept(this);
            }
        }

    }

    @Override
    public void visit(KLabel kLabel) {
        pString = "@." + kLabel.toString();
    }

    public List<String> getpStrings() {
        return pStrings;
    }

    private class TokenVisitor extends TermVisitor {
        private String baseString;
        private String pString;

        private List<String> candidates;

        public TokenVisitor(Context context, String string) {
            super(context);
            this.baseString = string;
            candidates = new ArrayList<>();
        }

        @Override
        public void visit(KLabelFreezer kLabelFreezer) {
            KItem frozenItem = (KItem) kLabelFreezer.term();
            frozenItem.kLabel().accept(this);
            frozenItem.kList().accept(this);
        }

        @Override
        public void visit(KLabel kLabel) {
            pString = baseString + ".1." + kLabel;
        }

        @Override
        public void visit(KList kList) {
            for (int i = 0; i < kList.size(); i++) {
                currentPosition = i + 1;
                kList.get(i).accept(this);
            }
        }

        @Override
        public void visit(Hole hole) {
            candidates.add(pString + "." + currentPosition + ".HOLE");
        }

        @Override
        public void visit(Token token) {
            candidates.add(pString + "." + currentPosition + "." + token.sort());
        }

        public void visit(KItem kItem) {
            candidates.add(pString + "." + currentPosition + "." + kItem.sort());

        }

        private List<String> getCandidates() {
            return candidates;
        }
    }
}
