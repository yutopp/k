package org.kframework.backend.provers.ast;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class Transformer implements KVisitor<K,KItem>{
    @Override
    public KItem visit(Freezer node) {
        return new Freezer(node.body.accept(this));
    }

    @Override
    public KItem visit(FreezerHole node) {
        return node;
    }

    @Override
    public KItem visit(KApp node) {
        ImmutableList.Builder<KItem> args = ImmutableList.builder();
        for (KItem child : node.args) {
            args.add(child.accept(this));            
        }
        return new KApp(node.klabel, args.build());
    }

    @Override
    public KItem visit(KVariable node) {
        return node;
    }

    @Override
    public KItem visit(BoolBuiltin node) {
        return node;
    }

    @Override
    public KItem visit(IntBuiltin node) {
        return node;
    }

    @Override
    public KItem visit(TokenBuiltin node) {
        return node;
    }

    @Override
    public KItem visit(Map map) {
        HashMap<KItem,KItem> items = new HashMap<KItem,KItem>();
        for (Entry<KItem,KItem> entry : map.items.entrySet()) {            
            KItem newKey = entry.getKey().accept(this);
            if (items.containsKey(newKey)) {
                throw new IllegalStateException(
                        "Transformer "+this+" made two keys identical while transforming map pattern "+map);
            }
            items.put(newKey, entry.getValue().accept(this));
        }
        return new Map(ImmutableMap.copyOf(items), map.rest);
    }

    @Override
    public K visit(KSequenceVariable node) {
        return node;
    }
}
