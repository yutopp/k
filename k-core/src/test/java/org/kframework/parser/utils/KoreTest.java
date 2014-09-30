// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.parser.utils;

import org.junit.Assert;
import org.junit.Test;
import org.kframework.kil.Definition;
import org.kframework.kil.Sort;
import org.kframework.kil.Sources;
import org.kframework.parser.outer.Outer;
import org.kframework.utils.file.FileUtil;

import java.io.File;

public class KoreTest {

    @Test
    public void testKore() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        String quq = FileUtil.getFileContent("k-distribution/samples/kast/quote-unquote.kore");
        String kore = FileUtil.getFileContent("k-distribution/samples/kast/kore.k");
        Definition def = new Definition();
        def.setItems(Outer.parse(Sources.generatedBy(KoreTest.class), kore, null));
        QuickParser.parse(quq, Sort.of("KDefinition"), def);


        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1.entryState) && nc.isNullable(nt1.exitState));
        //Assert.assertEquals("Expected Nullable NTs", true, nc.isNullable(nt1));
    }
}
