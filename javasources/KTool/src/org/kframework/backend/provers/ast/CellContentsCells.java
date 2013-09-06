package org.kframework.backend.provers.ast;

import com.google.common.collect.ImmutableList;

public final class CellContentsCells extends CellContents {
    public final ImmutableList<? extends CellContent> cells;

    public CellContentsCells(ImmutableList<? extends CellContent> cells) {
        super();
        this.cells = cells;
    }

    @Override
    public <R> R accept(CellContentsVisitor<R> visitor) {
        return visitor.visit(this);
    }
    
    public String toString() {
        return "CellContentsCells["+cells.toString()+"]";
    }
}
