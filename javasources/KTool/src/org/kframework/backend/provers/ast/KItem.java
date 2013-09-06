package org.kframework.backend.provers.ast;

public abstract class KItem extends K {
    public abstract <R> R accept(KItemVisitor<R> visitor);

    @SuppressWarnings("unchecked")
    @Override
    public final <RK,R extends RK> R accept(KVisitor<RK,R> visitor) {
        return accept((KItemVisitor<R>)visitor);
    }
}
