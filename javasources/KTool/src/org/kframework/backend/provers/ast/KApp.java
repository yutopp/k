package org.kframework.backend.provers.ast;

import com.google.common.collect.ImmutableList;

public class KApp extends KItem {
    public final String klabel;
    public final ImmutableList<KItem> args;

    public KApp(String klabel, ImmutableList<KItem> args) {
        this.klabel = klabel;
        this.args = args;
    }

    @Override
    public <R> R accept(KItemVisitor<R> visitor) {
         return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "KApp [klabel=" + klabel + ", args=" + args + "]";
    }
}