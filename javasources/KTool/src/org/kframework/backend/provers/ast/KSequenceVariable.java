package org.kframework.backend.provers.ast;

public final class KSequenceVariable extends K {
    public final String name;

    public KSequenceVariable(String name) {
        this.name = name;
    }

    @Override
    public <RK,R extends RK> RK accept(KVisitor<RK,R> visitor) {
        return visitor.visit(this);
    }
}
