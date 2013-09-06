package org.kframework.backend.acl2;

import org.kframework.compile.utils.MetaK;
import org.kframework.kil.ASTNode;
import org.kframework.kil.Empty;
import org.kframework.kil.KApp;
import org.kframework.kil.KLabelConstant;
import org.kframework.kil.KList;
import org.kframework.kil.Production;
import org.kframework.kil.UserList;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;

@SuppressWarnings("deprecation")
/**
 * Eliminate {@link Empty} nodes, replacing those
 * for user lists with the appropriate terminator and
 * throwing an error otherwise 
 */
public class ResolveEmpty extends CopyOnWriteTransformer {

    public ResolveEmpty(Context context) {
        super("Turn Empty nodes into lists", context);
    }

    @Override
    public ASTNode transform(Empty emp) {
        String l = emp.getLocation();
        String f = emp.getFilename();
        Production listProd = context.listConses.get(emp.getSort());
        if (listProd == null) {
            throw new IllegalArgumentException(
              "Empty node "+emp+"of non-list sort "+emp.getSort());
        }
        String separator = ((UserList) listProd.getItems().get(0)).getSeparator();
        return new KApp(l, f, KLabelConstant.of(MetaK.getListUnitLabel(separator), context), KList.EMPTY);
    }
}
