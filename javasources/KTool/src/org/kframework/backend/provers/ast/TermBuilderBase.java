package org.kframework.backend.provers.ast;

import java.math.BigInteger;
import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Static helper methods for constructing prover terms.
 * To work around the many name collisions with org.kframework.kil AST nodes
 * when constructing org.kframework.prover AST nodes, this class provides
 * static methods.
 * @author BrandonM
 */
public class TermBuilderBase {
    public static Cell cell(String label, CellContents contents) {
        return new Cell(label, contents);
    }
    
    public static CellContentVariable cellContentVariable(String name) {
        return new CellContentVariable(name);
    }
    
    public static CellContentCell cellContentCell(Cell cell) {
        return new CellContentCell(cell);
    }
    
    public static CellContentsCells cellContentsCells(ImmutableList<? extends CellContent> cells) {
        return new CellContentsCells(cells);
    }
    
    public static Cell cell(String label, ImmutableList<? extends CellContent> cells) {
        return cell(label, cellContentsCells(cells));
    }
    
    public static Cell cell(String label, Collection<? extends CellContent> cells) {
        return cell(label, cellContentsCells(ImmutableList.copyOf(cells)));
    }
    
    public static Cell cell(String label, CellContent... contents) {
        return cell(label, cellContentsCells(ImmutableList.copyOf(contents)));
    }
    
    public static KSequenceVariable ksequenceVariable(String name) {
        return new KSequenceVariable(name);
    }
    
    public static KSequence ksequence(ImmutableList<? extends K> contents) {
        return new KSequence(contents);
    }
    
    public static KSequence ksequence(Collection<? extends K> contents) {
        return new KSequence(contents);
    }
    
    public static KSequence ksequence(K... contents) {
        return ksequence(ImmutableList.copyOf(contents));
    }
    
    public static Freezer freezer(KItem body) {
        return new Freezer(body);
    }
    
    public static FreezerHole freezerHole() {
        return new FreezerHole();
    }
    
    public static KApp kapp(String klabel, ImmutableList<KItem> args) {
        return new KApp(klabel, args);
    }
    
    public static KApp kapp(String klabel, Collection<KItem> args) {
        return new KApp(klabel, ImmutableList.copyOf(args));
    }
    
    public static KApp kapp(String klabel, KItem... args) {
        return new KApp(klabel, ImmutableList.copyOf(args));
    }
    
    public static KVariable kvariable(String name, String sort) {
        return new KVariable(name, sort);
    }
    
    public static BoolBuiltin boolBuiltin(boolean value) {
        return new BoolBuiltin(value);
    }
    
    public static IntBuiltin intBuiltin(BigInteger value) {
        return new IntBuiltin(value);
    }
    
    public static TokenBuiltin tokenBuiltin(String sort, String value) {
    	return new TokenBuiltin(sort, value);
    }

    public static Map map(ImmutableMap<KItem, KItem> items, String rest) {
        return new Map(items, rest);
    }    
}