package org.kframework.backend.provers.ast;

public class BoolBuiltin extends KItem {
    public final boolean value;

    public BoolBuiltin(boolean value) {
        super();
        this.value = value;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return ""+value;
    }
}
