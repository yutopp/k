// Copyright (c) 2012-2014 K Team. All Rights Reserved.

module OUTER-SYNTAX

  syntax Bubble      ::= BubbleItem1 BubbleTail
                       | "`" Token{~[\`]+} "`"
                       | "``" Token{~[\`] (~[\`] | [\`]~[\`])*} "``"

  syntax BubbleTail  ::= List{BubbleItem2}{"\s"}  \\ white-space-separated non-empty list of BubbleItems
  syntax BubbleItem1 ::= Token{~[\s\`] ~[\s]*}     \\ one ore more non-white-space characters
                       | ~"rule"
  syntax BubbleItem2 ::= Token{~[\s]+} \ Keyword   \\ one ore more non-white-space, non-Keyword characters

  syntax Keyword ::= "rule" | "syntax" | ...

  syntax Sentence ::= Keyword Bubble
endmodule

module KAST-SYNTAX
  imports OUTER-SYNTAX
endmodule

module KAST
  imports KAST-SYNTAX
endmodule


syntax S1 ::= Token{...}  // regex1
syntax S2 ::= Token{...}  // regex2
syntax S3 ::= S1 | S2     
syntax S4 ::= S1 \ S2     // regex4
