package org.kframework.backend.provers.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public abstract class CellTransformer implements
    CellVisitor<Cell>,
    CellContentsVisitor<CellContents>
{
    public CellContent visit(CellContentVariable cellVar) {
        return cellVar;
    }
    public K visit(K kseqItem) {
        return kseqItem;
    }
    
    @Override
    public CellContents visit(KSequence seq) {
        Builder<K> builder = ImmutableList.builder();
        for (K item : seq.contents) {
            builder.add(visit(item));            
        }
        return new KSequence(builder.build());
    }

    @Override
    public CellContents visit(CellContentsCells cells) {
        Builder<CellContent> builder = ImmutableList.builder();
        for (CellContent item : cells.cells) {
            if (item instanceof CellContentCell) {
                CellContentCell cell = (CellContentCell) item;
                builder.add(new CellContentCell(cell.cell.accept(this)));
            } else {
                builder.add(visit((CellContentVariable)item));
            }
        }
        return new CellContentsCells(builder.build());
    }

    @Override
    public Cell visit(Cell cell) {
        return new Cell(cell.label, cell.contents.accept(this));
    }

}
