package org.kframework.backend.provers.ast;

import java.util.Map.Entry;

public abstract class BasicKVisitor implements KVisitor<Void,Void> {
    @Override
    public Void visit(KApp node) {
        for (KItem child : node.args) {
            child.accept(this);            
        }
        return null;
    }

    @Override
    public Void visit(KVariable node) {
        return null;
    }

    @Override
    public Void visit(BoolBuiltin node) {
        return null;
    }

    @Override
    public Void visit(IntBuiltin node) {
        return null;
    }

    @Override
    public Void visit(TokenBuiltin node) {
        return null;
    }

    @Override
    public Void visit(Map map) {
        for (Entry<KItem,KItem> entry : map.items.entrySet()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Freezer node) {
        return node.body.accept(this);
    }

    @Override
    public Void visit(FreezerHole node) {
        return null;
    }

    @Override
    public Void visit(KSequenceVariable node) {
        return null;
    }
}