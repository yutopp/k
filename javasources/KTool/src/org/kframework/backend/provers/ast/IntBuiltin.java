package org.kframework.backend.provers.ast;

import java.math.BigInteger;

public class IntBuiltin extends KItem {
    public final BigInteger value;    
    public IntBuiltin(BigInteger value) {
        super();
        this.value = value;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
