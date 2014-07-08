/*
     "<x0, p>     => <x0, skip ret>;\n" +
     "<x0, p>     => <x01, incx ret>;\n" +
     "<x01, incx> => <x0, p incx>;\n" +
     "<x0, skip>  => <x0 , eps>;\n" +
     "<x0, incx>  => <x1, eps>;\n" +
     "<x1, incx>  => <x2, eps>;\n" +
     "<x2, incx>  => <x0, eps>;\n" +
     "<x0, ret>   => <x0 , eps>;\n" +
     "<x1, ret>   => <x1 , eps>;\n" +
     "<x2, ret>   => <x2 , eps>"
*/

module EXAMPLE
  syntax State ::= Token{[x][0-9]+}     
  syntax Alphabet ::= "p" | "incx" | "skip" | "ret"

  configuration 
    <k> p </k>  <state> x0 </state>

  rule <k> p => skip ~> ret ...</k> <state> x0 </state> [r1]
  rule <k> p => incx ~> ret ...</k> <state> x0 => x01 </state> [r2]
  rule <k> incx => p ~> incx ...</k> <state> x01 => x0 </state> [r3]
  rule <k> skip => .K ...</k> <state> x0 </state> [r4]
  rule <k> incx => .K ...</k> <state> x0 => x1 </state> [r5]
  rule <k> incx => .K ...</k> <state> x1 => x2 </state> [r6]
  rule <k> incx => .K ...</k> <state> x2 => x0 </state> [r7]
  rule <k> ret => .K ...</k> <state> x0 </state> [r8]
  rule <k> ret => .K ...</k> <state> x1 </state> [r8]
  rule <k> ret => .K ...</k> <state> x2 </state> [r8]
endmodule
