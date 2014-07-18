// Copyright (c) 2010-2014 K Team. All Rights Reserved.
require "builtins/model-checker.k"

module IMP-SYNTAX
  imports LTL-HOOKS
  imports MODEL-CHECKER-HOOKS

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

  syntax Prop ::= Id
                | "eqToSum" "(" Id "," Id ")"
                | "eqTo" "(" Id "," Int ")"
                | "logInv" "(" Id "," Id "," Id ")"
                |  "lt" "(" Id "," Val ")" 
                |  "leq" "(" Id "," Val ")" 
                |  "gt" "(" Id "," Val ")" 
                |  "geq" "(" Id "," Val ")" 
                |  "neqTo" "(" Id "," Val ")" 
                |  "eq" "(" Id "," Id ")"


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
  rule <k> X:Id => I ...</k> <state>... X |-> I ...</state>

  rule <k> V:KResult => .K ...</k> <strict> .K => V </strict>

  rule E1 / E2 => E1 ~> HOLE / E2 requires isKResult(E1) =/=K true
  rule <k> HOLE / E2 => V / E2 ...</k> <strict> V:KResult => .K </strict>
  rule E1 / E2 => E2 ~> E1 / HOLE requires isKResult(E2) =/=K true
  rule <k> E1 / HOLE => E1 / V ...</k> <strict> V:KResult => .K </strict>

  rule I1 / I2 => I1 /Int I2  when I2 =/=Int 0

  rule E1 + E2 => E1 ~> HOLE + E2 requires isKResult(E1) =/=K true
  rule <k> HOLE + E2 => V + E2 ...</k> <strict> V:KResult => .K </strict>
  rule E1 + E2 => E2 ~> E1 + HOLE requires isKResult(E2) =/=K true
  rule <k> E1 + HOLE => E1 + V ...</k> <strict> V:KResult => .K </strict>

  rule I1 + I2 => I1 +Int I2
// BExp


  rule E1 <= E2 => E1 ~> HOLE <= E2 requires isKResult(E1) =/=K true
  rule <k> HOLE <= E2 => V <= E2 ...</k> <strict> V:KResult => .K </strict>
  rule E1:KResult <= E2 => E2 ~> E1 <= HOLE requires isKResult(E2) =/=K true
  rule <k> E1 <= HOLE => E1 <= V ...</k> <strict> V:KResult => .K </strict>

  rule I1 <= I2 => I1 <=Int I2

  rule ! E1 => E1 ~> ! HOLE requires isKResult(E1) =/=K true
  rule <k> ! HOLE => ! V ...</k> <strict> V:KResult => .K </strict>

  rule ! T => notBool T

  rule E1 && E2 => E1 ~> HOLE && E2 requires isKResult(E1) =/=K true
  rule <k> HOLE && E2 => V && E2 ...</k> <strict> V:KResult => .K </strict>

  rule true && B => B
  rule false && _ => false
// Block
  rule {} => .   [structural]
  rule {S} => S  [structural]
// Stmt
  rule X = E2; => E2 ~> X = HOLE; requires isKResult(E2) =/=K true
  rule <k> X = HOLE; => X = V; ...</k> <strict> V:KResult => .K </strict>

  rule <k> X = I:Int; => . ...</k> <state>... X |-> (_ => I) ...</state>
  rule S1:Stmt S2:Stmt => S1 ~> S2  [structural]

  rule if (E2) S1 else S2 => E2 ~> if (HOLE) S1 else S2 
    requires isKResult(E2) =/=K true
  rule <k> if (HOLE) S1 else S2 => if (V) S1 else S2 ...</k> 
       <strict> V:KResult => .K </strict>

  rule if (true)  S else _ => S
  rule if (false) _ else S => S
  rule while (B) S => if (B) {S while (B) S} else {}  [while]
// Pgm
  rule <k> int (X,Xs => Xs);_ </k> <state> Rho:Map (.Map => X|->0) </state>
    when notBool (X in keys(Rho))
  rule int .Ids; S => S  [structural]

/*
  syntax Bool ::= Bag "||=" String [function]
  syntax Id ::= Token{"x"}
  rule <generatedTop>... <state>... x |-> I:Int ...</state> ...</generatedTop> ||= S:String
    => ("p" +String Int2String(I)) ==String S

  syntax Bool ::= Bag "|=" String [function]
  rule <generatedTop> B </generatedTop> |= S:String 
    => (<generatedTop> B </generatedTop> ||= S) ==K true
*/

// LTL stuff
   syntax Val ::= Int

  /*@
    In order to give semantics to the proposition \texttt{eqTo}, we use an 
    auxiliary function \texttt{val} that returns the value of a given variable
    name in a given configuration: 
   */

  syntax Int ::= "val" "(" Bag "," Id ")" [function]

  rule val(<generatedTop>... <T>... 
                 <state>... L |-> I:Int ...</state> 
           ...</T> ...</generatedTop>, X)
       =>
       I

  //@ Some additional domain functions:
 
  syntax Int ::= "sumFirstN" "(" Int ")" [function]
  rule sumFirstN(0) => 0 [anywhere]
  rule sumFirstN(N:Int) => sumFirstN(N -Int 1) +Int N requires N >Int 0 [anywhere]

  syntax Int ::= "pow" "(" Int "," Int ")" [function]
  rule pow(X:Int, 0) => 1 
  rule pow(X:Int, Y:Int) => X *Int pow(X, Y -Int 1) requires Y >Int 0

  /*@
    We are ready now to give the semantics for atomic propositions, which is
    self explained:
   */

  

  rule B:Bag |=Ltl eqTo(X:Id, I:Int) => true requires val(B, X) ==Int I [ltl, anywhere]

  rule B:Bag |=Ltl neqTo(X:Id, I:Int) => true requires val(B, X) =/=Int I [ltl, anywhere]

  rule B:Bag |=Ltl lt(X:Id, I:Int) => true requires val(B, X) <Int I [ltl, anywhere]

  rule B:Bag |=Ltl leq(X:Id, I:Int) => true requires val(B, X) <=Int I [ltl, anywhere]

  rule B:Bag |=Ltl gt(X:Id, I:Int) => true requires val(B, X) >Int I [ltl, anywhere]

  rule B:Bag |=Ltl geq(X:Id, I:Int) => true requires val(B, X) >=Int I [ltl, anywhere]

  rule B:Bag |=Ltl eq(X:Id, Y:Id) => true requires val(B, X) ==Int val(B, Y) [ltl, anywhere]

/*
  rule <generatedTop>...
         <T>...
           <threads>
             <thread>...
               <k> L:Id : S:Stmt ...</k>
             ...</thread>
           </threads>
         ...</T>
       ...</generatedTop>
       |=Ltl L
       =>
       true
       [anywhere]
*/

  rule B:Bag |=Ltl eqToSum(X:Id, Y:Id) => true
       requires val(B, X) ==Int sumFirstN(val(B, Y))
       [ltl, anywhere]

  rule B:Bag |=Ltl eqTo(X:Id, I:Int) => true
       requires val(B, X) ==Int I
       [ltl, anywhere]

  rule B:Bag |=Ltl logInv(A:Id, X:Id, K:Id) => true
       requires (val(B, X) *Int pow(2, val(B, K)) <=Int val(B, A)) 
             andBool (val(B, A) <Int (val(B, X) +Int 1) *Int pow(2, val(B, K)))
       [ltl, anywhere]

endmodule
