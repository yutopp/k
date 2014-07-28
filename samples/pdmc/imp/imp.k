// Copyright (c) 2010-2014 K Team. All Rights Reserved.
module IMP-SYNTAX
  syntax AExp  ::= Int | Id
                 | AExp "/" AExp              [left, pdstrict]
                 > AExp "+" AExp              [left, pdstrict]
                 | "(" AExp ")"               [bracket]
  syntax BExp  ::= Bool
                 | AExp "<=" AExp             [pdseqstrict, latex({#1}\leq{#2})]
                 | "!" BExp                   [pdstrict]
                 > BExp "&&" BExp             [left, pdstrict(1)]
                 | "(" BExp ")"               [bracket]
  syntax Block ::= "{" "}"
                 | "{" Stmt "}"
  syntax Stmt  ::= Block
                 | Id "=" AExp ";"            [pdstrict(2)]
                 | "if" "(" BExp ")"
                   Block "else" Block         [pdstrict(1)]
                 | "while" "(" BExp ")" Block
                 > Stmt Stmt                  [left]
  syntax Pgm ::= "int" Ids ";" Stmt
  syntax Ids ::= List{Id,","}
endmodule


module IMP
  imports IMP-SYNTAX
  syntax KResult ::= Int | Bool

  configuration <T color="yellow">
                  <k color="green"> $PGM:Pgm </k>
                  <state color="red"> .Map </state>
                  <strict> .K </strict>
                </T>

// AExp
  rule [lookup] : <k> X:Id => I ...</k> <state>... X |-> I ...</state>

  rule [value] : <k> V:KResult => .K ...</k> <strict> .K => V </strict>

  rule [div-heatl] : E1 / E2 => E1 ~> HOLE / E2 requires isKResult(E1) =/=K true
  rule [div-cooll] : <k> HOLE / E2 => V / E2 ...</k> <strict> V:KResult => .K </strict>
  rule [div-heatr] : E1 / E2 => E2 ~> E1 / HOLE requires isKResult(E2) =/=K true
  rule [div-coolr] : <k> E1 / HOLE => E1 / V ...</k> <strict> V:KResult => .K </strict>

  rule [div] : I1 / I2 => I1 /Int I2  when I2 =/=Int 0

  rule [add-heatl] : E1 + E2 => E1 ~> HOLE + E2 requires isKResult(E1) =/=K true
  rule [add-cooll] : <k> HOLE + E2 => V + E2 ...</k> <strict> V:KResult => .K </strict>
  rule [add-heatr] : E1 + E2 => E2 ~> E1 + HOLE requires isKResult(E2) =/=K true
  rule [add-coolr] : <k> E1 + HOLE => E1 + V ...</k> <strict> V:KResult => .K </strict>

  rule [add] : I1 + I2 => I1 +Int I2
// BExp


  rule [leq-heatl] : E1 <= E2 => E1 ~> HOLE <= E2 requires isKResult(E1) =/=K true
  rule [leq-cooll] : <k> HOLE <= E2 => V <= E2 ...</k> <strict> V:KResult => .K </strict>
  rule [leq-heatr] : E1:KResult <= E2 => E2 ~> E1 <= HOLE requires isKResult(E2) =/=K true
  rule [leq-coolr] : <k> E1 <= HOLE => E1 <= V ...</k> <strict> V:KResult => .K </strict>

  rule [leq] : I1 <= I2 => I1 <=Int I2

  rule [not-heat] : ! E1 => E1 ~> ! HOLE requires isKResult(E1) =/=K true
  rule [not-cool] : <k> ! HOLE => ! V ...</k> <strict> V:KResult => .K </strict>

  rule [not] : ! T => notBool T

  rule [and-heat] : E1 && E2 => E1 ~> HOLE && E2 requires isKResult(E1) =/=K true
  rule [and-cool] : <k> HOLE && E2 => V && E2 ...</k> <strict> V:KResult => .K </strict>

  rule [and-true] : true && B => B
  rule [and-false] : false && _ => false
// Block
  rule [empty-block] : {} => .   [structural]
  rule [block] : {S} => S  [structural]
// Stmt
  rule [asgn-heat] : X = E2; => E2 ~> X = HOLE; requires isKResult(E2) =/=K true
  rule [asgn-cool] : <k> X = HOLE; => X = V; ...</k> <strict> V:KResult => .K </strict>

  rule [asgn] : <k> X = I:Int; => . ...</k> <state>... X |-> (_ => I) ...</state>
  rule [seq] : S1:Stmt S2:Stmt => S1 ~> S2  [structural]

  rule [if-heat] : if (E2) S1 else S2 => E2 ~> if (HOLE) S1 else S2
    requires isKResult(E2) =/=K true
  rule [if-cool] : <k> if (HOLE) S1 else S2 => if (V) S1 else S2 ...</k>
       <strict> V:KResult => .K </strict>

  rule [if-true] : if (true)  S else _ => S
  rule [if-false] : if (false) _ else S => S
  rule [while] : while (B) S => if (B) {S while (B) S} else {}  [while]
// Pgm
  rule [decl] : <k> int (X,Xs => Xs);_ </k> <state> Rho:Map (.Map => X|->0) </state>
    when notBool (X in keys(Rho))
  rule [start] : int .Ids; S => S  [structural]

  syntax Bool ::= Bag "||=" String [function]
  syntax Id ::= Token{"x"}
  rule <generatedTop>... <state>... x |-> I:Int ...</state> ...</generatedTop> ||= S:String
    => ("p" +String Int2String(I)) ==String S

  syntax Bool ::= Bag "|=" String [function]
  rule <generatedTop> B </generatedTop> |= S:String
    => (<generatedTop> B </generatedTop> ||= S) ==K true

endmodule
