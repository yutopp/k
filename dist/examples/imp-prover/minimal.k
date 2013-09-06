module MINIMAL-SYNTAX
  syntax Int ::= #Int
  syntax Id ::= #Id

  syntax AExp  ::= Int | Id
                 > AExp "+" AExp              [left, strict]
                 | "(" AExp ")"               [bracket]
endmodule


module MINIMAL
  imports MINIMAL-SYNTAX

  syntax KResult ::= Int

  syntax Int ::= Int "+Int" Int                 [function, left, hook(#INT:_+Int_)]
  configuration <T color="yellow">
                  <k color="green"> $PGM:AExp </k>
                  <state color="red"> .Map </state>
                </T>

  rule (I1 + I2) + I3 => I1 + (I2 + I3)

// AExp
  rule I1:Int + I2:Int => I1 +Int I2
  rule [lookup]: <k> X:Id => I ...</k> <state>... X |-> I ...</state>
endmodule
