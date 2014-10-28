// Copyright (c) 2010-2014 K Team. All Rights Reserved.

require "EVERYTHING REQUIRED BY THIS DEFINITION"

module IMP-SYNTAX
  imports EVERYTHING-THAT-THIS-DEF-IMPORTS
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

  configuration `<T>`(`__::Bag`(
                      `<k>`(PGM:Pgm),
                      `<state>`(`.::Map`(.::MetaKList))
                   )
                )
// AExp
  rule `__::Bag`(`<k>`(X:Id => I:K ~> _:MetaK), `<state>`(`_Map_`(`_|->_`(X:Id,I:K), _:Map)))
  rule `_/_`(I1:Int,I2:Int) => `_/Int_`(I1:Int,I2:Int) requires `_=/=Int_`(I2:Int, #token("Int", "0"))
  rule `_+_`(I1:Int,I2:Int) => `_+Int_`(I1:Int,I2:Int)
// BExp
  rule `_<=_`(I1:Int,I2:Int) => `_<=Int_`(I1:Int,I2:Int)
  rule `!_`(T:Bool) => `notBool_`(T:Bool)
  rule `_&&_`(#token("Bool", "true"),B:K) => B:K
  rule `_&&_`(#token("Bool", "false"),_:K) => #token("Bool", "false")
// Block
  rule `{}`(.::MetaKList) => .::MetaK      [structural]
  rule `{_}`(S:K) => S:K                   [structural]
// Stmt
  rule `__::Bag`(`<k>`(`_=_;`(X:Id,I:Int) => `.::MetaK`(.::MetaKList) ~> _:MetaK),
                 `<state>`(`__::Map`(`_|->_`(X:Id,`_:K => I:Int`), _:Map)))
  rule `__::Stmt*Stmt->Stmt`(S1:Stmt,S2:Stmt) => S1:Stmt ~> S2:Stmt  [structural]
  rule `if(_)_else_`(#token("Bool", "true") , S:K, _:K) => S:K
  rule `if(_)_else_`(#token("Bool", "false"), _:K, S:K) => S:K
  rule `while(_)_`(B:MetaK,S:MetaK)
    => `if(_)_else_`(B:K,`{_}`(`__`(S:K,`while(_)_`(B:K,S:K))),`{}`(.::MetaKList))  [structural]
// Pgm
  rule `__::Bag`(
           `<k>`    (`int_;_`(`_,_`(X:Id,Xs:Ids) => Xs:Ids)),
           `<state>`(`__::Map`(Rho:Map, ``.::Map`(.::MetaKList) => `_|->_`(X:Id, #token("Int", "0"))`)))
    requires `notBool_`(`_in_`(X:Id,`keys`(Rho)))
  rule `int_;_`(`.::Ids`(.::MetaKList), S:K) => S:K  [structural]
endmodule
