package org.kframework.backend.java.indexing.pathIndex;

import com.google.common.collect.Sets;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.indexing.pathIndex.trie.PathIndexTrie;
import org.kframework.backend.java.indexing.pathIndex.visitors.*;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.util.LookupCell;
import org.kframework.kil.Production;
import org.kframework.krun.K;

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
    private org.kframework.backend.java.indexing.pathIndex.trie.PathIndexTrie trie;
    private MultiMap<Integer, String> pStringMap;
    private boolean applyOutPutRules = false;
    private int baseOutSize = 2;

    public enum RuleType {
        COOLING,
        HEATING,
        OUT,
        IN,
        OTHER
    }

    public PathIndex(Definition definition) {
        this.definition = definition;
        this.indexedRules = new HashMap<>();
        constructIndex(definition);
    }

    private void constructIndex(Definition definition) {
        pStringMap = new MultiHashMap<>();
        int count = 1;
        //Step 1: initialize the Trie

        //Step 2: extract a p-string PS(i) from the LHS of each rule(i)
        //Step 3: assign a numeric index to identify the rule IND(i)
        for (Rule rule : definition.rules()) {
            if (rule.containsAttribute("heat") || rule.containsAttribute("print")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.HEATING));
            } else if (rule.containsAttribute("cool")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.COOLING));
            } else if (rule.containsAttribute("stdout") || rule.containsAttribute("stderr")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.OUT));
            } else if (rule.containsAttribute("stdin")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.IN));
            } else {
                pStringMap.putAll(createRulePString(rule, count, RuleType.OTHER));
            }
            indexedRules.put(count, rule);
            count++;
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
    }

    private void printIndices(Map<Integer, Rule> indexedRules, MultiMap<Integer,
            String> pString) {
        for (Integer n : indexedRules.keySet()) {
            System.out.println("Rule " + n + ": ");
            System.out.println(indexedRules.get(n));
            System.out.println("Rule Attribute:");
            System.out.println(indexedRules.get(n).getAttributes());
            System.out.println("P-Strings: ");
            ArrayList<String> p_strings = (ArrayList<String>) pString.get(n);
            //There should be no null p-string!!
            if (p_strings != null) {
                for (int i = 0; i < p_strings.size(); i++) {
                    System.out.println((i + 1) + ": " + p_strings.get(i));
                }
            }
            System.out.println();
        }
    }


    private MultiMap<Integer, String> createRulePString(Rule rule, int n, RuleType type) {
        MultiMap<Integer, String> pStrings = new MultiHashMap<>();
        RuleVisitor ruleVisitor;
        switch (type) {
            case COOLING:
                ruleVisitor = new CoolingRuleVisitor(rule, definition.context());
                break;
            case HEATING:
                ruleVisitor = new HeatingRuleVisitor(rule, definition.context());
                break;
            case OTHER:
                ruleVisitor = new RuleVisitor(definition.context());
                break;
            case OUT:
                pStrings.put(n, "@.out");
                return pStrings;
            case IN:
                pStrings.put(n, "@.in");
                return pStrings;
            default:
                throw new IllegalArgumentException("Cannot create P-String for unknown rule type:" + type);
        }

        rule.accept(ruleVisitor);
        pStrings.putAll(n, ruleVisitor.getpStrings());
        return pStrings;
    }

    public Set<Rule> getRulesForTerm(Term term) {
        ArrayList<String> pStrings = getTermPString2(term);

//        System.out.println("Term: " + term);
//        System.out.println("PStrings: " + pStrings);
//
//        check the out cell
        Cell out = LookupCell.find(term,"out");
        int outCellListSize = ((BuiltinList) out.getContent()).elements().size();
        if (outCellListSize > baseOutSize){
            pStrings.add(pStrings.size(),"@.out");
        }
        if (out.getContent() instanceof BuiltinList){
            for (int i = 0; i < ((BuiltinList) out.getContent()).elements().size(); i++) {
                if (((BuiltinList) out.getContent()).elements().get(i) instanceof KItem){
                    if (((KItem)((BuiltinList) out.getContent()).elements().get(i)).kLabel().toString().equals("#buffer")){
                        if (((KItem)((BuiltinList) out.getContent()).elements().get(i)).kList().get(0) instanceof Token){
                            String bufferContent = ((Token) ((KItem)((BuiltinList) out.getContent()).elements().get(i)).kList().get(0)).value();
                            if (!bufferContent.equals("\"\"")){
                                pStrings.add(pStrings.size(),"@.out");
                            }
                        }
                    }
                }
            }
        }

        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
        Set<Integer> nextRetrieved = null;
        Set<Integer> currentMatch = null;
        Set<Integer> matchingIndices = new HashSet<>();
        String subString = null;
        for (String pString : pStrings) {
            String[] split = pString.split("\\.");
            int i = split.length;
            currentMatch = trie.retrieve(trie.getRoot(), pString);
            subString = pString;
            while (i > 0 && subString.lastIndexOf('.') > 1) {
                subString = pString.substring(0, subString.lastIndexOf('.') - 2);
                if (trie.retrieve(trie.getRoot(),subString) != null){
                    currentMatch.addAll(trie.retrieve(trie.getRoot(), subString));
                }
            }

            if (matchingIndices.isEmpty()) {
                matchingIndices = currentMatch;
            } else {
                //should be an intersection?
                matchingIndices = Sets.union(matchingIndices, currentMatch);
            }
        }

//        if (currentMatch != null) {
//            matchingIndices.addAll(currentMatch);
//        }

        for (Integer n : matchingIndices) {
            rules.add(indexedRules.get(n));
        }

//        System.out.println("matching: "+matchingIndices);
//        System.out.println("rules: "+rules);
        return rules;
    }

