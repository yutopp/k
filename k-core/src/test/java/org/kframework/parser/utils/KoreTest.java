// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.parser.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.kframework.kil.Definition;
import org.kframework.kil.ProductionReference;
import org.kframework.kil.Sort;
import org.kframework.kil.Sources;
import org.kframework.parser.outer.Outer;
import org.kframework.utils.BaseTestCase;
import org.kframework.utils.errorsystem.ParseFailedException;

import java.io.File;

public class KoreTest extends BaseTestCase {

    @Test
    public void testKore() throws Exception {
        //System.out.println(new File(".").getAbsolutePath());
        String quq = FileUtils.readFileToString(new File("k-distribution/samples/kast/quote-unquote.kore"));
        String kore = FileUtils.readFileToString(new File("k-distribution/samples/kast/kore.k"));
        Definition def = new Definition();
        def.setItems(Outer.parse(Sources.generatedBy(KoreTest.class), kore, null));
        ProductionReference pr = null;
        try {
            pr = QuickParser.parse(quq, Sort.of("KDefinition"), def, kem);
        } catch (ParseFailedException e) {
            System.err.println(e.getMessage() + " Line: " + e.getKException().getLocation().lineStart + " Column: " + e.getKException().getLocation().columnStart);
            assert false;
        }
        System.out.println(pr);

        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1.entryState) && nc.isNullable(nt1.exitState));
        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1));
    }
}
