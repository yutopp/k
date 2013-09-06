package org.kframework.backend.acl2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import org.kframework.compile.utils.BasicCompilerStep;
import org.kframework.compile.utils.CompilerStepDone;
import org.kframework.compile.utils.MetaK;
import org.kframework.kil.Attribute;
import org.kframework.kil.Definition;
import org.kframework.kil.Module;
import org.kframework.kil.Production;
import org.kframework.kil.ProductionItem;
import org.kframework.kil.Sort;
import org.kframework.kil.Terminal;
import org.kframework.kil.loader.Context;

/**
 * Declare a syntax predicate for each sort, as a hooked predicate.
 * Hooks are named {@code SORT::<sort>}, matching those registered by
 * {@link GenerateAcl2Syntax}.
 * Assumes the definition has been flattened into a single module.
 * @author bmmoore
 *
 */
public class AddAcl2Predicates extends BasicCompilerStep<Definition> {

	public AddAcl2Predicates(Context context) {
		super(context);
	}

	@Override
	public Definition compile(Definition def, String stepName)
			throws CompilerStepDone {
		Module m = def.getSingletonModule();
		
		Set<String> sorts = m.getAllSorts();
		for (String sort : sorts) {
			if (MetaK.isKSort(sort)) {
				continue;
			}
			
			Production p = new Production(new Sort(Sort.BOOL),
					Arrays.<ProductionItem>asList(
							new Terminal("is"+sort),
							new Terminal("("),
							new Sort("K"),
							new Terminal(")")));
			p.addAttribute(Attribute.PREDICATE);
			p.addAttribute("hook", "SORT::"+sort);
			m.addProduction(Sort.BOOL, p);
			context.productions.put("is"+sort, Sets.newHashSet(p));
		}
		
		return def;
	}

	@Override
	public String getName() {
		return "Add Syntax Predicates for ACL2";
	}

}
