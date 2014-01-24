package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.symbolic.LocalVisitor;
import org.kframework.backend.java.util.LookupCell;
import org.kframework.kil.Production;
import org.kframework.kil.loader.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: OwolabiL
 * Date: 1/21/14
 * Time: 12:05 PM
 */
public class TermVisitorGeneral extends LocalVisitor {
    private List<String> pStrings;

    private final Context context;
    private String pString;
    private int currentPosition = 0;
    private boolean inner = false;
    private String currentLabel;
    private final String SEPARATOR = ".";
    private final String START_STRING = "@.";

    public TermVisitorGeneral(Context context) {
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
                pString = START_STRING+"KResult";
            } else {
                //TODO(OwolabiL): Use a better check than the nullity of pString
                pStrings.add(START_STRING + token.sort());
            }
        }

        if (inner) {
//
            if (context.isSubsorted("KResult", token.sort())) {
                if (pString != null) {
                    pStrings.add(pString + SEPARATOR + currentPosition + SEPARATOR + token.sort());

                }
            } else {
                ArrayList<Production> productions = (ArrayList<Production>) context.productionsOf(currentLabel);
                Production p = productions.get(0);
                pStrings.add(pString + SEPARATOR+ currentPosition + SEPARATOR + p.getChildSort(0));

//                pStrings.add(pString + SEPARATOR+ currentPosition + SEPARATOR + "KItem");
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
                if (kItem.kList().size() == 0 && currentLabel.equals("List{\",\"}")){
//                    System.out.println("current label: "+currentLabel);
                    pStrings.add(pString + SEPARATOR + currentPosition + SEPARATOR + "'.List{\",\"}");
                } else {
                    pStrings.add(pString + SEPARATOR + currentPosition + SEPARATOR + kItem.sort());
                }
            }
        }
    }

    @Override
    public void visit(KList kList) {
        if (kList.size() == 0) {
//            pStrings.add(pString + ".1." + "#ListOf#Bot{\",\"}");
//            System.out.println("currentLabel: "+currentLabel);
//            pStrings.add(pString + ".1." + currentLabel);
            pStrings.add(pString);
        } else {
            for (int i = 0; i < kList.size(); i++) {
                currentPosition = i + 1;
                kList.get(i).accept(this);
            }
        }
    }

    @Override
    public void visit(KLabel kLabel) {
        pString = START_STRING + kLabel.toString();
    }

    public List<String> getpStrings() {
        return pStrings;
    }

    private class TokenVisitor extends TermVisitorGeneral {
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
            candidates.add(pString + SEPARATOR + currentPosition + ".HOLE");
        }

        @Override
        public void visit(Token token) {
            candidates.add(pString + SEPARATOR + currentPosition + SEPARATOR + token.sort());
        }

        public void visit(KItem kItem) {
            candidates.add(pString + SEPARATOR + currentPosition + SEPARATOR + kItem.sort());
        }

        private List<String> getCandidates() {
            return candidates;
        }
    }
}
