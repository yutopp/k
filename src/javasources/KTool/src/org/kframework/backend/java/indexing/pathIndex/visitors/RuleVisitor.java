package org.kframework.backend.java.indexing.pathIndex.visitors;

import org.kframework.backend.java.kil.*;

/**
 * Created with IntelliJ IDEA.
 * User: owolabi
 * Date: 1/20/14
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RuleVisitor {
    public void visit(Rule rule);
    public void visit(Cell cell);
    public void visit(KSequence kSequence);
    public void visit(KItem kItem);
    public void visit(KList kList);
    public void visit(KLabel kLabel);
}
