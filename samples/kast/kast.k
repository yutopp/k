// Copyright (c) 2012-2014 K Team. All Rights Reserved.

module OUTER-SYNTAX

  syntax Keyword ::= "rule" | "syntax" | ...

  syntax BubbleItem ::= Token{[^`\s]\S+} \ Keyword
                      | Token{`[^`]+`}
                      | Token{``([^`]([^`]|`[^`])*)``}
                      | Token{```([^`]([^`]|`[^`]|``[^`])*)```}
                      // can add more if we want to

  syntax Sentence ::= Keyword Bubble
endmodule

module KAST-SYNTAX
  imports OUTER-SYNTAX
endmodule

module KAST
  imports KAST-SYNTAX
endmodule
