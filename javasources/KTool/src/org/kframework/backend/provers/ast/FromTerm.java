package org.kframework.backend.provers.ast;

import static org.kframework.backend.provers.ast.TermBuilderBase.*;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.Bag;
import org.kframework.kil.GenericToken;
import org.kframework.kil.IntBuiltin;
import org.kframework.kil.Map;
import org.kframework.kil.BoolBuiltin;
import org.kframework.kil.Cell;
import org.kframework.kil.Freezer;
import org.kframework.kil.FreezerHole;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.KSequence;
import org.kframework.kil.MapItem;
import org.kframework.kil.Term;
import org.kframework.kil.TermCons;
import org.kframework.kil.Token;
import org.kframework.kil.Variable;

/**
 * Methods for converting general KIL terms in the form allowed by the prover backends
 * into the prover AST.
 * 
 * Cells are required to contain either nested cells or K terms.
 * Cells may be a single Cell or a Bag of Cells,
 * K terms may be a KSequence of variables or terms,
 * or a single TermCons or a Variable of a sort satisfying
 * MetaK.isComputationSort()
 * 
 * terms are built from Freezer, FreezerHole, TermCons, and Variable
 * @author BrandonM
 */
public class FromTerm {
    public static org.kframework.backend.provers.ast.Cell convertCell(Cell cell) {
        Term body = cell.getContents();
        if (body instanceof Variable) {
            Variable v = (Variable)body;
            if (MetaK.isComputationSort(v.getSort())) {
                return cell(cell.getLabel(), ksequence(convertKItem(v)));
            }
        }
        if (body instanceof KSequence) {
            KSequence seq = (KSequence) body;
            return cell(cell.getLabel(), convertKSequence(seq));
        }
        if (body instanceof TermCons) {
            return cell(cell.getLabel(), ksequence(convertKItem(body)));
        }
        if (body instanceof Cell) {
            // wrap single cells into bags for uniformity
            body = new Bag(ImmutableList.of(body));
        }
        if (body instanceof Bag) {
            ImmutableList.Builder<CellContent> builder = ImmutableList.builder();
            for (Term t : ((Bag) body).getContents()) {
                if (t instanceof Cell) {
                    Cell c = (Cell) t;
                    builder.add(cellContentCell(convertCell(c)));
                } else if (t instanceof Variable && MetaK.isCellSort(((Variable)t).getSort())) {
                    builder.add(cellContentVariable(((Variable)t).getName()));
                } else {
                    throw new IllegalArgumentException(
                            "Cell "+cell.getLabel()+" contains bag including non-Cell item "+t);
                }
            }
            return cell(cell.getLabel(), builder.build());
        }
        if (body instanceof org.kframework.kil.Map) {
            return cell(cell.getLabel(), ksequence(convertKItem(body)));
        }
        throw new IllegalArgumentException(
                "Cell "+cell.getLabel()+" contains a "+body.getClass()+" "+body);
    }
    
    public static org.kframework.backend.provers.ast.KSequence convertKSequence(KSequence seq) {
        ImmutableList.Builder<org.kframework.backend.provers.ast.K> builder = ImmutableList.builder();
        List<Term> contents = seq.getContents();
        final int len = contents.size();
        if (len == 0) {
            throw new IllegalArgumentException("Empty KSequence");
        }
        for (int i = 0; i < len-1; ++i) {
            builder.add(convertKItem(contents.get(i)));
        }
        Term last = contents.get(len-1);
        if (last instanceof Variable) {
            Variable v = (Variable) last;
            builder.add(ksequenceVariable(v.getName()));
        } else {
            builder.add(convertKItem(last));
        }

        return ksequence(builder.build());
    }
    
    public static org.kframework.backend.provers.ast.KItem convertKItem(Term term) {
        if (term instanceof TermCons) {
            TermCons app = (TermCons) term;
            ImmutableList.Builder<KItem> builder = ImmutableList.builder();
            for (Term t : app.getContents()) {
                builder.add(convertKItem(t));
            }
            return kapp(app.getProduction().getKLabel(), builder.build());
        } else if (term instanceof KApp) {
            KApp app = (KApp)term;
            Term args = app.getChild();
            if (!(args instanceof KList)) {
                throw new IllegalArgumentException("Cannot convert KApp with non-KList arguments "+args);
            }
            List<Term> children = ((KList)args).getContents();

            Term labelTerm = app.getLabel();
            if (labelTerm instanceof KLabelConstant) {
                final String label = ((KLabelConstant) labelTerm).getLabel();
                ImmutableList.Builder<KItem> builder = ImmutableList.builder();
                for (Term t : ((KList)args).getContents()) {
                    builder.add(convertKItem(t));
                }                
                return kapp(label, builder.build());
            } else if (labelTerm instanceof Token && !children.isEmpty()) {
                throw new IllegalArgumentException("Cannot convert KApp of Token to non-empty argument list "+app);                
            } else if (labelTerm instanceof BoolBuiltin) {
                return boolBuiltin(((BoolBuiltin)labelTerm).booleanValue());
            } else if (labelTerm instanceof IntBuiltin) {
                return intBuiltin(((IntBuiltin)labelTerm).bigIntegerValue());
            } else if (labelTerm instanceof GenericToken)  {
            	GenericToken token = (GenericToken)labelTerm;
            	return tokenBuiltin(token.tokenSort(), token.value());
            } else {
                throw new IllegalArgumentException("Cannot convert KApp with non-constant label "+labelTerm);
            }
        } else if (term instanceof Variable) {
            Variable v = (Variable)term;
            return kvariable(v.getName(), v.getSort());
        } else if (term instanceof Freezer) {
            Freezer f = (Freezer) term;
            return freezer(convertKItem(f.getTerm()));
        } else if (term instanceof FreezerHole) {
            return freezerHole();
        } else if (term instanceof Map) {
            ImmutableMap.Builder<KItem,KItem> builder = ImmutableMap.builder();
            String rest = null;
            for (Term t : ((Map)term).getContents()) {
                if (t instanceof MapItem) {
                    MapItem item = (MapItem) t;
                    builder.put(convertKItem(item.getKey()), convertKItem(item.getValue()));
                } else if (t instanceof Variable) {
                    Variable v = (Variable) t;
                    if (v.getSort().equals("Map")) {
                        if (rest == null) {
                            rest = v.getName();
                        } else {
                            throw new IllegalArgumentException("Map containing multiple map variables "+term);
                        }                        
                    } else if (v.getSort().equals("MapItem")) {
                        throw new UnsupportedOperationException("Don't support MapItem variables yet "+term);                        
                    } else {
                        throw new IllegalArgumentException("Map contains variable of unexpected sort "+v.getSort()+": "+term);
                    }
                } else if (t instanceof Map && ((Map)t).getContents().isEmpty()) {
                    // had .Map as an entry
                    continue;
                } else {
                    throw new IllegalArgumentException("Map contains element of unexpected kil class "+t.getClass()+": "+term);
                }
            }
            ImmutableMap<KItem,KItem> items = builder.build();
            if (items.isEmpty() && rest != null) {
                return kvariable(rest, "Map");
            } else {
                return map(items, rest);
            }
        } else {
            throw new IllegalArgumentException("Can't make prover KItem from Term "+term);
        }
    }
}
