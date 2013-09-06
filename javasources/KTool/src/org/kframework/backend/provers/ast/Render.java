package org.kframework.backend.provers.ast;

import java.io.StringWriter;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.kframework.backend.acl2.Acl2Context;
import org.kframework.kil.Attribute;
import org.kframework.kil.Production;

public class Render {
	private static abstract class LispWorker implements KVisitor<Void,Void>
	{
		final StringWriter out = new StringWriter();
				
		void symbol(String name) {		    
			if (Character.isAlphabetic(name.charAt(0))
				&& StringUtils.isAlphanumeric(name)) {
				out.append(name);
				return;
			} else {
				out.append('|');
				out.append(name.toUpperCase());
				out.append('|');
				return;
			}
		}
		// override to print constructor
		void constructor(String cons) {
		    symbol(cons);
		}
        // override to print variable
		void variable(String name) {
		    // avoid ACL2 constant "t", which can't be a variable
		    if (name.charAt(0) == 't' ||
		        name.charAt(0) == 'T') {
		        String suffix = name.substring(1);
		        if (suffix.isEmpty()) {
		            symbol("t0");
		            return;
		        } else  if (StringUtils.isNumeric(suffix)) {
		            int value = Integer.parseInt(suffix);
		            symbol("t"+(value+1));
		            return;
		        }
		    }
		    symbol(name);
        }

		@Override
        public Void visit(KVariable node) {
        	variable(node.name);
            return null;
		}
		
        @Override
        public Void visit(KApp node) {
        	out.append('(');
        	constructor(node.klabel);
            for (KItem k : node.args) {
                out.append(' ');
                k.accept(this);
            }
            out.append(')');
            return null;
        }
        
        @Override
        public Void visit(FreezerHole node) {
        	constructor("HOLE");
            return null;
        }

        @Override
        public Void visit(Freezer node) {
        	out.append('(');
        	constructor("freezer");
        	out.append(' ');
            node.body.accept(this);
            out.append(')');
            return null;
        }

        @Override
        public Void visit(KSequenceVariable node) {
            variable(node.name);            
            return null;
        }        

        @Override
        public Void visit(BoolBuiltin node) {
            out.append('(');
            if (node.value) {
                constructor("'true");
            } else {
                constructor("'false");
            }
            out.append(')');
            return null;
        }

        @Override
        public Void visit(IntBuiltin node) {
        	out.append('(');
        	constructor("#INT");
        	out.append(' ');
        	out.append(node.value.toString());
        	out.append(')');
            return null;
        }

        @Override
        public Void visit(TokenBuiltin node) {
        	out.append('(');
        	constructor(node.sort);
        	out.append(' ');
        	out.append(node.value.toString());
        	out.append(')');
            return null;
        }

        @Override
        public Void visit(Map map) {
            out.append('(');
            for (java.util.Map.Entry<KItem,KItem> item : map.items.entrySet()) {
                out.append('(');
                item.getKey().accept(this);
                out.append(" . ");
                item.getValue().accept(this);
                out.append(')');
            } 
            if (map.rest != null) {
                out.append(" . ");
                variable(map.rest);
            }
            out.append(')');
            return null;
        }

        void cell(Cell cell) {
            out.append("(");
            constructor(cell.label);
            out.append(' ');
            if (cell.contents instanceof KSequence) {
                ksequence((KSequence) cell.contents);
            } else if (cell.contents instanceof CellContentsCells) {
                CellContentsCells cells = (CellContentsCells) cell.contents;
                for (CellContent child : cells.cells) {
                    cellContent(child);
                }
            }
            out.append(')');
        }

        void cellContent(CellContent content) {
            if (content instanceof CellContentCell) {
                cell(((CellContentCell)content).cell);
            } else if (content instanceof CellContentVariable) {
            	variable(((CellContentVariable)content).name);
            }
        }

        void ksequence(KSequence seq) {
            out.append('(');
            constructor("ksequence");
            int i = 0;
            for (K k : seq.contents) {
                if (k instanceof KSequenceVariable) {
                    if (i == seq.contents.size() - 1) {
                        out.append(" .");
                    } else {
                        throw new IllegalArgumentException(
                                "KSequenceVar not at end of list");
                    }
                }
                out.append(' ');
                k(k);
                ++i;
            }
            out.append(')');
        }
        
        void k(K k) {
            k.accept(this);
        }	
	}
	
	/**
	 * Formats a term like a case-match pattern,
	 * with constructors quoted and variables bare
	 */
    private static class PatternWorker extends LispWorker {
    	void constructor(String cons) {
    		out.append('\'');
    		super.symbol(cons);
    	}
    }
    private static class DataWorker extends LispWorker {
    	void variable(String name) {
    		throw new IllegalArgumentException("Ground term should not contain variables, but saw "+name);
    	}
    }

    /**
     * Print a term as a quasiquoted expression
     *
     * Caller needs to supply the opening quasiquote to
     * make a full term.
     * Constructors become simple symbols,
     * variable and expressions are unquoted.
     */
	private static class ExpressionWorker extends LispWorker {
		final Acl2Context context;

		public ExpressionWorker(Acl2Context context) {
			super();
			this.context = context;
			out.append('`');
		}

		void variable(String name) {
			out.append(',');
			super.variable(name);
		}

		@Override
		public Void visit(KApp node) {
			// override to find the production and print it as a hook call if there is one.
			Production prod;
			{
				Collection<Production> labelProds = context.context.productions
						.get(node.klabel);
				if (labelProds.size() == 0) {
					throw new IllegalArgumentException("Found KApp with label "
							+ node.klabel
							+ " for which no productions are known");
				}
				if (labelProds.size() > 1) {
					throw new IllegalArgumentException("Found KApp with label "
							+ node.klabel
							+ " for which multiple productions exist");
				}
				prod = labelProds.iterator().next();
			}
			String hook = prod.getAttribute(Attribute.HOOK_KEY);
			if (null == hook) {
				super.visit(node);
			} else {
				String function = context.getHookImplementation(hook);
				if (null == function) {
					throw new IllegalArgumentException("Found KApp with label "
							+ node.klabel
							+ " which is bound to unimplemented hook " + hook);
				}
				out.append(",(");				
				out.append(function);
				for (KItem k : node.args) {
					out.append(" `");
					k.accept(this);
				}
				out.append(')');
			}
			return null;
		}
    }

	private static String cell(LispWorker worker, Cell cell) {
		worker.cell(cell);
		return worker.out.toString();
	}
    public static String cellPattern(Cell cell) {
    	return cell(new PatternWorker(), cell);
    }
    public static String cellExpression(Acl2Context context, Cell cell) {
    	return cell(new ExpressionWorker(context), cell);
    }
    public static String cellData(Cell cell) {
    	return cell(new DataWorker(), cell);
    }
    private static String k(LispWorker worker, K k) {
    	worker.k(k);
    	return worker.out.toString();
    }
    public static String kPattern(K k) {
        return k(new PatternWorker(), k);
    }	
    public static String kExpression(Acl2Context context, K k) {
    	return k(new ExpressionWorker(context), k);    	
    }
    public static String kData(K k) {
    	return k(new DataWorker(), k);
    }
}