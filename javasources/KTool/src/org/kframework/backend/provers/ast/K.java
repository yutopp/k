package org.kframework.backend.provers.ast;

public abstract class K {
    public abstract <RK,R extends RK> RK accept(KVisitor<RK,R> visitor);
}