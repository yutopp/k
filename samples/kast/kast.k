// Copyright (c) 2014 K Team. All Rights Reserved.

module OUTER-SYNTAX

  // Top level module stuff
  syntax Definition ::= Requires Modules
  syntax Requires   ::= List{Require,""}
  syntax Require    ::= "require" STRING
  syntax Modules    ::= List{Module,""}
  syntax Module     ::= "module" MODULENAME Imports Sentences "endmodule"
  syntax Imports    ::= List{Import,""}
  syntax Import     ::= "imports" MODULENAME
  syntax MODULENAME ::= Token{"dummy"} [regex("[A-Z\\-]+")]
  syntax Sentences  ::= List{Sentence,""}

  // Bubbles (rule, context, configuration)
  syntax Keyword ::= "rule"
                   | "syntax"
                   | "configuration"
                   | "endmodule"
                   | "context"

  syntax Bubble ::= List{BubbleItem,""}
  syntax BubbleItem ::= Token{"dummy"} [regex([^`\s]\S*)] //\ Keyword
                      | Token{"dummy"} [regex([\`]{1}([^\`])+[\`]{1})]
                      | Token{"dummy"} [regex([\`]{2}([^\`]|[\`][^\`])+[\`]{2})]
                      | Token{"dummy"} [regex([\`]{3}([^\`]|[\`][^\`]|[\`][\`][^\`])+[\`]{3})]
                      // can add more if we want to

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
  syntax ProductionList   ::= NeList{Production,"|"}
  syntax Production       ::= SimpleProduction | AnnotatedProduction
  syntax SimpleProduction ::= NeList{ProductionItem,""}
                            | TAG "(" SORTIDs ")"
  syntax AnnotatedProduction ::= SimpleProduction "[" AnnotationList "]"

  syntax ProductionItem ::= SORTID  // non-terminal
                          | STRING  // terminals
                          | "Token{" TOKEN "}" // will see
                          | "List{" SORTID "," STRING "}"
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
  syntax SORTIDs    ::= NeList{SORTID,","}

endmodule

module KAST-SYNTAX
  imports OUTER-SYNTAX
endmodule

module KAST
  imports KAST-SYNTAX
  configuration <k> $PGM:Definition </k>
endmodule
