package org.kframework.backend.provers.ast;

public interface KVisitor<K,R extends K> extends KItemVisitor<R> {
    K visit(KSequenceVariable node);
}
