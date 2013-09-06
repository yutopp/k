package org.kframework.backend.provers.ast;

public interface CellContentsVisitor<R> {
    public R visit(KSequence seq);
    public R visit(CellContentsCells cells);
}
