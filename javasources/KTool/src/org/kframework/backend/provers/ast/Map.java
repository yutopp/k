package org.kframework.backend.provers.ast;

import com.google.common.collect.ImmutableMap;

public class Map extends KItem {
    public final ImmutableMap<KItem, KItem> items;
    public final String rest;

    public Map(ImmutableMap<KItem, KItem> items, String rest) {
        super();
        this.items = items;
        this.rest = rest;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
        return visitor.visit(this);
    }

}
