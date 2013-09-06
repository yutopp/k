package org.kframework.backend.provers.ast;

public class Freezer extends KItem {
    public final KItem body;

    public Freezer(KItem body) {
        this.body = body;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
         return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Freezer [body=" + body + "]";
    }    
}
