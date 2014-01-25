package org.kframework.backend.java.indexing.pathIndex;

import com.google.common.collect.Sets;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.indexing.pathIndex.visitors.*;
import org.kframework.backend.java.kil.*;
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
        printIndices(indexedRules, pStringMap);

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
        System.out.println("PStrings: "+pStrings);
        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
        Set<Integer> nextRetrieved = null;
        Set<Integer> currentMatch = null;
        Set<Integer> matchingIndices = new HashSet<>();
        if (pStrings.size() >= 1) {
//            for (int i = 0; i < pStrings.size(); i++) {
//                if (trie.retrieve(trie.getRoot(), pStrings.get(i)) != null) {
//                    matchingIndices.addAll(trie.retrieve(trie.getRoot(), pStrings.get(i)));
//                }
//            }
            currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(0));
            System.out.println("initial match: "+currentMatch);
            String possible = getHigherPString(pStrings.get(0),0);
            System.out.println("Possible: "+possible);

            if (possible != null && currentMatch != null) {
                currentMatch = Sets.union(currentMatch, trie.retrieve(trie.getRoot(), possible));
                System.out.println("XXX "+currentMatch);
//                if (currentMatch != null) {
//                } else{
//                    currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(1));
//                }
            }

            if (currentMatch != null && currentMatch.size() > 1 && pStrings.size() > 1){
                System.out.println("YYY "+trie.retrieve(trie.getRoot(), pStrings.get(1)));
                if (trie.retrieve(trie.getRoot(), pStrings.get(1)) != null) {
                    Set<Integer> possibleIntersection = Sets.intersection(currentMatch,trie.retrieve(trie.getRoot(), pStrings.get(1)));
                    if (possibleIntersection.size() > 0){
                        currentMatch = Sets.intersection(currentMatch,trie.retrieve(trie.getRoot(), pStrings.get(1)));
                    }
                } else {
                    System.out.println("hahaha");
                    String nextPossible = getHigherPString(pStrings.get(1),1);
                    System.out.println("nextPossible: "+nextPossible);
                    if (nextPossible != null) {
                        currentMatch = Sets.intersection(currentMatch, trie.retrieve(trie.getRoot(),nextPossible));
                    }
                }
            }


            if (currentMatch == null){
                currentMatch = trie.retrieve(trie.getRoot(), pStrings.get(1));
            }


            System.out.println("zzz: "+currentMatch);
//            for (String pString : pStrings.subList(1, pStrings.size())) {
//                nextRetrieved = trie.retrieve(trie.getRoot(),getHigherPString pString);
//                if (nextRetrieved != null && currentMatch != null) {
//                    System.out.println("currentmatch: "+currentMatch);
//                    System.out.println("next: "+nextRetrieved);
//                    currentMatch = Sets.intersection(currentMatch, nextRetrieved);
//                }                                             nextPossible
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
////                    currentMatch = Sets.union(currentMatch,getClosestIndices(list));
//////                    currentMatch = Sets.intersection(currentMatch,getClosestIndices2(pStrings));
////                }
//            }
            if (currentMatch != null) {
                matchingIndices.addAll(currentMatch);
            }

//        } else if (pStrings.size() == 1) {
//            if (trie.retrieve(trie.getRoot(), pStrings.get(0)) != null) {
//                matchingIndices.addAll(trie.retrieve(trie.getRoot(), pStrings.get(0)));
//            } else{
//                matchingIndices.addAll(getClosestIndices(pStrings));
//            }
        }
        //TODO(OwolabiL): Bad hack to be removed. Manipulate sorts instead
        //this is needed if we had multiple pStrings that do not match any rules
        //e.g. for imp, [@.'_+_.1.Id] should match [@.'_+_.1.KItem] or [@.'_+_.1.AExp] but it
        // currently doesn't
//        if (matchingIndices.size() == 0 && pStrings.size() != 0) {
//            System.out.println("here!");
//            Set<Integer> closestIndices = getClosestIndices(pStrings);
//            matchingIndices.addAll(closestIndices);
//        }

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

    private String getHigherPString(String pString, int n){
        String newString = null;
        String newPString = null;
        ArrayList<String> strings = new ArrayList<>();
        strings.add(pString);
        Set<String> sorts = getSortsFromPStrings(strings);
        for (String sort : sorts){
            System.out.println("UUU "+ sort);
            if (sort.equals("HOLE")){
                return null;
            }
            if (definition.context().isSubsorted("KResult",sort)){
                String replacement = pString.substring(0,pString.lastIndexOf("."))+".";

                String [] split =pString.split("\\.");
                ArrayList<String> splitList = new ArrayList<>(Arrays.asList(split));
                System.out.println("$$$$ "+pString);
                System.out.println("$$$$ "+splitList.size());
                int pos = splitList.size() - 3;
                System.out.println("$$$ "+splitList.get(pos));
//                System.out.println("&&&"+replacement);
                String currentLabel = splitList.get(pos);
//                String currentLabel = replacement.substring(replacement.indexOf(".")+1,replacement.lastIndexOf("."));
//                currentLabel = currentLabel.substring(0,currentLabel.lastIndexOf("."));
//                System.out.println("currentLabel: "+currentLabel);
                ArrayList<Production> productions = (ArrayList<Production>) definition.context().productionsOf(currentLabel);
                Production p = productions.get(0);
                newString = p.getChildSort(n);
//                System.out.println("### "+newString);
                replacement = replacement + newString;
//                System.out.println("replaced: "+replacement);
                newPString = replacement;
            }else {
                String replacement = pString.substring(0,pString.lastIndexOf("."))+".";

                String [] split =pString.split("\\.");
                ArrayList<String> splitList = new ArrayList<>(Arrays.asList(split));
                if (splitList.size() > 2){
                    System.out.println("$$$$ "+pString);
                    System.out.println("$$$$ "+splitList.size());
                    int pos = splitList.size() - 3;
                    System.out.println("$$$ "+splitList.get(pos));
//                System.out.println("&&&"+replacement);
                    String currentLabel = splitList.get(pos);
//                String currentLabel = replacement.substring(replacement.indexOf(".")+1,replacement.lastIndexOf("."));
//                currentLabel = currentLabel.substring(0,currentLabel.lastIndexOf("."));
//                System.out.println("currentLabel: "+currentLabel);
                    if (!definition.context().productionsOf(currentLabel).isEmpty()){
                        ArrayList<Production> productions = (ArrayList<Production>) definition.context().productionsOf(currentLabel);
                        Production p = productions.get(0);
                        newString = p.getChildSort(n);
                        replacement = replacement + newString;
                        newPString = replacement;
                    }
                }
//                System.out.println("### "+newString);
//                System.out.println("replaced: "+replacement);
            }
        }

        return newPString;
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

    private Set<String> getSortsFromPStrings(ArrayList<String> pStrings) {
        Set<String> sorts = new HashSet<>();
        for (String pString : pStrings){
            String sub = pString.substring(pString.lastIndexOf(".")+1);
            if (sub.equals("HOLE")){
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
