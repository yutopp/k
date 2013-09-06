package org.kframework.backend.provers.ast;

public abstract class BasicCellVisitor implements
    CellVisitor<Void>,
    CellContentsVisitor<Void>  
{
    public Void visit(CellContentVariable cellVariable) {
        return null;
    }
    public Void visit(K ksequenceItem) {
        return null;
    }
    
    @Override
    public Void visit(CellContentsCells cells) {
        for (CellContent item : cells.cells) {
            if (item instanceof CellContentCell) {
                CellContentCell cell = (CellContentCell) item;
                cell.cell.accept(this);
            } else {
                visit((CellContentVariable)item);
            }
        }
        return null;
    }

    @Override
    public Void visit(Cell cell) {
        cell.contents.accept(this);
        return null;
    }
    @Override
    public Void visit(KSequence seq) {
        for (K item : seq.contents) {
            visit(item);
        }
        return null;
    }
}
