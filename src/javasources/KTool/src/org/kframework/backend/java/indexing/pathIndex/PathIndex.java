package org.kframework.backend.java.indexing.pathIndex;

import com.google.common.collect.Sets;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.kframework.backend.java.indexing.pathIndex.trie.PathIndexTrie;
import org.kframework.backend.java.indexing.pathIndex.visitors.*;
import org.kframework.backend.java.indexing.util.MultipleCellUtil;
import org.kframework.backend.java.indexing.util.MultiplicityStarCellHolder;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.kil.Cell;
import org.kframework.backend.java.kil.Definition;
import org.kframework.backend.java.kil.Rule;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.Token;
import org.kframework.backend.java.util.LookupCell;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Owolabi Legunsen
 * 1/8/14: 10:08 AM
 */
//TODO(OwolabiL): How to deal with macros and function rules? (imp has none)
public class PathIndex {
    private Map<Integer, Rule> indexedRules;
    private Definition definition;
    private PathIndexTrie trie;
    private MultiMap<Integer, String> pStringMap;
    MultiplicityStarCellHolder multiCellInfoHolder = null;
    private final int baseIOCellSize = 2;
    //TODO(OwolabiL): use list instead of set?
    Set<Integer> outputRuleIndices = new HashSet<>();
    Set<Integer> inputRuleIndices = new HashSet<>();

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
        multiCellInfoHolder = MultipleCellUtil.checkDefinitionForMultiplicityStar(definition.context());
        constructIndex(definition);
    }

    private void constructIndex(Definition definition) {
        pStringMap = new MultiHashMap<>();
        int count = 1;

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
        //for rules with multiple K cells
        ArrayList<Cell> kCells = new ArrayList<>();
        switch (type) {
            case COOLING:
                ruleVisitor = new CoolingRuleVisitor(rule, definition.context());
                break;
            case HEATING:
                ruleVisitor = new HeatingRuleVisitor(rule, definition.context());
                break;
            case OTHER:
                ruleVisitor = new RuleVisitor(definition.context());
                //TODO(OwolabiL): Move this to the RuleVisitor class
                if (multiCellInfoHolder != null) {
                    kCells = MultipleCellUtil.checkRuleForMultiplicityStar(rule, multiCellInfoHolder.getParentOfCellWithMultipleK());
                }
                break;
            case OUT:
                pStrings.put(n, "@.out");
                outputRuleIndices.add(n);
                return pStrings;
            case IN:
                pStrings.put(n, "@.in");
                inputRuleIndices.add(n);
                return pStrings;
            default:
                throw new IllegalArgumentException("Cannot create P-String for unknown rule type:" + type);
        }

        //TODO(OwolabiL): Move this check to the RuleVisitor class
        if (kCells.size() > 1) {
            for (Cell kCell : kCells) {
                kCell.accept(ruleVisitor);
            }
        } else {
            rule.accept(ruleVisitor);
        }

        pStrings.putAll(n, ruleVisitor.getpStrings());
        return pStrings;
    }

    public Set<Rule> getRulesForTerm(Term term) {
        ArrayList<String> pStrings;

        //check if the definition has a cell with multiplicity* which contains a k cell
        //if such a cell is found, the multiCellInfoHolder field should have been set at index creation time and
        // getting pStrings from it is done differently
        if (multiCellInfoHolder != null) {
            pStrings = MultipleCellUtil.getPStringsFromMultiple(term,
                    multiCellInfoHolder.getParentOfCellWithMultipleK(),
                    definition.context());
        } else {
            pStrings = getTermPString2(term);
        }

        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
//        Set<Integer> outputRuleIndices = null;
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
                if (trie.retrieve(trie.getRoot(), subString) != null) {
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

        //        check the out cell
        if (!outputRuleIndices.isEmpty()){
            Cell out = LookupCell.find(term, "out");
            List<Term> outCellList = ((BuiltinList) out.getContent()).elements();


        if (outCellList.size() > baseIOCellSize) {
            matchingIndices = Sets.union(matchingIndices, outputRuleIndices);
        }

        //TODO(OwolabiL): Write a visitor for this
        if (out.getContent() instanceof BuiltinList) {
            for (int i = 0; i < outCellList.size(); i++) {
                Term outCellElement = outCellList.get(i);
                if (outCellElement instanceof KItem) {
                    if (((KItem) outCellElement).kLabel().toString().equals("#buffer")) {
                        Term bufferTerm = ((KItem) outCellElement).kList().get(0);
                        if (bufferTerm instanceof Token) {
                            String bufferContent = ((Token) bufferTerm).value();
                            if (!bufferContent.equals("\"\"")) {
                                matchingIndices = Sets.union(matchingIndices, outputRuleIndices);
                            }
                        }
                    }
                }
            }
        }

        }

        // check the in cell
        if (!inputRuleIndices.isEmpty()){
            Cell in = LookupCell.find(term, "in");
            List<Term> inCellList = ((BuiltinList) in.getContent()).elements();

            if (inCellList.size() > baseIOCellSize) {
                matchingIndices = Sets.union(matchingIndices, inputRuleIndices);
            }
        }

        for (Integer n : matchingIndices) {
            rules.add(indexedRules.get(n));
        }

//        System.out.println("matching: "+matchingIndices);
//        System.out.println("rules: "+rules +"\n");
        return rules;
    }


    private ArrayList<String> getTermPString2(Term term) {
        TermVisitor termVisitor = new TermVisitor(definition.context());
        term.accept(termVisitor);
        return (ArrayList<String>) termVisitor.getpStrings();
    }
}