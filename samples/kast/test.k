// Copyright (c) 2014 K Team. All Rights Reserved.
require "modules/k-parser.k"
module K-SORT
  syntax MetaK
endmodule


module OUTER
  imports K-SORT

  // Top level module stuff
  syntax KDefinition   ::= KRequireList KModuleList
  syntax KRequireList  ::= List{KRequire,""}
  syntax KRequire      ::= "require" KString
  syntax KModuleList   ::= List{KModule,""}
  syntax KModule       ::= "module" KModuleName KImportList KSentenceList "endmodule"
  syntax KImportList   ::= List{KImport,""}
  syntax KImport       ::= "imports" KModuleName
  syntax KSentenceList ::= List{Sentence,""}

  syntax KModuleName   ::= Token{"dummy"} [onlyLabel, noAutoReject, regex(#?[a-zA-Z](\-?[a-zA-Z0-9])*)]

  syntax Sentence ::= KSyntacticSentence
                    | KSemanticSentence

  syntax KSyntacticSentence ::= "syntax" KSort "::=" KPriorityBlockList
                              | "syntax" KSort
                              | "syntax" KSort KAttributes
  syntax KPriorityBlockList ::= NeList{KPriorityBlock,">"}
  syntax KPriorityBlock ::=             KProductionList
                         | "left:"      KProductionList
                         | "right:"     KProductionList
                         | "non-assoc:" KProductionList
  syntax KProductionList   ::= NeList{KProduction,"|"}
  syntax KProduction       ::= KSimpleProduction KAttributes
  syntax KSimpleProduction ::= NeList{KProductionItem,""}
                             | KTag "(" KSortList ")"
  syntax KSortList ::= List{KSort,","}
  syntax KProductionItem ::= KSort                            // non-terminal
                           | KString                          // terminals
                           | "Token{" KString "}"             // token
                           |   "List{" KSort "," KString "}"  // lists
                           | "NeList{" KSort "," KString "}"  // nelists
                           // maybe more

  syntax KSemanticSentence ::= "configuration" MetaK KAttributes
                             | "rule"          MetaK KAttributes
                             | "context"       MetaK KAttributes

  syntax KAttributes ::= ""                     [onlyLabel, klabel(noAttributes)]
                       | "[" KAttributeList "]" [prefer]
                       
  syntax KAttributeList ::= List{KAttribute,","}

  syntax KAttribute ::= KTag
                      | KTag "(" KTagContent ")" // To consider: maybe allow KItem in here?
                      | KTag "(" KString     ")"
					  
  syntax KTagContent ::= List{KTagC,""} [token] // anything with balanced parenthesis. Will have to get original string.
  syntax KTagC       ::= Token{"dummy"} [onlyLabel, noAutoReject, regex("[^\\n\\r\\(\\)\\\"]+")]
  syntax KTagC       ::= "(" KTagContent ")"


  syntax KString ::= Token{"dummy"} [onlyLabel, noAutoReject, regex("[\\\"]([^\\\"\\n\\r\\\\]|[\\\\][nrtf\\\"\\\\])*[\\\"]")]
  syntax KSort   ::= Token{"dummy"} [onlyLabel, noAutoReject, regex((?<![A-Za-z0-9])#?[A-Z][a-zA-Z0-9]*)]
  syntax KTag    ::= Token{"dummy"} [onlyLabel, noAutoReject, regex([a-z][A-Za-z\-0-9]*)]
  syntax KTagList ::= List{KTag,","}

endmodule


module BUBBLE
  syntax Bubble ::= Token{"dummy"} [onlyLabel, noAutoReject, regex(((?!\Srule|\Sendmodule|\Ssyntax|\Sconfiguration|\Scontext)(\S+)|\s+)+)]
                  //| Token{"dummy"} [onlyLabel, noAutoReject, regex([\`]{1}([^\`])+[\`]{1})]
                  //| Token{"dummy"} [onlyLabel, noAutoReject, regex([\`]{2}([^\`]|[\`][^\`])+[\`]{2})]
                  //| Token{"dummy"} [onlyLabel, noAutoReject, regex([\`]{3}([^\`]|[\`][^\`]|[\`][\`][^\`])+[\`]{3})]
                  // can add more if we want to
endmodule


module OUTER-WITH-BUBBLES
  imports OUTER
  imports BUBBLE
  syntax MetaK ::= Bubble
endmodule


module KAST
  imports K-SORT
  syntax MetaKLabel    ::= Token{"dummy"} [onlyLabel, noAutoReject, regex(`[^`\s]+`)]
                     //| "`" MetaKLabel "`"  [bracket]
  syntax KConstant ::= "#token"  "("KString "," KString ")"
                     | "#klabel" "(" MetaKLabel ")"
  syntax MetaKItem ::= KConstant
                     | MetaKLabel "(" MetaKList ")"
  syntax MetaKList     ::= NeList{MetaK,","}
                     | ".::KList"
  // we need to say something like List{K}{","}{"."}
  syntax MetaK         ::= NeList{MetaKItem,"~>"}
                     | ".::K"
  // we need to say List{MetaKItem}{"~>"}{"."}

  syntax KVariable ::= Token{"dummy"} [onlyLabel, noAutoReject, regex((?<![A-Za-z0-9\_])(\$|\!|\?)?([A-Z][A-Za-z0-9']*|_))]
  syntax MetaKItem ::= KVariable
                     | MetaKItem ":" KSort  // KItem must obey its sort at runtime
                                     // sorting arbitrary KItems; Sort can also be
                                     // K, KItem, KToken, but not KList
  syntax MetaK ::= MetaK "=>" MetaK       [onlyLabel]
                 | MetaK "requires" MetaK [onlyLabel]
                 | MetaK "ensures"  MetaK [onlyLabel]
                 | "`" MetaK "`"  [bracket]
endmodule


module TEST
  imports OUTER
  imports KAST
  imports K-PARSER
  
  //configuration <k> $PGM:KDefinition </k>
  syntax KAny ::= Token{"dummy"} [onlyLabel, noAutoReject, regex(.*)]
  
  //rule #token(S:String,, "KAny")   => #parseInModule(S, "KDefinition", "OUTER-WITH-BUBBLES")
  rule #token(S:String,, "Bubble") => #parseInModule(S, "K", "KAST") [anywhere]
endmodule
