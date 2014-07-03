/*
                r1: <p0, g0> => <p1, g1 g0>;
                r2: <p1, g1> => <p2, g2 g0>;
                r3: <p2, g2> => <p0, g1>;
                r4: <p0, g1> => <p0>;
                <p0, g0 g0>
*/

module EXAMPLE
  syntax State ::= Token{[p][0-9]}     
  syntax Alphabet ::= Token{[g][0-9]}  

  configuration 
    <k> g0 ~> g0 </k>  <state> p0 </state>

  rule <k> g0 => g1 ~> g0 ...</k> <state> p0 => p1 </state> [r1]
  rule <k> g1 => g2 ~> g0 ...</k> <state> p1 => p2 </state> [r2]
  rule <k> g2 => g1 ...</k> <state> p2 => p0 </state> [r3]
  rule <k> g1 => .K ...</k> <state> p0 </state> [r4]
endmodule
