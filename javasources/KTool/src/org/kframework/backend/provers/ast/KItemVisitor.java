package org.kframework.backend.provers.ast;

public interface KItemVisitor<R> {
    R visit(Freezer node);
    R visit(FreezerHole node);
    R visit(KApp node);
    R visit(KVariable node);
    R visit(BoolBuiltin node);
    R visit(IntBuiltin node);
    R visit(TokenBuiltin node);
    R visit(Map map);
}
