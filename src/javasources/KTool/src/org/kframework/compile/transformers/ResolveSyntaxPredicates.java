// Copyright (c) 2012-2014 K Team. All Rights Reserved.
package org.kframework.compile.transformers;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.*;
import org.kframework.kil.visitors.CopyOnWriteTransformer;

import java.util.Set;

public class ResolveSyntaxPredicates extends CopyOnWriteTransformer {
    
    
    
    public ResolveSyntaxPredicates(org.kframework.kil.loader.Context context) {
        super("Resolve syntax predicates", context);
    }
    
    
    @Override
    public ASTNode visit(Configuration node, Void _)  {
        return node;
    }
    
    @Override
    public ASTNode visit(Syntax node, Void _)  {
        return node;
    }
    
    /*
     * (non-Javadoc)
     * @see org.kframework.kil.AbstractVisitor#visit(org.kframework.kil.Sentence, java.lang.Object)
     * fixed here,
     * if a sort has getExpectedSort then we check if it is a KSort, otherwise, we check if var's getSort is a KSort
     * Not sure if it is a good solution for doing the ResolveSyntaxPredicate after flattenTerms
     * The problem is the following: If we do the flattenTerms before this steps, then
     * All variable will become sort KItem, then it does not make sense to check MetaK.isKSort(var.getSort())
     * if the sort has a more concrete sort.
     */
    @Override
    public ASTNode visit(Sentence node, Void _)  {
        boolean change = false;
        Set<Variable> vars = node.getBody().variables();
        KList ands = new KList();
        Term condition = node.getRequires();
        if (null != condition) {
            ands.getContents().add(condition);
        }
        for (Variable var : vars) {
//            if (!var.isUserTyped()) continue;
            if (var.isSyntactic()) continue;
            if ((var.getExpectedSort() == null && MetaK.isKSort(var.getSort())) || (var.getExpectedSort()!=null && MetaK.isKSort(var.getExpectedSort()))) continue;
            change = true;
            ands.getContents().add(getPredicateTerm(var));
        }
        if (!change) return node;
        if (ands.getContents().size() > 1) {
            condition = new KApp(KLabelConstant.ANDBOOL_KLABEL, ands);
        } else {
            condition = ands.getContents().get(0);
        }
        node = node.shallowCopy();
        node.setRequires(condition);
        return node;
    }

    private Term getPredicateTerm(Variable var) {
        return KApp.of(KLabelConstant.of(AddPredicates.predicate(var.getExpectedSort()), context), var);
    }

}
