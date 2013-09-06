package org.kframework.backend.provers.ast;

public abstract class CellContents {
    public abstract <R> R accept(CellContentsVisitor<R> visitor);
}