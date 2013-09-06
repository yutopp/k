module BASIC-SYNTAX
  syntax Int ::= #Int
  syntax Id ::= #Id
  syntax #Bool ::= "true"
  syntax #Bool ::= "false"
  syntax Bool ::= #Bool
endmodule

module BASIC-SEMANTICS
  syntax Int ::= Int "/Int" Int                 [function, left, hook(#INT:_/Int_)]
               | Int "+Int" Int                 [function, left, hook(#INT:_+Int_)]
  syntax Bool ::=
                 Int "=/=Int" Int        [function, left, hook(#INT:_=/=Int_)]
               | Int "<=Int" Int        [function, left, hook(#INT:_<=Int_)]

  syntax Bool ::= "notBool" Bool          [function, left, hook(#BOOL:notBool_)]

  syntax Bool ::= K "in" Set   [function, hook(#SET:_inSet_)]
  syntax Set ::= keys(Map)   [function, hook(#MAP:keys)]
endmodule