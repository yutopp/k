package org.kframework.backend.pdmc.pda;

import junit.framework.Assert;
import org.junit.Test;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomaton;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;

/**
 * @author TraianSF
 */
public class PostStarTest {

    @Test
    public void testPostStar() throws Exception {

        // Schwoon's thesis, Figure 3.1, page 32
        PushdownSystem pds = PushdownSystem.of("" +
                "r1: <p0, g0> => <p1, g1 g0>;\n" +
                "r2: <p1, g1> => <p2, g2 g0>;\n" +
                "r3: <p2, g2> => <p0, g1>;\n" +
                "r4: <p0, g1> => <p0>;\n" +
                "<p0, g0 g0>");
        TrackingLabelFactory<String, String> factory = new TrackingLabelFactory<>();
        PostStar<String, String> postStar = new PostStar(pds, factory);

        // Schwoon's Thesis, Figure 3.5, page 49, labelling in Figure 3.7, page 60
//        PAutomaton<PAutomatonState<String, String>, String> expectedPostStar = PAutomaton.of("" +
//                "p1 g1 <p1,g1>[r1];" +
//                "<p2,g2> g0 <p1,g1>[r2];" +
//                "s1! g0 s2!;" +
//                "p0 <p2,g2>[r4];" +
//                "<p1,g1> g0 s1![r1];" +
//                "<p1,g1> g0 <p1,g1>[r1,<p2,g2>];" +
//                "p2 g2 <p2,g2>[r2];" +
//                "p0 g1 <p2,g2>[r3];" +
//                "p0 g0 <p1,g1>;" +
//                "p0 g0 s1!;" +
//                "p0;" +
//                "s2!");
        System.err.println(postStar.toString());
//        System.err.print(expectedPostStar.toString());
        System.err.println("\n---------------------------");
//        Assert.assertEquals(expectedPostStar.length(), aPostStar.toString().length());
    }
}
