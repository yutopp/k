package org.kframework.backend.provers.ast;

import java.util.HashSet;
import java.util.Set;

/**
 * Accumulate the set of variables seen in the presented terms.
 */
public class FindVariables {
    private final Set<String> variables = new HashSet<String>();
    
    private final BasicCellVisitor cellVisitor = new BasicCellVisitor() {        
        @Override
        public Void visit(K kseqItem) {
            return kseqItem.accept(kvisitor);
        }       
        @Override
        public Void visit(CellContentVariable cellVariable) {
            variables.add(cellVariable.name);
            return null;
        }
    };
    private final BasicKVisitor kvisitor = new BasicKVisitor() {
        @Override
        public Void visit(KVariable var) {
            variables.add(var.name);
            return null;
        }
        @Override
        public Void visit(KSequenceVariable seqVar) {
            variables.add(seqVar.name);
            return null;
        }
        @Override
        public Void visit(Map map) {
            if (map.rest != null) {
                variables.add(map.rest);
            }
            return super.visit(map);
        }
    };
    
    /** Get set of variables seen in the terms processed so far.
     * The returned set is invalidated if this object processes
     * any further terms, and modifying the returned set invalidates
     * this object.
     */
    public Set<String> getVariables() {
        return variables;
    }

    /**
     * Accumulate the variables in this Cell
     */
    public void process(Cell cell) {
        cell.accept(cellVisitor);
    }
    /**
     * Accumulate the variables in this KItem
     */
    public void process(KItem kitem) {
        kitem.accept(kvisitor);
    }
    
    public static Set<String> getVariables(Cell term) {
        FindVariables vars = new FindVariables();
        vars.process(term);
        return vars.getVariables();
    }    
    
    public static Set<String> getVariables(KItem term) {
        FindVariables vars = new FindVariables();
        vars.process(term);
        return vars.getVariables();
    }
}