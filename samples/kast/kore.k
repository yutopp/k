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
  The "hook" attribute has the follosing semantics:
  1. Hooks are relative to the hookNamespace module attribute.
  2. "hook(Foo)" will hook to the constructor named Foo with the arguments being 
     the non-terminals of the production.
  3. "hook" is shorthand for hooking to a constructor which has the same name as 
     the sort of the current production.
*/

module BASIC-K
  syntax K   [...]
  syntax KList ::= K   [...]
  syntax KList ::= ".::KList"   [klabel(emptyKList), ...]
  syntax KList ::= KList "," KList   [assoc, unit(emptyKList), ...]
endmodule


module OUTER-KORE [ hookNamespace(org.kframework.kore.outer) ]
  import BASIC-K

  syntax KDefinition   ::= KRequireList KModuleList   [ hook ]

  syntax KRequireList  ::= KRequire                   [ ... ]
  syntax KRequireList  ::= ""                         [ klabel(emptyKRequireList), ... ]
  syntax KRequireList  ::= KRequireList KRequireList  [ assoc, com, unit(emptyKRequireList) ]
  syntax KRequire      ::= "require" KString          [ hook ]

  syntax KModuleList   ::= KModule                    [ ... ]
  syntax KModuleList   ::= ""                         [ klabel(emptyKModuleList), ... ]
  syntax KModuleList   ::= KModuleList KModuleList    [ assoc, com, unit(emptyKModuleList) ]
  syntax KModule       ::= "module" KModuleName KImportList KSentenceList "endmodule"   [ hook ]

  syntax KImportList   ::= KImport                  [ ... ]
  syntax KImportList   ::= ""                       [ klabel(emptyKImportList), ... ]
  syntax KImportList   ::= KImportList KImportList  [ assoc, com, unit(emptyKImportList), ... ]
  syntax KImport       ::= "import" KModuleName     [ hook ]

  syntax KSentenceList ::= KSentence                    [ ... ]
  syntax KSentenceList ::= ""                           [ klabel(emptyKSentenceList), ... ]
  syntax KSentenceList ::= KSentenceList KSentenceList  [ assoc, com, unit(emptyKSentenceList), ... ]

  syntax KSentence ::= "syntax" KSort                         KAttributes   [ hook(Syntax) ]
  syntax KSentence ::= "syntax" KSort "::=" KProduction       KAttributes   [ hook(Syntax) ]
  syntax KSentence ::= "configuration" K                      KAttributes   [ hook(Configuration) ]
  syntax KSentence ::= "rule"          K requires K ensures K KAttributes   [ hook(Rule) ]
  syntax KSentence ::= "context"       K requires K           KAttributes   [ hook(Context) ]

  syntax KProduction ::= KProductionItem   [...]
  syntax KProduction ::= ""   [klabel(emptyProduction), ...]
  syntax KProduction ::= KProduction KProduction [assoc, unit(emptyKProduction), ...]
  syntax KProductionItem ::= KSort    [ hook(NonTerminal) ] // non-terminal
  syntax KProductionItem ::= KString  [ hook(Terminal) ]    // terminals, including regexes

  syntax KAttributes ::= "[" KList "]"   [ hook(KAttributes) ]

  // The following can still change
  syntax KModuleName ::= r"[A-Z][A-Z\-\_]*"   [token, ...]
  syntax KSort ::= r"[A-Z][A-Za-z0-9]*"     [ token, hook ]
  syntax KString ::= ...Radu...   [token, ...]
         // optionally qualified strings, like in Scala "abc", i"abc", r"a*bc", etc.
endmodule

module INNER-KAST [ hookNamespace(org.kframework.kore) ]
  import BASIC-K

  syntax KLabel    ::= r"([^`]|\+`)*"   [ token, hook ] // everything except `, unless escaped \`
  syntax KLabel    ::= "`" KLabel "`"   [ bracket, hook ]
    // prefer optional brackets for labels, because we encode attributes as K terms, too

  syntax KConstant ::= "#token" "(" KString "," KString ")" [ hook ]
  syntax KConstant ::= "#klabel" "(" KLabel ")"             [ hook ]
  syntax KItem     ::= KConstant                            [ ... ]
  syntax KItem     ::= KLabel "(" KList ")"                 [ hook ]
  syntax K         ::= KItem                                [ ... ]
  syntax K         ::= ".::K"                               [ klabel(emptyK) ]
  syntax K         ::= K "~>" K                             [ assoc, unit(emptyK) ]

  syntax KVariable ::= ...              [ hook ] // same like KSort?
  syntax KItem     ::= KVariable        [ ... ]
  syntax KItem     ::= KItem ":" KSort  [ hook ]
  syntax K         ::= K "=>" K         [ hook(Rewrite) ]
  syntax K         ::= "`" K "`"        [ bracket, ... ] // needed, e.g., <k> V ~> `env(E) => .` ...</k>
endmodule


module KORE
  import OUTER-KORE
  import INNER-KAST
...
endmodule
