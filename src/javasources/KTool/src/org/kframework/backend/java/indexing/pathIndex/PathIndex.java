package org.kframework.backend.java.indexing.pathIndex;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.util.LookupCell;

import java.util.*;

/**
 * Author: Owolabi Legunsen
 * 1/8/14: 10:08 AM
 */
//TODO(OwolabiL): How to deal with macros and function rules? (imp has none)
public class PathIndex {
    private Map<Integer, Rule> indexedRules;

    public PathIndex(Definition definition) {
        this.indexedRules = new HashMap<>();
        constructIndex(definition);
    }

    //TODO(Owolabi): should this be called from constructor or from client?
    private void constructIndex(Definition definition) {
        MultiMap<Integer, String> pString = new MultiHashMap<>();
        int count = 1;
        //Step 1: initialize the Trie

        //Step 2: extract a p-string PS(i) from the LHS of each rule(i)
        //Step 3: assign a numeric index to identify the rule IND(i)
        for (Rule rule : definition.rules()) {
            if (rule.containsAttribute("heat")) {
                pString.putAll(createHeatingRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
                continue;
            }

            if (rule.containsAttribute("cool")) {
                pString.putAll(createCoolingRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
            } else {
                pString.putAll(createRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
            }

        }

        assert indexedRules.size() == definition.rules().size();
        printIndices(indexedRules, pString);

        //add p-string to PathIndexTrie with IND(i) in the set on the leaf
        //create IndexedRule(rule(i),PS(i))
        //add <IND(i),IndexedRule(rule(i),PS(i))> to IndexToRuleMap
    }

    private void printIndices(Map<Integer, Rule> indexedRules, MultiMap<Integer,
            String> pString) {
        for (Integer n : indexedRules.keySet()) {
            System.out.println("Rule " + n + ": ");
            System.out.println(indexedRules.get(n));
            System.out.println("P-Strings: ");
            ArrayList<String> p_strings = (ArrayList<String>) pString.get(n);
            for (int i = 0; i < p_strings.size(); i++) {
                System.out.println((i + 1) + ": " + p_strings.get(i));
            }
            System.out.println();
        }
    }

    private MultiMap<Integer, String> createRulePString(Rule rule, int count) {
        //these rules can have multiple forms. will need to see if a trend
        // emerges and whether there is a more general way. taking them case
        // by case for now
        MultiMap<Integer, String> pStrings = new MultiHashMap<>();
        Cell lhsK = LookupCell.find(rule.leftHandSide(), "k");
        if (lhsK.getContent() instanceof KSequence) {
            KSequence kSequence = (KSequence) lhsK.getContent();
            Term content0 = kSequence.get(0);
            if (content0 instanceof Variable) {
                String varString = "@." + ((Variable) content0).sort();
                pStrings.put(count, varString);
            }

            if (content0 instanceof KItem) {
                KLabel kLabel = ((KItem) content0).kLabel();
                KList kList = ((KItem) content0).kList();
                //TODO(OwolabiL): do a loop with the kList size instead?
                Term first = kList.get(0);
                Term second = kList.get(1);
                if (first instanceof Variable) {
                    String firstString = "@." + kLabel.toString() + ".1." +
                            ((Variable) first).sort();
                    pStrings.put(count, firstString);
                } else if (first instanceof KItem) {
                    KItem innerFirst = (KItem) first;
                    String firstString = "@." + innerFirst.kLabel().toString()
                            + ".1.";
                    if (innerFirst.kList().size() == 0) {
                        firstString += "_KList";
                    }
                    pStrings.put(count, firstString);
                }

                if (second instanceof Variable) {
                    String secondString = "@." + kLabel.toString() + ".2." +
                            ((Variable) second).sort();
                    pStrings.put(count, secondString);
                }

            }
        } else {
            //we don't have a kSequence means that term fills entire K cell
            if (lhsK.getContent() instanceof KItem) {
                KItem kItem = (KItem) lhsK.getContent();
                KLabel outerKLabel = kItem.kLabel();
                KList kList = kItem.kList();
                //TODO(OwolabiL): Again, maybe use loop
                Term first = kList.get(0);
                Term second = kList.get(1);
                if (first instanceof KItem) {
                    KItem innerKItem = (KItem) first;
                    KLabel innerKLabel = innerKItem.kLabel();
                    KList innerkList = innerKItem.kList();
                    Term innerFirst = innerkList.get(0);
                    Term innerSecond = innerkList.get(1);
                    if (innerFirst instanceof Variable) {
                        String innerFirstString =
                                "@." + outerKLabel.toString() + ".2." +
                                        innerKLabel + ".1." +
                                        ((Variable) innerFirst).sort();
                        pStrings.put(count, innerFirstString);
                    }

                    if (innerSecond instanceof Variable) {
                        String innerSecondString = "@." + outerKLabel.toString()
                                + ".2." + innerKLabel + ".2." +
                                ((Variable) innerSecond).sort();
                        pStrings.put(count, innerSecondString);
                    }

                    if (second instanceof Variable) {
                        String outerSecondString = "@." + outerKLabel.toString()
                                + ".1." + ((Variable) second).sort();
                        pStrings.put(count, outerSecondString);

                    }
                }
            }
        }
        return pStrings;
    }

    private MultiMap<Integer, String> createCoolingRulePString(Rule rule, int n) {
        Cell lhsK = LookupCell.find(rule.leftHandSide(), "k");
        MultiMap<Integer, String> pStrings = new MultiHashMap<>();
        if (lhsK.getContent() instanceof KSequence) {
            KSequence kSequence = (KSequence) lhsK.getContent();
            Term content0 = kSequence.get(0);
            Term content1 = kSequence.get(1);

            Variable variable0 = (Variable) content0;
            String requiredKresult = "isKResult(" + variable0 + ")";
            String firstSort;
            //TODO(OwolabiL): Remove this check and use concrete sort instead
            if (rule.requires().toString().contains(requiredKresult)) {
                firstSort = "KResult";
            } else {
                //TODO(OwolabiL): this should never happen!! throw exception?
                firstSort = variable0.sort();
            }

            KLabelFreezer freezer = (KLabelFreezer) ((KItem) content1).kLabel();
            KItem frozenItem = (KItem) freezer.term();
            String frozenItemLabel = frozenItem.kLabel().toString();
            Term frozenItemListMember1 = frozenItem.kList().get(0);
            Term frozenItemListMember2 = frozenItem.kList().get(1);
            String frozenItem1String;
            String frozenItem2String;
            if (frozenItemListMember1 instanceof Hole) {
                frozenItem1String = "HOLE";
            } else {
                //is it always a variable?
                frozenItem1String = ((Variable) frozenItemListMember1).sort();
            }

            if (frozenItemListMember2 instanceof Hole) {
                frozenItem2String = "HOLE";
            } else {
                frozenItem2String = ((Variable) frozenItemListMember2).sort();
            }

            pStrings.put(n, "@." + firstSort + ".1." + frozenItemLabel + ".1."
                    + frozenItem1String);

            pStrings.put(n, "@." + firstSort + ".1." + frozenItemLabel + ".2."
                    + frozenItem2String);

        }

        return pStrings;
    }

    private MultiMap<Integer, String> createHeatingRulePString(Rule rule, int n) {
        Cell lhsK = LookupCell.find(rule.leftHandSide(), "k");
        MultiMap<Integer, String> pStrings = new MultiHashMap<>();
        if (lhsK.getContent() instanceof KSequence) {
            //TODO(OwolabiL): There has to be a more general way of doing this!
            //TODO(OwolabiL): Should I be keeping requires information?
            Term content = ((KSequence) lhsK.getContent()).get(0);
            KLabel label = ((KItem) content).kLabel();
            Term first = ((KItem) content).kList().get(0);
            Variable second = (Variable) ((KItem) content).kList().get(1);

            String firstSort = ((Variable) first).sort();
            String secondSort = second.sort();

            pStrings.put(n, "@." + label.toString() + ".1." + firstSort);
            pStrings.put(n, "@." + label.toString() + ".2." + secondSort);
        }

        return pStrings;
    }
}
