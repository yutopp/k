package org.kframework.backend.provers.ast;

public class FreezerHole extends KItem {
    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
         return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return "HOLE";
    }
}
