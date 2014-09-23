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

/*
  The "hook" attribute has the following semantics:
  1. Hooks are relative to the hookNamespace module attribute.
  2. "hook(Foo)" will hook to the constructor named Foo with the arguments being 
     the non-terminals of the production.
  3. "hook" is shorthand for hooking to a constructor which has the same name as 
     the sort of the current production.
*/

// Known issue: The attributes do not yet parse as KList or K terms

// Recall that in KORE productions must have one of the attributes klabel, token, bracket



// Module defining only the sorts K and KString, useful for modularity
module BASIC-K
  syntax K   [...]   // no constructors at this stage
  syntax KString ::= ...Radu...   [token, ...]
         // optionally qualified strings, like in Scala "abc", i"abc", r"a*bc", etc.
endmodule



// Defining the KList sort, useful for defining both KAST terms
// and attributes
module KLIST
  import BASIC-K
  syntax KList ::= K                [...]
  syntax KList ::= ".::KList"       [klabel(#emptyKList), ...]
  syntax KList ::= KList "," KList  [klabel(#KList), assoc, unit(#emptyKList), ...]
endmodule



// To be used when parsing programs in KAST format
// (e.g., programs parsed and then output by an external parser)
module KAST [ hookNamespace(org.kframework.kore) ]  // the hook will probably change
  import KLIST
  syntax KLabel ::= r"([^`]|\+`)*"                        [ token, hook ] // everything except `, unless escaped \`
// KLabel brackets not needed if @cos' approach below works
  syntax KLabel    ::= "`" KLabel "`"   [ bracket, hook ]

  syntax KItem  ::= "#token" "(" KString "," KString ")"  [ klabel(#KToken), hook ]
  // @cos proposes to eliminate the next injection, and instead to allow
  // the use of meta-level KAST in definitions: #KApply(#token("_+_","KLabel"),#KList(1,3))
  // @grosu is skeptic: that would imply to allow KAST syntax in programs; not worth the price
  syntax KItem  ::= "#klabel" "(" KLabel ")"              [ klabel(#KLabel), hook ]
  syntax KItem  ::= KLabel "(" KList ")"                  [ klabel(#KApply), hook ]
  syntax K      ::= KItem                                 [ ... ]
endmodule



// To be used when pretty-printing stuck computations
module KSEQ
  import BASIC-K
  syntax K     ::= ".::K"                                [ klabel(#emptyK) ]
  syntax K     ::= K "~>" K                              [ klabel(#KSequence), assoc, unit(#emptyK) ]
endmodule



// To be used to parse semantic rules
module K
  import KAST
  import KSEQ
  syntax KVariable ::= ...              [ hook ] // same like KSort?
  syntax KItem     ::= KVariable        [ ... ]
  syntax K         ::= K "=>" K         [ klabel(#KRewrite), hook(Rewrite) ]
  // K bracket needed, e.g., Cxt[V ~> `env(E) => .`]
  syntax K         ::= "`" K "`"        [ bracket, ... ]

// Sort membership not needed anymore, as we can use requires/ensures
//  syntax KItem     ::= KItem ":" KSort  [ hook ]
endmodule



// To be used for first-level parsing of global KORE definitions,
// where the K terms are put in bubbles.  A similar, but larger
// module can be defined for global KAST definitions.
module OUTER-KORE [ hookNamespace(org.kframework.kore.outer) ]
  import KLIST  // @cos see below

  syntax KDefinition   ::= KRequireList KModuleList   [ hook ]

  syntax KRequireList  ::= KRequire                   [ ... ]
  syntax KRequireList  ::= ""                         [ klabel(#emptyKRequireList), ... ]
  syntax KRequireList  ::= KRequireList KRequireList  [ klabel(#KRequireList), assoc, comm, unit(#emptyKRequireList) ]
  syntax KRequire      ::= "require" KString          [ klabel(#KRequire), hook ]

  syntax KModuleList   ::= KModule                    [ ... ]
  syntax KModuleList   ::= ""                         [ klabel(#emptyKModuleList), ... ]
  syntax KModuleList   ::= KModuleList KModuleList    [ klabel(#KModuleList), assoc, comm, unit(#emptyKModuleList) ]
  syntax KModule       ::= "module" KModuleName KImportList KSentenceList "endmodule"  [ klabel(#KModule), hook ]

  syntax KImportList   ::= KImport                  [ ... ]
  syntax KImportList   ::= ""                       [ klabel(#emptyKImportList), ... ]
  syntax KImportList   ::= KImportList KImportList  [ klabel(#KImportList), assoc, comm, unit(#emptyKImportList), ... ]
  syntax KImport       ::= "import" KModuleName     [ klabel(#KImport), hook ]

  syntax KSentenceList ::= KSentence                    [ ... ]
  syntax KSentenceList ::= ""                           [ klabel(#emptyKSentenceList), ... ]
  syntax KSentenceList ::= KSentenceList KSentenceList  [ klabel(#KSentenceList), assoc, comm, unit(#kemptyKSentenceList), ... ]

  syntax KSentence ::= "syntax" KSort                         KAttributes   [ klabel(#KSentenceSyntaxSort), hook(Syntax) ]
  syntax KSentence ::= "syntax" KSort "::=" KProduction       KAttributes   [ klabel(#KSentenceSyntaxProduction), hook(Syntax) ]
  syntax KSentence ::= "configuration" K                      KAttributes   [ klabel(#KSentenceConfiguration), hook(Configuration) ]
  syntax KSentence ::= "rule"          K requires K ensures K KAttributes   [ klabel(#KSentenceRule), hook(Rule) ]
  syntax KSentence ::= "context"       K requires K           KAttributes   [ klabel(#KSentenceContext), hook(Context) ]

  syntax KProduction ::= KProductionItem         [ ... ]
  syntax KProduction ::= ""                      [ klabel(#emptyKProduction), ...]
  syntax KProduction ::= KProduction KProduction [ klabel(#KProduction), assoc, unit(#emptyKProduction), ...]
  syntax KProductionItem ::= KSort               [ hook(NonTerminal) ] // non-terminal
  syntax KProductionItem ::= KString             [ hook(Terminal) ]    // terminals, including regexes

  // @cos prefers K instead of KList below.  In that case, we need to define
  // "_,_" as as assoc/comm construct of KAttribute contents
  syntax KAttributes ::= "[" KList "]"   [ klabel(#KAttributes), hook(KAttributes) ]

  // The following can still change
  syntax KModuleName ::= r"[A-Z][A-Z\-\_]*"  [token, ...]
  syntax KSort ::= r"[A-Z][A-Za-z0-9]*"      [ token, hook ]
endmodule



module KORE
  import OUTER-KORE
  import K
... // needs to parse in two stages: first OUTER-KORE, then the KAST bubbles
endmodule
