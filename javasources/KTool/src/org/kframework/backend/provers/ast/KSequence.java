package org.kframework.backend.provers.ast;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

public final class KSequence extends CellContents {
    public final ImmutableList<? extends K> contents;

    /** Make a KSequence with the given contents,
     * sharing the provided list.
     */
    public KSequence(ImmutableList<? extends K> contents) {
        this.contents = contents;
    }

    /** Make a KSequence with the given contents,
     * copying the provided collection.
     */
    public KSequence(Collection<? extends K> contents) {
        this(ImmutableList.copyOf(contents));
    }

    @Override
    public <R> R accept(CellContentsVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String toString() {
        UnmodifiableIterator<? extends K> iterator = contents.iterator();
        String result = "KSequence[";
        if (iterator.hasNext()) {
            result += iterator.next().toString();
        }
        while (iterator.hasNext()) {
            result += " ~> "+iterator.next().toString();
        }
        return result+"]";
    }
}
