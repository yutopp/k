// Copyright (c) 2014 K Team. All Rights Reserved.

module OUTER-SYNTAX

  // Top level module stuff
  syntax Start        ::= RequireList ModuleList
  syntax RequireList  ::= List{Require,""}
  syntax Require      ::= "require" STRING
  syntax ModuleList   ::= List{Module,""}
  syntax Module       ::= "module" MODULENAME ImportList SentenceList "endmodule"
  syntax ImportList   ::= List{Import,""}
  syntax Import       ::= "imports" MODULENAME
  syntax MODULENAME   ::= Token{"dummy"} [regex("[A-Z\\-]+")]
  syntax SentenceList ::= List{Sentence,""}

  // Bubbles (rule, context, configuration)
  syntax Keyword ::= "rule"
                   | "syntax"
                   | "configuration"
                   | "endmodule"
                   | "context"

  syntax Bubble ::= BubbleHead BubbleTail
                  | Token{"dummy"} [regex([\`]{1}([^\`])+[\`]{1})]
                  | Token{"dummy"} [regex([\`]{2}([^\`]|[\`][^\`])+[\`]{2})]
                  | Token{"dummy"} [regex([\`]{3}([^\`]|[\`][^\`]|[\`][\`][^\`])+[\`]{3})]
                  // can add more if we want to
  syntax BubbleHead     ::= Token{"dummy"} [regex([^`\s]\S*)] //\ Keyword
  syntax BubbleTail     ::= List{BubbleTailItem,""}
  syntax BubbleTailItem ::= Token{"dummy"} [regex(\S+)]//\ Keyword

  syntax Sentence ::= "rule"          Bubble
                    | "configuration" Bubble
                    | "context"       Bubble
					| Syntax

  // Syntax declarations
  syntax Syntax ::= "syntax" SORTID "::=" PriorityBlockList
  syntax Syntax ::= "syntax" SORTID
  syntax Syntax ::= "syntax" SORTID "[" AnnotationList "]"
  syntax PriorityBlockList ::= NeList{PriorityBlock,">"}
  syntax PriorityBlock ::=              ProductionList
                         | "left:"      ProductionList
                         | "right:"     ProductionList
                         | "non-assoc:" ProductionList
  syntax ProductionList ::= NeList{Production,"|"}
  syntax Production     ::= NoAnnoProd | AnnotatedProd
  syntax NoAnnoProd     ::= NeList{ProductionItem,""}
                          | TAG "(" SortIdList ")"
  syntax AnnotatedProd  ::= NoAnnoProd "[" AnnotationList "]"
  syntax SortIdList     ::= NeList{SORTID,","}

  syntax ProductionItem ::= SORTID  // non-terminal
                          | STRING  // terminals
                          | "Token{" TOKEN "}" // will see
                          |   "List{" SORTID "," STRING "}"
                          | "NeList{" SORTID "," STRING "}"

  syntax AnnotationList ::= NeList{Annotation,","}
  syntax Annotation ::= TAG
                      | TAG "(" TAGCONTENT ")"
                      | TAG "(" STRING ")"
  syntax TAG        ::= Token{"dummy"} [regex([a-z][A-Za-z\-0-9]*)]
  syntax TAGCONTENT ::= List{TAGC,""}  // anything with balanced parenthesis. Will have to get original string.
  syntax TAGC       ::= Token{"dummy"} [regex("[^\\n\\r\\(\\)\\\"]+")]
  syntax TAGC       ::= "(" TAGCONTENT ")"
  syntax STRING     ::= Token{"dummy"} [regex("[\\\"](([^\\\"\\n\\r\\\\])|([\\\\][nrtf\\\"\\\\])|)*[\\\"]")]
  syntax TOKEN      ::= Token{"dummy"} [regex(([^\}\n\r]|[\\][\}])+)]
  syntax SORTID     ::= Token{"dummy"} [regex([#]?[A-Z][A-Za-z0-9]*)]

endmodule

module KAST-SYNTAX
  imports OUTER-SYNTAX
endmodule

module KAST
  imports KAST-SYNTAX
  configuration <k> $PGM:Start </k>
endmodule
