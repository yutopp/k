package org.kframework.backend.provers.ast;

public final class Cell {
    public final String label;
    public final CellContents contents;

    public Cell(String label, CellContents contents) {
        this.label = label;
        this.contents = contents;
    }
    
    public <R> R accept(CellVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Cell [label=" + label + ", contents=" + contents + "]";
    }
}
