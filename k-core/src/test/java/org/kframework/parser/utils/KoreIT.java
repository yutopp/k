// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.parser.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.kframework.kil.Definition;
import org.kframework.kil.ProductionReference;
import org.kframework.kil.Sort;
import org.kframework.kil.Sources;
import org.kframework.parser.outer.Outer;
import org.kframework.utils.BaseTestCase;
import org.kframework.utils.errorsystem.ParseFailedException;
import org.kframework.utils.file.JarInfo;

import java.io.File;

/**
 * Test a the kore definition using the new parser.
 * KoreIT has to be run after the distribution has been built (apparently), therefore the name IT at the end.
 */
public class KoreIT extends BaseTestCase {

    @Test
    public void testKore() throws Exception {
        //System.out.println(JarInfo.getKBase(false));
        String quq = FileUtils.readFileToString(new File(JarInfo.getKBase(false) + "/samples/kast/quote-unquote.kore"));
        String kore = FileUtils.readFileToString(new File(JarInfo.getKBase(false) + "/samples/kast/kore.k"));
        Definition def = new Definition();
        def.setItems(Outer.parse(Sources.generatedBy(KoreIT.class), kore, null));
        ProductionReference pr = null;
        try {
            pr = QuickParser.parse(quq, Sort.of("KDefinition"), def, kem);
        } catch (ParseFailedException e) {
            System.err.println(e.getMessage() + " Line: " + e.getKException().getLocation().lineStart + " Column: " + e.getKException().getLocation().columnStart);
            assert false;
        }
        //System.out.println(pr);

        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1.entryState) && nc.isNullable(nt1.exitState));
        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1));
    }
}
