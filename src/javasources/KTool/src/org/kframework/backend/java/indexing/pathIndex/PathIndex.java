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
    private org.kframework.backend.java.indexing.pathIndex.trie.PathIndexTrie trie;
    private MultiMap<Integer, String> pStringMap;
    MultiplicityStarCellHolder holder = null;
    private boolean applyOutPutRules = false;
    private int baseIOCellSize = 2;

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
        holder = MultipleCellUtil.checkDefinitionForMultiplicityStar(definition.context());
        constructIndex(definition);
    }

//    public MultiplicityStarCellHolder checkDefinitionForMultiplicityStar(Context context) {
//        MultiplicityStarCellHolder starCellHolder = null;
//        for (Map.Entry<String, ConfigurationStructure> entry : definition.context().getConfigurationStructureMap().entrySet()){
//
//            //for now I am assuming that there is only one cell in the definition which (1) has
//            // multiplicity* and (2) has children which can contain kCells
//            if (entry.getValue().multiplicity.equals(org.kframework.kil.Cell.Multiplicity.ANY)){
//                Term backendKILCell = null;
//                try {
//                    backendKILCell = (Cell)entry.getValue().cell.accept(new KILtoBackendJavaKILTransformer(definition.context()));
//                    Term kCell = LookupCell.find(backendKILCell, "k");
//                    if (LookupCell.find(kCell, "k") != null){
//                        System.out.println("Cell "+entry.getKey()+" has multiplicity* and contains a K cell!");
//
//                        starCellHolder = new MultiplicityStarCellHolder();
//                        starCellHolder.setCellWithMultipleK(entry.getKey());
//                        starCellHolder.setParentOfCellWithMultipleK(entry.getValue().parent.cell.getId());
//                    }
//                } catch (TransformerException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return starCellHolder;
//    }



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
                ruleVisitor = new RuleVisitor(definition.context(),holder.getParentOfCellWithMultipleK());
                kCells = MultipleCellUtil.checkRuleForMultiplicityStar(rule, holder.getParentOfCellWithMultipleK());
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


        //TODO(OwolabiL): Move this check to the RuleVisitor class
        if (kCells.size() > 1){
            for (Cell kCell: kCells){
                kCell.accept(ruleVisitor);
            }
        }else{
            rule.accept(ruleVisitor);
        }

        pStrings.putAll(n, ruleVisitor.getpStrings());
        return pStrings;
    }

    public Set<Rule> getRulesForTerm(Term term) {
        ArrayList<String> pStrings;

        if (holder != null) {
            pStrings = MultipleCellUtil.getPStringsFromMultiple(term,
                    holder.getParentOfCellWithMultipleK(),
                    definition.context());
        } else {
            pStrings = getTermPString2(term);
        }

        Set<Rule> rules = new HashSet<>();
        //find the intersection of all the sets returned
        Set<Integer> outSet = null;
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

        //        check the out cell
        Cell out = LookupCell.find(term,"out");
        List<Term> outCellList = ((BuiltinList) out.getContent()).elements();

        //maybe cache this value at indexing time instead of always computing it here
        outSet = trie.retrieve(trie.getRoot(), "@.out");
        if (outCellList.size() > baseIOCellSize){
            matchingIndices = Sets.union(matchingIndices, outSet);
        }

        if (out.getContent() instanceof BuiltinList){
            for (int i = 0; i < outCellList.size(); i++) {
                Term outCellElement = outCellList.get(i);
                if (outCellElement instanceof KItem){
                    if (((KItem) outCellElement).kLabel().toString().equals("#buffer")){
                        Term bufferTerm = ((KItem) outCellElement).kList().get(0);
                        if (bufferTerm instanceof Token){
                            String bufferContent = ((Token) bufferTerm).value();
                            if (!bufferContent.equals("\"\"")){
                                matchingIndices = Sets.union(matchingIndices, outSet);
                            }
                        }
                    }
                }
            }
        }

        // check the in cell
        Cell in = LookupCell.find(term,"in");
        List<Term> inCellList = ((BuiltinList) in.getContent()).elements();

        //maybe cache the value of inSet at indexing time instead of always computing it here
        Set<Integer> inSet = trie.retrieve(trie.getRoot(), "@.in");
        if (inCellList.size() > baseIOCellSize){
            matchingIndices = Sets.union(matchingIndices, inSet);
        }

        for (Integer n : matchingIndices) {
            rules.add(indexedRules.get(n));
        }

//        System.out.println("matching: "+matchingIndices);
//        System.out.println("rules: "+rules +"\n");
        return rules;
    }



    private ArrayList<String> getTermPString2(Term term) {
        TermVisitorGeneral termVisitor = new TermVisitorGeneral(definition.context());
        term.accept(termVisitor);
        return (ArrayList<String>) termVisitor.getpStrings();
    }
}
