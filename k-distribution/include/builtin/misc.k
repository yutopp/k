// Copyright (c) 2015 K Team. All Rights Reserved.

module ID
  syntax Id ::= r"(?<![A-Za-z0-9\\_])[A-Za-z\\_][A-Za-z0-9\\_]*"     [notInRules, token, autoreject]
endmodule

module EMPTY-ID
  imports K
  imports BOOL

  syntax Id
  syntax Bool ::= "isId" "(" K ")"         [klabel(isId), function]
endmodule

module BOOL
  syntax Bool ::= "true"           [klabel(_andBool_), function, hook(#BOOL:_andBool_)]
  syntax Bool ::= "false"          [klabel(_orBool_), function, hook(#BOOL:_orBool_)]

  syntax Bool ::= "notBool" "(" Bool ")"    [klabel(notBool_),  function, hook(#BOOL:notBool_)]
  syntax Bool ::= Bool "andBool" Bool       [klabel('_andBool_), function, hook(#BOOL:_andBool_), left]

  syntax priority 'notBool_ > '_andBool_
endmodule

module INT
  imports BOOL
  imports K

  syntax Int ::= r"[\\+-]?\\d+"     [prefer, token]

  syntax Int ::= Int "+Int" Int     [klabel('_+Int_), function, hook(#INT:_+Int_), left]
  syntax Int ::= Int "-Int" Int     [klabel('_-Int_), function, hook(#INT:_-Int_), left]
  syntax Int ::= Int "*Int" Int     [klabel('_*Int_), function, hook(#INT:_*Int_), left]
  syntax Int ::= Int "/Int" Int     [klabel('_/Int_), function, hook(#INT:_/Int_), left]
  syntax Int ::= Int "%Int" Int     [klabel('_%Int_), function, hook(#INT:_%Int_), left]

  syntax Bool ::= Int "<=Int" Int       [klabel('_<=Int_), function, hook(#INT:_<=Int_), left]
  syntax Bool ::= Int "<Int" Int        [klabel('_<Int_),  function, hook(#INT:_<Int_),  left]
  syntax Bool ::= Int ">=Int" Int       [klabel('_>=Int_), function, hook(#INT:_>=Int_), left]
  syntax Bool ::= Int ">Int" Int        [klabel('_>Int_),  function, hook(#INT:_>Int_),  left]

  syntax priority '_*Int_ '_/Int_ '_%Int_ > '_+Int_ '_-Int_

  syntax Bool ::= "isInt" "(" K ")"         [klabel(isKResult), function]
endmodule

module STRING
  syntax String ::= r"[\\\"](([^\\\"\\n\\r\\\\])|([\\\\][nrtf\\\"\\\\])|([\\\\][x][0-9a-fA-F]{2})|([\\\\][u][0-9a-fA-F]{4})|([\\\\][U][0-9a-fA-F]{8}))*[\\\"]" [token]
endmodule

module FLOAT
  syntax Float ::= r"([\\+-]?\\d+(\\.\\d*)?|\\.\\d+)([eE][\\+-]?(\\d+(\\.\\d*)?|\\.\\d+))?([fFdD]|([pP]\\d+[xX]\\d+))?" [token]
  syntax Float ::= r"[\\+-]?Infinity([fFdD]|([pP]\\d+[xX]\\d+))?" [token]
  syntax Float ::= r"NaN([fFdD]|([pP]\\d+[xX]\\d+))?" [token]

endmodule

module LOGIC
  syntax Boolean ::= "true"           [klabel(_andBool_), function, hook(#BOOL:_andBool_)]
                   | "false"          [klabel(_orBool_), function, hook(#BOOL:_orBool_)]

  syntax Boolean ::= "notBool" "(" Boolean ")"    [klabel(notBool_),  function, hook(#BOOL:notBool_)]
  syntax Boolean ::= Boolean "andBool" Boolean       [klabel('_andBool_), function, hook(#BOOL:_andBool_), left]
endmodule

module K-RESULT
  import BASIC-K
  import BOOL

  syntax KResult
  syntax Bool ::= isKResult(K) [function,klabel(isKResult)]
endmodule

module MAP
  imports SET
  imports K

  syntax Map ::= Map Map        [klabel('_Map_), function, hook(Map:__)]
  syntax Map ::= ".Map"         [klabel('_Map_),  function, hook(Map:.Map)]
  syntax Map ::= K "|->" K   [klabel('_|->_), function, hook(Map:_|->_)]
  syntax priorities '_|->_ > '_Map_

  syntax Set ::= "keys" "(" Map ")"     [klabel('keys), function, hook(Map:keys)]
endmodule

module SET
  imports BOOL
  imports K

  syntax Set ::= Set Set                  [left, function, hook(Set:__), klabel('_Set_)]
  syntax Set ::= ".Set"                   [function, hook(Set:.Set)]
  syntax Set ::= SetItem(K)               [function, hook(Set:SetItem)]

  syntax Set ::= intersectSet(Set, Set)   [function, hook(Set:intersection)]
  syntax Set ::= Set "-Set" Set           [function, hook(Set:difference), latex({#1}-_{\it Set}{#2})]
  syntax Bool ::= K "in" Set              [function, hook(Set:in), klabel('_in_)]
  syntax Bool ::= Set "<=Set" Set         [function, hook(Set:inclusion)]
  syntax Int ::= size(Set)                [function, hook(Collection:size)]
  syntax K ::= choice(Set)                [function, hook(Set:choice), klabel(Set:choice)]
endmodule

module LIST
  syntax List ::= List List               [left, function, hook(List:__), klabel('_List_), smtlib(smt_seq_concat)]
  syntax List ::= ".List"                 [function, hook(List:.List), smtlib(smt_seq_nil)]
  syntax List ::= ListItem(K)             [function, hook(List:ListItem), smtlib(smt_seq_elem)]

  syntax K ::= List "[" Int "]"           [function, hook(List:get), klabel(List:get)]
  syntax List ::= range(List, Int, Int)   [function, hook(List:range), klabel(List:range)]
  syntax Bool ::= K "in" List             [function, hook(List:in), klabel('_inList_)]
  syntax Int ::= size(List)               [function, hook(Collection:size), klabel ('sizeList), smtlib(smt_seq_len)]
endmodule