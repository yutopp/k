/*

*/

module EXAMPLE
  syntax State ::= Token{[x][0-9]+}     
  syntax Alphabet ::= "p" | "incx" | "skip" | "ret" | "bot"

//                "<x0, p bot>");
  configuration 
    <k> p ~> bot </k>  <state> x0 </state>

//                "r1: <x0, p>     => <x0, skip ret>;\n" +
  rule <k> p => skip ~> ret ...</k> <state> x0 </state> [r1]
//                "r2: <x0, p>     => <x01, incx ret>;\n" +
  rule <k> p => incx ~> ret ...</k> <state> x0 => x01 </state> [r2]
//                "r3: <x01, incx> => <x0, p incx>;\n" +
  rule <k> incx => p ~> incx ...</k> <state> x01 => x0 </state> [r3]
//                "r4: <x0, skip>  => <x0>;\n" +
  rule <k> skip => .K ...</k> <state> x0 </state> [r4]
//                "r5: <x0, incx>  => <x1>;\n" +
  rule <k> incx => .K ...</k> <state> x0 => x1 </state> [r5]
//                "r6: <x1, incx>  => <x2>;\n" +
  rule <k> incx => .K ...</k> <state> x1 => x2 </state> [r6]
//                "r7: <x2, incx>  => <x0>;\n" +
  rule <k> incx => .K ...</k> <state> x2 => x0 </state> [r7]
//                "r8: <x0, ret>   => <x0>;\n" +
  rule <k> ret => .K ...</k> <state> x0 </state> [r8]
//                "r9: <x1, ret>   => <x1>;\n" +
  rule <k> ret => .K ...</k> <state> x1 </state> [r8]
//                "r10: <x2, ret>   => <x2>;\n" +
  rule <k> ret => .K ...</k> <state> x2 </state> [r8]
//                "r11: <x0, bot>   => <x0, bot>;\n" +
//                "r12: <x01, bot>   => <x01, bot>;\n" +
//                "r13: <x1, bot>   => <x1, bot>;\n" +
//                "r14: <x2, bot>   => <x2, bot>;\n" +
  rule <k> bot => bot ...</k> <state> _ </state>

  syntax Bool ::= Bag "||=" String [function]
  rule <generatedTop>... <state> x0 </state> ...</generatedTop> ||= "px0"
    => true
  rule <generatedTop>... <state> x01 </state> ...</generatedTop> ||= "px0"
    => true
  rule <generatedTop>... <state> x1 </state> ...</generatedTop> ||= "px1"
    => true
  rule <generatedTop>... <state> x2 </state> ...</generatedTop> ||= "px2"
    => true

  syntax Bool ::= Bag "|=" String [function]
  rule <generatedTop> B </generatedTop> |= S:String 
    => (<generatedTop> B </generatedTop> ||= S) ==K true
endmodule
