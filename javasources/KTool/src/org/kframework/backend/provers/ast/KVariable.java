package org.kframework.backend.provers.ast;

public class KVariable extends KItem {
    public final String name;
    public final String sort;

    public KVariable(String name, String sort) {
        this.name = name;
        this.sort = sort;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
         return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return name+":"+sort;
    }
}
