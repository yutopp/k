// Copyright (c) 2014 K Team. All Rights Reserved.

module OUTER-SYNTAX

  // Top level module stuff
  syntax Start        ::= RequireList ModuleList
  syntax RequireList  ::= List{Require,""}
  syntax Require      ::= "require" STRING
  syntax ModuleList   ::= List{Module,""}
  syntax Module       ::= "module" MODULENAME ImportList SentenceList "endmodule"
  syntax ImportList   ::= "import" MODULENAME
  syntax MODULENAME   ::= Token{[A-Z\-]+}
  syntax SentenceList ::= List{Sentence,""}
  
  // Bubbles (rule, context, configuration)
  syntax Keyword ::= "rule"
                   | "syntax"
				   | "configuration"
				   | "endmodule"
				   | "context"

  syntax Bubble ::= BubbleHead BubbleTail
                  | Token{`[^`]+`}
                  | Token{``([^`]([^`]|`[^`])*)``}
                  | Token{```([^`]([^`]|`[^`]|``[^`])*)```}
                  // can add more if we want to
  syntax BubbleHead     ::= Token{[^`\s]\S*} \ Keyword
  syntax BubbleTail     ::= List{BubbleTailItem,""}
  syntax BubbleTailItem ::= Token{\S+} \ Keyword

  syntax Sentence ::= "rule"          Bubble
                    | "configuration" Bubble
					| "context"       Bubble
					
  // Syntax declarations
  syntax Syntax ::= "syntax" SORTID "::=" PriorityBlockList
  syntax Syntax ::= "syntax" SORTID
  syntax Syntax ::= "syntax" SORTID "[" AnnotationList "]
  syntax PriorityBlockList ::= NeList{PriorityBlock,">"}
  syntax PriorityBlock ::=              ProductionList
                         | "left:"      ProductionList
						 | "right:"     ProductionList
						 | "non-assoc:" ProductionList
  syntax ProductionList ::= NeList{Production,"|"}
  syntax Production     ::= NeList{ProductionItem,""}

  syntax ProductionItem ::= SORTID  // non-terminal
                          | STRING  // terminals
						  | "Token{" ... "}" // will see
						  |   "List{" SORTID "," STRING "}"
						  | "NeList{" SORTID "," STRING "}"
  
  syntax AnnotationList ::= NeList{Annotation,","}
  syntax Annotation ::= TAG
                      | TAG "(" TAGCONTENT ")"
                      | TAG "(" STRING ")"
  syntax TAG ::= Token{[a-z][A-Za-z\-0-9]*}
  syntax TAGCONTENT ::= Token{...} // anything with balanced parenthesis
  syntax STRING ::= Token{[\"](([^\"\n\r\\])|([\\][nrtf\"\\])|)*[\"]}

endmodule

module KAST-SYNTAX
  imports OUTER-SYNTAX
endmodule

module KAST
  imports KAST-SYNTAX
endmodule
