package org.kframework.backend.java.indexing.pathIndex;

import com.google.common.collect.Sets;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.indexing.pathIndex.visitors.*;
import org.kframework.backend.java.kil.*;
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

    public enum RuleType {
        COOLING,
        HEATING,
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
            if (rule.containsAttribute("heat")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.HEATING));
            } else if (rule.containsAttribute("cool")) {
                pStringMap.putAll(createRulePString(rule, count, RuleType.COOLING));
            } else {
                pStringMap.putAll(createRulePString(rule, count, RuleType.OTHER));
            }
            indexedRules.put(count, rule);
            count++;
        }

        assert indexedRules.size() == definition.rules().size();
//        printIndices(indexedRules, pStringMap);

        //intitialize the trie
        trie = new org.kframework.backend.java.indexing.pathIndex.trie.PathIndexTrie();

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
            default:
                throw new IllegalArgumentException("Cannot create P-String for unknown rule type:" + type);
        }

        MultiMap<Integer, String> pStrings = new MultiHashMap<>();
        rule.accept(ruleVisitor);
        pStrings.putAll(n, ruleVisitor.getpStrings());
        return pStrings;
    }

    public Set<Rule> getRulesForTerm(Term term) {
        ArrayList<String> pStrings = getTermPString2(term);
//        System.out.println("K-testgen? "+ K.do_concrete_exec);
//        System.out.println("PStrings: "+pStrings);
        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
        Set<Integer> nextRetrieved = null;
        Set<Integer> currentMatch = null;
        Set<Integer> matchingIndices = new HashSet<>();
        if (pStrings.size() > 1) {
            currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(0));
            for (String pString : pStrings.subList(1, pStrings.size())) {
                nextRetrieved = trie.retrieve(trie.getRoot(), pString);
                if (nextRetrieved != null && currentMatch != null) {
                    currentMatch = Sets.intersection(currentMatch, nextRetrieved);
                }

//                if (nextRetrieved != null && currentMatch == null) {
//                    currentMatch = nextRetrieved;
//                }

                //TODO(OwolabiL):Another terrible hack that should be removed!!! Needed with general sorts
                //This is a result of not yet knowing how to manipulate the sort hierarchy in
                // the index
                if (nextRetrieved == null && currentMatch != null){
                    ArrayList<String> list = new ArrayList<>();
                    list.add(pString);
                    currentMatch = Sets.union(currentMatch,getClosestIndices(list));
//                    currentMatch = Sets.intersection(currentMatch,getClosestIndices2(pStrings));
                }
            }
            if (currentMatch != null) {
                matchingIndices.addAll(currentMatch);
            }

        } else if (pStrings.size() == 1) {
            if (trie.retrieve(trie.getRoot(), pStrings.get(0)) != null) {
                matchingIndices.addAll(trie.retrieve(trie.getRoot(), pStrings.get(0)));
            } else{
                matchingIndices.addAll(getClosestIndices(pStrings));
            }
        }
        //TODO(OwolabiL): Bad hack to be removed. Manipulate sorts instead
        //this is needed if we had multiple pStrings that do not match any rules
        //e.g. for imp, [@.'_+_.1.Id] should match [@.'_+_.1.KItem] or [@.'_+_.1.AExp] but it
        // currently doesn't
        if (matchingIndices.size() == 0 && pStrings.size() != 0) {
            Set<Integer> closestIndices = getClosestIndices(pStrings);
            matchingIndices.addAll(closestIndices);
        }

        for (Integer n : matchingIndices) {
            rules.add(indexedRules.get(n));
        }

        return rules;
    }

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

//    private Set<Integer> getClosestIndices2(ArrayList<String> pStrings) {
//        Set<String> sorts = getSortsFromPStrings(pStrings);
//
//        sorts.add("KItem");
//
//        String lubSort = definition.context().getLUBSort(sorts);
//
//        ArrayList<String> replacements = new ArrayList<>();
//
//        System.out.println("The Sorts: "+sorts);
//        System.out.println("The GLB: "+lubSort);
//
//        for (String pString: pStrings){
//            String replacement = pString.substring(0,pString.lastIndexOf("."))+"."+ lubSort;
//            replacements.add(replacement);
//        }
//
//        System.out.println("Replacements: "+replacements);
//
//        Set<Integer> candidates = new HashSet<>();
//
//        if (replacements.size() > 1) {
//            Set<Integer> nextRetrieved;
//            Set<Integer> currentMatch = trie.retrieve(trie.getRoot(), replacements.get(0));
//            for (String test : replacements.subList(1, replacements.size())) {
//                nextRetrieved = trie.retrieve(trie.getRoot(), test);
//                if (nextRetrieved != null && currentMatch != null) {
//                    currentMatch = Sets.intersection(currentMatch, nextRetrieved);
//                }
//
////                if (nextRetrieved != null && currentMatch == null) {
////                    currentMatch = nextRetrieved;
////                }
//
//                //TODO(OwolabiL):Another terrible hack that should be removed!!! Needed with general sorts
//                //This is a result of not yet knowing how to manipulate the sort hierarchy in
//                // the index
////                if (nextRetrieved == null && currentMatch != null){
////                    ArrayList<String> list = new ArrayList<>();
////                    list.add(pString);
////                    currentMatch = Sets.union(currentMatch,getClosestIndices2(list));
////                }
//            }
//            if (currentMatch != null) {
//                candidates.addAll(currentMatch);
//            }
//
//        } else if (pStrings.size() == 1) {
//            candidates.addAll(trie.retrieve(trie.getRoot(), replacements.get(0)));
//        }
//
////        String firstPString = pStrings.get(0);
////        String sub = firstPString.substring(0, firstPString.lastIndexOf("."));
////        for (Map.Entry<Integer, Collection<String>> entry : pStringMap.entrySet()) {
////            for (String str : entry.getValue()) {
////                if (str.startsWith(sub)) {
////                    candidates.add(entry.getKey());
////                }
////            }
////        }
//        System.out.println("Extra Candidates: "+candidates);
//        return candidates;
//    }

//    private Set<String> getSortsFromPStrings(ArrayList<String> pStrings) {
//        Set<String> sorts = new HashSet<>();
//        for (String pString : pStrings){
//            String sub = pString.substring(pString.lastIndexOf(".")+1);
//            if (sub.equals("HOLE")){
//                sub = "KItem";
//            }
//            sorts.add(sub);
//        }
//
//        return sorts;
//    }

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
