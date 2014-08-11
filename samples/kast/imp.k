// Copyright (c) 2010-2014 K Team. All Rights Reserved.

module IMP-SYNTAX
  syntax AExp  ::= Int | Id
                 | AExp "/" AExp              [left, strict, klabel(`_/_`)]
                 > AExp "+" AExp              [left, strict, klabel(`_+_`)]
                 | "(" AExp ")"               [bracket]
  syntax BExp  ::= Bool
                 | AExp "<=" AExp             [seqstrict, latex({#1}\leq{#2}), klabel(`_<=_`)]
                 | "!" BExp                   [strict, klabel(`!_`)]
                 > BExp "&&" BExp             [left, strict(1), klabel(`_&&_`)]
                 | "(" BExp ")"               [bracket]
  syntax Block ::= "{" "}"                    [kconstant(`{}`)]
                 | "{" Stmt "}"               [klabel(`{_}`)]
  syntax Stmt  ::= Block
                 | Id "=" AExp ";"            [strict(2), klabel(`_=_;`)]
                 | "if" "(" BExp ")"
                   Block "else" Block         [strict(1), klabel(`if(_)_else_`)]
                 | "while" "(" BExp ")" Block [klabel(`while(_)_`)]
                 > Stmt Stmt                  [left, klabel(`__::Stmt*Stmt->Stmt`)]
  syntax Pgm ::= "int" Ids ";" Stmt           [klabel(`int_;_`)]
  syntax Ids ::= List{Id,","}                 [klabel(`_,_`)]
endmodule

/* We assumed that the following constants have been assigned the following
   kconstant labels in their respective modules:
   * `0::Int` for the 0 of module INT
   * `true::Bool` for the true of BOOL
   * `false::Bool` for the false of BOOL
   * `.::Map` for the . of module MAP
*/

module IMP
  imports IMP-SYNTAX
  syntax KResult ::= Int | Bool

  configuration <T color="yellow">
                  <k color="green"> $PGM:Pgm </k>
                  <state color="red"> `.::Map` </state>
                </T>
// AExp
  rule <k> X:Id => I:K ...</k> <state>... `_|->_`(X:Id,I:K) ...</state>
  rule `_/_`(I1:Int,I2:Int) => `_/Int_`(I1:Int,I2:Int) requires `_=/=Int_`(I2:Int,`0::Int`)
  rule `_+_`(I1:Int,I2:Int) => `_+Int_`(I1,I2) // add all sorts to all vars?
// BExp
  rule `_<=_`(I1:Int,I2:Int) => `_<=Int_`(I1,I2)
  rule `!_`(T:Bool) => `notBool_`(T)
  rule `_&&_`(`true::Bool`,B) => B
  rule `_&&_`(`false::Bool`,_) => `false::Bool`
// Block
  rule `{}` => .K     [structural]
  rule `{_}`(S) => S  [structural]
// Stmt
  rule <k> `_=_;`(X,I:Int) => .K ...</k> <state>... `_|->_`(X,`_=>I`) ...</state>
  rule `__::Stmt*Stmt->Stmt`(S1:Stmt,S2:Stmt) => S1 ~> S2  [structural]
  rule `if(_)_else_`(`true::Bool`,S,_) => S
  rule `if(_)_else_`(`false::Bool`,_,S) => S
  rule `while(_)_`(B,S) => `if(_)_else_`(B,`{_}`(`__`(S,`while(_)_`(B,S))),`{}`)  [structural]
// Pgm
  rule <k> `int_;_`(`_,_`(X:Id,Xs) => Xs) </k>
       <state> `__::Map`(Rho:Map, `.::Map` => `_|->_`(X,`0::Int`)) </state>
    requires `notBool_`(`_in_`(X,`keys`(Rho)))
  rule `int_;_`(`.::Ids`,S) => S  [structural]
endmodule
