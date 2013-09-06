package org.kframework.backend.provers.ast;

public class CellContentCell extends CellContent {
    public final Cell cell;

    public CellContentCell(Cell cell) {
        super();
        this.cell = cell;
    }
    
    public String toString() {
        return "CellContentCell["+cell.toString()+"]";
    }
}
