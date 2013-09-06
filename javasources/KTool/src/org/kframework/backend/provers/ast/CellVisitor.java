package org.kframework.backend.provers.ast;

public interface CellVisitor<C> {
    public C visit(Cell cell);
}
