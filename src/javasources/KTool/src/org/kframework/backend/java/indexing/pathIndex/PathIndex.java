package org.kframework.backend.java.indexing.pathIndex;

import com.google.common.collect.Sets;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.builtins.UninterpretedToken;
import org.kframework.backend.java.kil.*;
//import org.kframework.backend.java.kil.Collection;
import org.kframework.backend.java.util.LookupCell;

import java.util.*;
import java.util.Collection;

/**
 * Author: Owolabi Legunsen
 * 1/8/14: 10:08 AM
 */
//TODO(OwolabiL): How to deal with macros and function rules? (imp has none)
public class PathIndex {
    private Map<Integer, Rule> indexedRules;
    private Definition definition;
    private PathIndexTrie trie;
    private MultiMap<Integer,String> pStringMap;

    public PathIndex(Definition definition) {
        this.definition = definition;
        this.indexedRules = new HashMap<>();
        constructIndex(definition);
    }

    //TODO(Owolabi): should this be called from constructor or from client?
    private void constructIndex(Definition definition) {
        pStringMap = new MultiHashMap<>();
        int count = 1;
        //Step 1: initialize the Trie

        //Step 2: extract a p-string PS(i) from the LHS of each rule(i)
        //Step 3: assign a numeric index to identify the rule IND(i)
        for (Rule rule : definition.rules()) {
            if (rule.containsAttribute("heat")) {
                pStringMap.putAll(createHeatingRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
                continue;
            }

            if (rule.containsAttribute("cool")) {
                pStringMap.putAll(createCoolingRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
            } else {
                pStringMap.putAll(createRulePString(rule, count));
                indexedRules.put(count, rule);
                count++;
            }

        }

        assert indexedRules.size() == definition.rules().size();
//        printIndices(indexedRules, pStringMap);

        //intitialize the trie
        trie = new PathIndexTrie();

        //add all the pStrings to the trie
        ArrayList<String> strings;
        for (Integer key : pStringMap.keySet()) {
            strings = (ArrayList<String>) pStringMap.get(key);
            for (String string : strings) {
                trie.addIndex(trie.getRoot(), string.substring(2), key);
            }
        }

//        System.out.println("full trie: "+trie);
//        System.out.println("full trie size: "+trie.size(trie.getRoot()));


        //TODO(OwolabL): move these to the test class
//        ArrayList<String> firstSet = (ArrayList<String>) pString.get(5);
//        trie.addIndex(trie.getRoot(),firstSet.get(0).substring(2),5);
//        trie.addIndex(trie.getRoot(), firstSet.get(1).substring(2), 5);
//        trie.addIndex(trie.getRoot(),firstSet.get(2).substring(2),5);
//        System.out.println("Trie: "+trie);
//
//        ArrayList<String> secondSet = (ArrayList<String>) pString.get(11);
//        trie.addIndex(trie.getRoot(),secondSet.get(0).substring(2),11);
//        trie.addIndex(trie.getRoot(),secondSet.get(1).substring(2),11);
//        System.out.println("Trie Again: "+trie);
//
//        ArrayList<String> thirdSet = (ArrayList<String>) pString.get(8);
//        trie.addIndex(trie.getRoot(),thirdSet.get(0).substring(2),8);
//        trie.addIndex(trie.getRoot(),thirdSet.get(1).substring(2),8);
//        System.out.println("Trie III: "+trie);
//
//        System.out.println("Trie III size: "+trie.size(trie.getRoot()));
//
//        trie.removeIndex(trie.getRoot(), thirdSet.get(0).substring(2),8);
//        trie.removeIndex(trie.getRoot(), thirdSet.get(1).substring(2),8);
//
//        System.out.println("Triee IV: "+trie);
//
//        Set<Integer> retrieved = trie.retrieve(trie.getRoot(),secondSet.get(1));
//        System.out.println("retrieved: "+retrieved);

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
//                Term first = kList.get(0);
//                Term second = kList.get(1);
                ArrayList<Term> terms = new ArrayList<>();
                for (int i = 0; i < kList.size(); i++) {
                    terms.add(kList.get(i));
                    if(kList.get(i) instanceof Variable){
                        String firstString = "@." + kLabel.toString() + "."+(i+1)+"." +
                                ((Variable) kList.get(i)).sort();
                        pStrings.put(count, firstString);
                    } else if (kList.get(i) instanceof KItem){
                        KItem innerFirst = (KItem) kList.get(i);
                        String firstString = "@." + kLabel.toString()
                                + "."+(i+1)+".";
                        if (innerFirst.kList().size() == 0) {
                            firstString += "#ListOf#Bot{\",\"}";
                        } //TODO(OwolabiL): else what? this is brittle!
                        pStrings.put(count, firstString);
                    }
                }
//                if (first instanceof Variable) {
//                    String firstString = "@." + kLabel.toString() + ".1." +
//                            ((Variable) first).sort();
//                    pStrings.put(count, firstString);
//                } else if (first instanceof KItem) {
//                    KItem innerFirst = (KItem) first;
//                    String firstString = "@." + kLabel.toString()
//                            + ".1.";
//                    if (innerFirst.kList().size() == 0) {
//                        firstString += "#ListOf#Bot{\",\"}";
//                    } //TODO(OwolabiL): else what? this is brittle!
//                    pStrings.put(count, firstString);
//                }
//
//                if (second instanceof Variable) {
//                    String secondString = "@." + kLabel.toString() + ".2." +
//                            ((Variable) second).sort();
//                    pStrings.put(count, secondString);
//                }

            }
        } else {
            //we don't have a kSequence. means that term fills entire K cell
            if (lhsK.getContent() instanceof KItem) {
                KItem kItem = (KItem) lhsK.getContent();
                KLabel outerKLabel = kItem.kLabel();
                KList kList = kItem.kList();
                //TODO(OwolabiL): Again, maybe use loop
                Term first = kList.get(0);
                Term second = kList.get(1);
                if (first instanceof KItem) {
                    KItem innerKItem = (KItem) first;
//                    KLabel innerKLabel = innerKItem.kLabel();
//                    KList innerkList = innerKItem.kList();
//                    Term innerFirst = innerkList.get(0);
//                    Term innerSecond = innerkList.get(1);
                    String outerFirstString = "@." + outerKLabel.toString()
                            + ".1." + innerKItem.sort();
                    pStrings.put(count, outerFirstString);
//                    if (innerFirst instanceof Variable) {
//                        String innerFirstString =
//                                "@." + outerKLabel.toString() + ".2." +
//                                        innerKLabel + ".1." +
//                                        ((Variable) innerFirst).sort();
//                        pStrings.put(count, innerFirstString);
//                    }
//
//                    if (innerSecond instanceof Variable) {
//                        String innerSecondString = "@." + outerKLabel.toString()
//                                + ".2." + innerKLabel + ".2." +
//                                ((Variable) innerSecond).sort();
//                        pStrings.put(count, innerSecondString);
//                    }

                    if (second instanceof Variable) {
                        String outerSecondString = "@." + outerKLabel.toString()
                                + ".2." + ((Variable) second).sort();
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

            Term frozenTerm;
            String frozenItemString;

            for (int i = 0; i < frozenItem.kList().size(); i++) {
                frozenTerm = frozenItem.kList().get(i);
                if (frozenTerm instanceof Hole) {
                    frozenItemString = "HOLE";
                } else {
                    //is it always a variable?
                    frozenItemString = ((Variable) frozenTerm).sort();
                }
                pStrings.put(n, "@." + firstSort + ".1." + frozenItemLabel + "."+(i+1)+"."
                        + frozenItemString);
            }
//            Term frozenItemListMember1 = frozenItem.kList().get(0);
//            Term frozenItemListMember2 = frozenItem.kList().get(1);
//            String frozenItem1String;
//            String frozenItem2String;
//            if (frozenItemListMember1 instanceof Hole) {
//                frozenItem1String = "HOLE";
//            } else {
//                //is it always a variable?
//                frozenItem1String = ((Variable) frozenItemListMember1).sort();
//            }
//
//            if (frozenItemListMember2 instanceof Hole) {
//                frozenItem2String = "HOLE";
//            } else {
//                frozenItem2String = ((Variable) frozenItemListMember2).sort();
//            }
//
//            pStrings.put(n, "@." + firstSort + ".1." + frozenItemLabel + ".1."
//                    + frozenItem1String);
//
//
//            pStrings.put(n, "@." + firstSort + ".1." + frozenItemLabel + ".2."
//                    + frozenItem2String);

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
            String firstSort = ((Variable) first).sort();
            pStrings.put(n, "@." + label.toString() + ".1." + firstSort);

            Variable second;
            String secondSort;
            if (((KItem) content).kList().size() > 1){
                second = (Variable) ((KItem) content).kList().get(1);
                secondSort = second.sort();
                pStrings.put(n, "@." + label.toString() + ".2." + secondSort);
            }
        }

        return pStrings;
    }

    public Set<Rule> getRulesForTerm(Term term) {
        ArrayList<String> pStrings = getTermPString(term);
        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
//        System.out.println("term: " + term);
        Set<Integer> matchingIndices = new HashSet<>();
        if (pStrings.size() > 1) {
            Set<Integer> retrieved = trie.retrieve(trie.getRoot(), pStrings.get(0));
            Set<Integer> nextRetrieved;
            Set<Integer> currentMatch = retrieved;
            for (String pString : pStrings.subList(1, pStrings.size())) {
                nextRetrieved = trie.retrieve(trie.getRoot(), pString);
                if (nextRetrieved != null && currentMatch != null) {
                    currentMatch = Sets.intersection(currentMatch, nextRetrieved);
                }

                if (nextRetrieved != null && currentMatch == null) {
                    currentMatch = nextRetrieved;
                }

                //TODO(OwolabiL):Another terrible hack that should be removed!!!
                //This is a result of not yet knowing how to manipulate the sort hierarchy in
                // the index
                if (nextRetrieved == null && currentMatch != null){
                    ArrayList<String> list = new ArrayList<>();
                    list.add(pString);
                    currentMatch = Sets.union(currentMatch,getClosestIndices(list));
                }
            }
            if (currentMatch != null) {
                matchingIndices.addAll(currentMatch);
            }

        } else if (pStrings.size() == 1) {
            matchingIndices.addAll(trie.retrieve(trie.getRoot(), pStrings.get(0)));
        }
        //TODO(OwolabiL): Bad hack to be removed. Manipulate sorts instead
        //this is needed if we had multiple pStrings that do not match any rules
        //e.g. for imp, [@.'_+_.1.Id] should match [@.'_+_.1.KItem] or [@.'_+_.1.AExp] but it
        // currently doesn't
        if (matchingIndices.size() == 0 && pStrings.size() != 0){
            Set<Integer> closestIndices = getClosestIndices(pStrings);
            matchingIndices.addAll(closestIndices);
        }

//        System.out.println("matching: " + matchingIndices);
        for (Integer n : matchingIndices) {
            rules.add(indexedRules.get(n));
        }
//        System.out.println("Rules: " + rules + "\n");

        return rules;
        //no matching rule was found
//        return null;
    }

    private Set<Integer> getClosestIndices(ArrayList<String> pStrings) {
        Set<Integer> candidates = new HashSet<>();
        String firstPString = pStrings.get(0);
        String sub = firstPString.substring(0,firstPString.lastIndexOf("."));
        for (Map.Entry<Integer,Collection<String>> entry: pStringMap.entrySet()){
            for (String str : entry.getValue()){
                if (str.startsWith(sub)){
                    candidates.add(entry.getKey());
                }
            }
        }
        return candidates;
    }

    private ArrayList<String> getTermPString(Term term) {
        //TODO(OwolabiL): another case for generality using visitors: should be able to use the same pString generator as for rules!
        Cell kCell = LookupCell.find(term, "k");
        ArrayList<String> candidates = new ArrayList<>();
//        StringBuilder builder = new StringBuilder("@.");
        Term kTerm = kCell.getContent();
        if (kTerm instanceof KSequence) {
            if (((KSequence) kTerm).size() > 0) {
                // Is this so in general? If head is not a token, treat as a normal KItem
                //Here, an instance of Token at the head means that a cooling rule should apply.
                Term sequenceHead = ((KSequence) kTerm).get(0);
                if (sequenceHead instanceof Token) {
                    String string1;
                    if (definition.context().isSubsorted("KResult", ((Token) sequenceHead).sort())) {
                        string1 = "@.KResult.";
                        Term sequenceSecond = ((KSequence) kTerm).get(1);
                        if (sequenceSecond instanceof KItem) {
                            KItem kItem = (KItem) sequenceSecond;
                            KLabel kLabel = kItem.kLabel();
                            if (kLabel instanceof KLabelFreezer) {
                                //TODO(OwolabiL): This is duplicated code!!!!!!!
                                KLabelFreezer freezer = (KLabelFreezer) kLabel;
                                KItem frozenItem = (KItem) freezer.term();
                                String frozenItemLabel = frozenItem.kLabel().toString();
                                Term frozenItemListMember1 = frozenItem.kList().get(0);
                                Term frozenItemListMember2 = frozenItem.kList().get(1);
                                String frozenItem1String;
                                String frozenItem2String;
                                if (frozenItemListMember1 instanceof Hole) {
                                    frozenItem1String = "HOLE";
                                } else {
                                    //is it always a variable? No! Here it can be an UninterpretedToken
                                    frozenItem1String = ((Token) frozenItemListMember1).sort();
                                }

                                if (frozenItemListMember2 instanceof Hole) {
                                    frozenItem2String = "HOLE";
                                } else {
                                    frozenItem2String = ((Token) frozenItemListMember2).sort();
                                }

                                String string2 = string1 + "1." + frozenItemLabel + ".1." + frozenItem1String;
                                String string3 = string1 + "1." + frozenItemLabel + ".2." + frozenItem2String;
                                // end of duplicated code
                                candidates.add(string2);
                                candidates.add(string3);

                            }
                        }
                    } else {
                        string1 = "@." + ((Token) sequenceHead).sort();
                        candidates.add(string1);
                    }
                } else if (sequenceHead instanceof KItem) {
                    //TODO(OwolabiL): More duplicated code. Remove!!!!
                    KItem kItem = (KItem) sequenceHead;
                    KLabel kLabel = kItem.kLabel();
//                    builder.append(kLabel.toString());
                    String string1 = "@." + kLabel.toString();
                    KList kList = kItem.kList();

                    Term first = kList.get(0);
                    Term second = kList.get(1);

                    //for imp there are two cases for the first element:
                    //(1) it is a kItem
                    if (first instanceof KItem) {
                        KItem innerKItem = (KItem) first;
                        String string2 = string1 + ".1." + innerKItem.sort();
                        candidates.add(string2);

                    } else if (first instanceof Token) {
                        //(2) it is an uninterpretedToken
                        String string2 = string1 + ".1." + ((Token) first).sort();
                        candidates.add(string2);
                    }

                    if (second instanceof KItem) {
                        String string3 = string1 + ".2." + ((KItem) second).sort();
                        candidates.add(string3);
                    } else if (second instanceof Token) {
                        String string3 = string1 + ".2." + ((Token) second).sort();
                        candidates.add(string3);
                    }
                }
            }

        } else {
            KItem kItem = (KItem) kTerm;
            KLabel kLabel = kItem.kLabel();
//            builder.append(kLabel.toString());
            String string1 = "@." + kLabel.toString();
            KList kList = kItem.kList();

            Term first = kList.get(0);
            Term second = kList.get(1);

            //for imp there are two cases for the first element:
            //(1) it is a kItem
            if (first instanceof KItem) {
                KItem innerKItem = (KItem) first;
                String string2 = string1 + ".1." + innerKItem.sort();
                candidates.add(string2);

            } else if (first instanceof Token) {
                //(2) it is an uninterpretedToken
                String string2 = string1 + ".1." + ((Token) first).sort();
                candidates.add(string2);
            }

            if (second instanceof KItem) {
                String string3 = string1 + ".2." + ((KItem) second).sort();
                candidates.add(string3);
            } else if (second instanceof Token) {
                String string3 = string1 + ".2." + ((Token) second).sort();
                candidates.add(string3);
            }
        }
//        System.out.println("candidates: "+candidates);
        return candidates;
    }
}
