// Copyright (c) 2014-2015 K Team. All Rights Reserved.

package org.kframework.kore.convertors;

import org.junit.Test;
import org.junit.rules.TestName;
import org.kframework.kore.Kompile;
import org.kframework.definition.Module;
import org.kframework.kore.K;
import org.kframework.tiny.Rewriter;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Function;


public class TstTinyOnKORE {


    @org.junit.Rule
    public TestName name = new TestName();


    protected File testResource(String baseName) {
        return new File(new File("k-distribution/src/test/resources" + baseName)
                .getAbsoluteFile().toString().replace("k-distribution" + File.separator + "k-distribution", "k-distribution"));
        // a bit of a hack to get around not having a clear working directory
        // Eclipse runs tests within k/k-distribution, IntelliJ within /k
    }

    @Test
    public void testCSemantics() throws IOException, URISyntaxException {
        String filename = "/home/dwightguth/c-semantics/semantics/c11-translation.k";
        Tuple2<Module, Function<String, K>> rwModuleAndProgramParser = Kompile.getStuff(new File(filename),
                "C11-TRANSLATION", "C11-TRANSLATION");
        System.out.println(rwModuleAndProgramParser._1());;
        K program = rwModuleAndProgramParser._2().apply("t(.Set, int) ==Type t(.Set, int)");
        Rewriter rewriter = Kompile.getRewriter(rwModuleAndProgramParser._1());
        K result = rewriter.execute(program);
        System.out.println(result);
    }

    @Test
    public void kore_imp_tiny() throws IOException, URISyntaxException {

        String filename = "/convertor-tests/" + name.getMethodName() + ".k";

        File definitionFile = testResource(filename);
        Tuple2<Module, Function<String, K>> rwModuleAndProgramParser = Kompile.getStuff(definitionFile, "TEST", "TEST-PROGRAMS");

        Module module = rwModuleAndProgramParser._1();
        Function<String, K> programParser = rwModuleAndProgramParser._2();
        Rewriter rewriter = Kompile.getRewriter(module);

        K program = programParser.apply(
                "<top><k> while(0<=n) { s = s + n; n = n + -1; } </k><state>n|->10 s|->0</state></top>");

        long l = System.nanoTime();
        K result = rewriter.execute(program);
        System.out.println("time = " + (System.nanoTime() - l) / 1000000);

        System.out.println("result = " + result.toString());
    }

}