//    public Set<Rule> getRulesForTerm(Term term) {
//        ArrayList<String> pStrings = getTermPString2(term);
//
//        System.out.println("Term: "+term);
//        System.out.println("PStrings: "+pStrings);
//
//        Set<Rule> rules = new HashSet<>();
//        //find the intersection of all the sets returned
//        Set<Integer> nextRetrieved = null;
//        Set<Integer> currentMatch = null;
//        Set<Integer> matchingIndices = new HashSet<>();
//        if (pStrings.size() >= 1) {
//            currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(0));
//            System.out.println("current match A: "+currentMatch);
//            String possible = getHigherPString(pStrings.get(0), 0);
//
//            if (possible != null && currentMatch != null) {
//                if (trie.retrieve(trie.getRoot(), possible) != null) {
//                    currentMatch = Sets.union(currentMatch, trie.retrieve(trie.getRoot(), possible));
//                    System.out.println("current match B: "+currentMatch);
//                }
//            }
//
//            if (currentMatch != null && currentMatch.size() > 1 && pStrings.size() > 1) {
//                if (trie.retrieve(trie.getRoot(), pStrings.get(1)) != null) {
//                    Set<Integer> possibleIntersection = Sets.intersection(currentMatch, trie.retrieve(trie.getRoot(), pStrings.get(1)));
//                    if (possibleIntersection.size() > 0) {
//                        System.out.println("PossibleIntersection: "+possibleIntersection);
//                        currentMatch = Sets.intersection(currentMatch, trie.retrieve(trie.getRoot(), pStrings.get(1)));
//                        System.out.println("current match c: "+currentMatch);
//                    }
//                } else {
//                    String nextPossible = getHigherPString(pStrings.get(1), 1);
//                    if (nextPossible != null) {
//                        currentMatch = Sets.intersection(currentMatch, trie.retrieve(trie.getRoot(), nextPossible));
//                        System.out.println("current match d: "+currentMatch);
//                    }
//                }
//            }
//
//
//            if (currentMatch == null) {
//                if (pStrings.size() > 1) {
//                    currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(1));
//                    System.out.println("current match e: "+currentMatch);
//                } else {
//                    //TODO(OwolabiL): use a substring of pString.get(0)
//                }
//            }
//
//            if (currentMatch != null) {
//                matchingIndices.addAll(currentMatch);
//            }
//
//        }
//
//        for (Integer n : matchingIndices) {
//            rules.add(indexedRules.get(n));
//        }
//        System.out.println("matching: "+matchingIndices);
//        System.out.println("Rules: "+rules);
//        return rules;
//    }

    private Set<Integer> getClosestIndices(ArrayList<String> pStrings) {
        Set<Integer> candidates = new HashSet<>();
        String firstPString = pStrings.get(0);
        String sub = firstPString.substring(0, firstPString.lastIndexOf("."));
        for (Map.Entry<Integer, Collection<String>> entry : pStringMap.entrySet()) {
            for (String str : entry.getValue()) {
                if (str.startsWith(sub)) {
                    candidates.add(entry.getKey());
                }
            }
        }
        return candidates;
    }

    private String getHigherPString(String pString, int n) {
        String newString = null;
        String newPString = null;
        ArrayList<String> strings = new ArrayList<>();
        strings.add(pString);
        Set<String> sorts = getSortsFromPStrings(strings);
        for (String sort : sorts) {
            if (sort.equals("HOLE")) {
                return null;
            }
            if (definition.context().isSubsorted("KResult", sort)) {
                String replacement = pString.substring(0, pString.lastIndexOf(".")) + ".";

                String[] split = pString.split("\\.");
                ArrayList<String> splitList = new ArrayList<>(Arrays.asList(split));
                int pos = splitList.size() - 3;
                String currentLabel = splitList.get(pos);
                ArrayList<Production> productions = (ArrayList<Production>) definition.context().productionsOf(currentLabel);
                Production p = productions.get(0);
                newString = p.getChildSort(n);
                replacement = replacement + newString;
                newPString = replacement;
            } else {
                String replacement = pString.substring(0, pString.lastIndexOf(".")) + ".";

                String[] split = pString.split("\\.");
                ArrayList<String> splitList = new ArrayList<>(Arrays.asList(split));
                if (splitList.size() > 2) {
                    int pos = splitList.size() - 3;
                    String currentLabel = splitList.get(pos);
                    if (!definition.context().productionsOf(currentLabel).isEmpty()) {
                        ArrayList<Production> productions = (ArrayList<Production>) definition.context().productionsOf(currentLabel);
                        Production p = productions.get(0);
                        newString = p.getChildSort(n);
                        replacement = replacement + newString;
                        newPString = replacement;
                    }
                }
            }
        }

        return newPString;
    }

    private Set<String> getSortsFromPStrings(ArrayList<String> pStrings) {
        Set<String> sorts = new HashSet<>();
        for (String pString : pStrings) {
            String sub = pString.substring(pString.lastIndexOf(".") + 1);
            if (sub.equals("HOLE")) {
                sub = "HOLE";
            }
            sorts.add(sub);
        }

        return sorts;
    }

    private ArrayList<String> getTermPString(Term term) {
        TermVisitor termVisitor = new TermVisitor(definition.context());
        term.accept(termVisitor);
        return (ArrayList<String>) termVisitor.getpStrings();
    }

    private ArrayList<String> getTermPString2(Term term) {
        TermVisitorGeneral termVisitor = new TermVisitorGeneral(definition.context());
        term.accept(termVisitor);
        return (ArrayList<String>) termVisitor.getpStrings();
    }
}
