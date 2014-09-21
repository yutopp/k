// Copyright (c) 2014 K Team. All Rights Reserved.

/*
This defines a core subset of K and KAST, called KORE, with the property
that any KAST definition, and thus any K definition, can be mechanically
translated to a definition using only this minimal subset.  An additional
property of KORE is that we do not want to lose anything from the original
definition.  For example, we do not want to lose the capability to pretty
print terms based on the original syntax, or the capability to report error
messages referring to constructs in the original definition.

We selected this core of K based on the following two criteria:

(1) We want to support everything that KORE contains

(2) Everything we support can be easily expressed in KORE

(3) It is minimal with properties (1) and (2)
*/

// Everything is Meta- at this level, so I dropped the Meta everywhere

module BASIC-K
  syntax K   [...]
  syntax KList ::= K   [...]
  syntax KList ::= ".::KList"   [klabel(emptyKList), ...]
  syntax KList ::= KList "," KList   [assoc, unit(emptyKList), ...]
endmodule


module OUTER-KORE
  import BASIC-K

  syntax KDefinition   ::= KRequireList KModuleList   [...]

  syntax KRequireList  ::= KRequire   [...]
  syntax KRequireList  ::= ""   [klabel(emptyKRequireList), ...]
  syntax KRequireList  ::= KRequireList KRequireList   [assoc, unit(emptyKRequireList), ...]
  syntax KRequire      ::= "require" KString   [...]

  syntax KModuleList   ::= KModule   [...]
  syntax KModuleList   ::= ""   [klabel(emptyKModuleList), ...]
  syntax KModuleList   ::= KModuleList KModuleList   [assoc, unit(emptyKModuleList), ...]
  syntax KModule       ::= "module" KModuleName KImportList KSentenceList "endmodule"   [...]

  syntax KImportList   ::= KImport   [...]
  syntax KImportList   ::= ""   [klabel(emptyKImportList), ...]
  syntax KImportList   ::= KImportList KImportList   [assoc, unit(emptyKImportList), ...]
  syntax KImport       ::= "import" KModuleName   [...]

  syntax KSentenceList ::= KSentence   [...]
  syntax KSentenceList ::= ""      [klabel(emptyKSentenceList), ...]
  syntax KSentenceList ::= KSentenceList KSentenceList   [assoc, unit(emptyKSentenceList), ...]

  syntax KSentence ::= "syntax" KSort                         KAttributes   [...]
  syntax KSentence ::= "syntax" KSort "::=" KProduction       KAttributes   [...]
  syntax KSentence ::= "configuration" K                      KAttributes   [...]
  syntax KSentence ::= "rule"          K requires K ensures K KAttributes   [...]
  syntax KSentence ::= "context"       K requires K           KAttributes   [...]

  syntax KProduction ::= KProductionItem   [...]
  syntax KProduction ::= ""   [klabel(emptyProduction), ...]
  syntax KProduction ::= KProduction KProduction [assoc, unit(emptyKProduction), ...]
  syntax KProductionItem ::= KSort   [...] // non-terminal
  syntax KProductionItem ::= KString   [...] // terminals, including regexes

  syntax KAttributes ::= "[" KList "]"   [...]

  // The following can still change
  syntax KModuleName ::= r"[A-Z][A-Z\-\_]*"   [token, ...]
  syntax KSort ::= r"[A-Z][A-Za-z0-9]*"   [token, ...]
  syntax KString ::= ...Radu...   [token, ...]
         // optionally qualified strings, like in Scala "abc", i"abc", r"a*bc", etc.
endmodule

module INNER-KAST
  import BASIC-K

  syntax KLabel    ::= r"([^`]|\+`)*"  [token, ...] // everything except `, unless escaped \`
  syntax KLabel    ::= "`" KLabel "`"   [bracket, ...]
    // prefer optional brackets for labels, because we encode attributes as K terms, too

  syntax KConstant ::= "#token" "(" KString "," KString ")"   [...]
  syntax KConstant ::= "#klabel" "(" KLabel ")"   [...]
  syntax KItem     ::= KConstant   [...]
  syntax KItem     ::= KLabel "(" KList ")"   [...]
  syntax K         ::= KItem   [...]
  syntax K         ::= ".::K"   [klabel(emptyK)]
  syntax K         ::= K "~>" K  [assoc, unit(emptyK)]

  syntax KVariable ::= ... // same like KSort?
  syntax KItem     ::= KVariable   [...]
  syntax KItem     ::= KItem ":" KSort   [...]
  syntax K         ::= K "=>" K   [...]
  syntax K         ::= "`" K "`"  [bracket, ...] // needed, e.g., <k> V ~> `env(E) => .` ...</k>
endmodule


module KORE
  import OUTER-KORE
  import INNER-KAST
...
endmodule
