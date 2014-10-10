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

// Known issue: The attributes do not yet parse as K terms

// Recall that in KORE productions must have one of the attributes klabel, token, bracket


// Module defining only the sorts K and KString, useful for modularity
module BASIC-K
  syntax K   //[...]   // no constructors at this stage
  syntax KString ::= r"[\\\"]([^\\\"\\n\\r\\\\]|[\\\\][nrtf\\\"\\\\])*[\\\"]"   [token, noAutoReject, onlyLabel, hook(org.kframework.kore.KString)]
         // optionally qualified strings, like in Scala "abc", i"abc", r"a*bc", etc.
endmodule



// To be used when parsing programs in KAST format
// (e.g., programs parsed and then output by an external parser)
module KAST  
  imports BASIC-K
  syntax K     ::= KItem                                 

  syntax KItem  ::= "#token" "(" KString "," KString ")"  [ klabel(#KToken), hook(org.kframework.kore.KToken) ]
  // @cos proposes to eliminate the next injection, and instead to allow
  // the use of meta-level KAST in definitions: #KApply(#token("_+_","KLabel"),#KList(1,3))
  // @grosu is skeptic: that would imply to allow KAST syntax in programs; not worth the price
  syntax KItem ::= "#klabel" "(" KLabel ")"              [ klabel(#WrappedKLabel), hook(org.kframework.kore.meta.WrappedKLabel) ]
  syntax KItem ::= KLabel "(" KList ")"                  [ klabel(#KApply), hook(org.kframework.kore.KApply) ]

  syntax KLabel ::= r"`(\\\\`|\\\\\\\\|[^`\\\\\\s])+`"   [ klabel(#KLabel), token ] // everything except `, unless escaped \`
  syntax KLabel ::= r"[#a-z][a-zA-Z0-9]*"                [ klabel(#KLabel), token, hook(org.kframework.kore.KLabel) ] // something that doesn't collide with meta-variables
// KLabel brackets not needed if @cos' approach above works
//  syntax KLabel ::= "`" KLabel "`"   [ bracket, hook ]

  syntax KList ::= K                
  syntax KList ::= ".::KList"       [klabel(#EmptyKList), hook(org.kframework.kore.EmptyKList) ]
  syntax KList ::= KList "," KList  [klabel(#KList), left, assoc, unit(#EmptyKList), hook(org.kframework.kore.KList) ]
endmodule



// To be used when pretty-printing stuck computations
module KSEQ
  imports KAST
  syntax K     ::= ".::K"                                [ klabel(#EmptyK), hook(org.kframework.kore.EmptyK) ] 
  syntax K     ::= K "~>" K                              [ klabel(#KSequence), left, assoc, unit(#EmptyK), hook(org.kframework.kore.KSequence) ]
endmodule



// To be used to parse semantic rules
module K
  imports KSEQ
  syntax KVariable ::= r"(?<![A-Za-z0-9\\_])(\\$|\\!|\\?)?([A-Z][A-Za-z0-9']*|_)" [ hook(org.kframework.kore.KVariable) ] // same like KSort?
  syntax KItem     ::= KVariable        
  syntax KLabel    ::= KVariable        
  syntax K         ::= K "=>" K         [ klabel(#KRewrite), hook(org.kframework.kore.KRewrite) ]
  // K bracket needed, e.g., Cxt[V ~> `env(E) => .`]
  syntax K         ::= "`" K "`"        [ bracket ] 

// Sort membership not needed anymore, as we can use requires/ensures
//  syntax KItem     ::= KItem ":" KSort  [ hook ]
endmodule



// To be used for first-level parsing of global KORE definitions,
// where the K terms are put in bubbles.  A similar, but larger
// module can be defined for global KAST definitions.
module OUTER-KORE 
  imports BASIC-K

  syntax KDefinition   ::= KRequireList KModuleList   [ klabel(#KDefinition), hook(org.kframework.kore.outer.Definition) ]

  syntax KRequire      ::= "require" KString          [ klabel(#KRequire), hook(org.kframework.kore.outer.Require) ]
  // Radu: These list declarations are a bit tricky (no separator, and the empty element is null).
  // Grigore wants to have a semantic declaration of the type L ::= L L [assoc]
  // but when the list can be nullable (has the production ""), then this goes into an infinite cycle.
  // My fix was to declare the list as L ::= LItem L | ""
  // Grigore is right in saying that it looks weird when we want to match somewhere in the middle.
  // Another format to take into consideration would be the L ::= List{LItem,""}, but this complicates KORE, and we would
  // like to keep this at a minimum. So are associative lists something different enough to deserve special treatment?
  syntax KRequireList  ::= KRequire KRequireList      [ klabel(#KRequireList), assoc, comm, unit(#emptyKRequireList) ]
  syntax KRequireList  ::= ""                         [ klabel(#emptyKRequireList), onlyLabel]

  syntax KModule       ::= "module" KModuleName KAttributesDeclaration KImportList KSentenceList "endmodule" [ klabel(#KModule), hook(org.kframework.kore.outer.Module) ]
  syntax KModuleList   ::= KModule KModuleList        														 [ klabel(#KModuleList), assoc, comm, unit(#emptyKModuleList) ]
  syntax KModuleList   ::= ""                         														 [ klabel(#emptyKModuleList), onlyLabel]

  syntax KImport       ::= "imports" KModuleName    [ klabel(#KImport), hook(org.kframework.kore.outer.Import) ]
  syntax KImportList   ::= KImport KImportList      [ klabel(#KImportList), assoc, comm, unit(#emptyKImportList) ]
  syntax KImportList   ::= ""                       [ klabel(#emptyKImportList), onlyLabel]

  syntax KSentenceList ::= KSentence KSentenceList      [ klabel(#KSentenceList), assoc, comm, unit(#kemptyKSentenceList) ]
  syntax KSentenceList ::= ""                           [ klabel(#emptyKSentenceList), onlyLabel]

  syntax KSentence ::= "syntax" KSort                             KAttributesDeclaration   [ klabel(#KSyntaxSort), hook(org.kframework.kore.outer.SyntaxSort) ]
  syntax KSentence ::= "syntax" KSort "::=" KProduction           KAttributesDeclaration   [ klabel(#KSyntaxProduction), hook(org.kframework.kore.outer.SyntaxProduction) ] ]
  syntax KSentence ::= "syntax" "priority" KString ">" KString    KAttributesDeclaration   [ klabel(#KSyntaxPriority), hook(org.kframework.kore.outer.SyntaxPriority) ]  
  
  syntax KSentence ::= "configuration" K "requires" K             KAttributesDeclaration   [ klabel(#KConfiguration), hook(org.kframework.kore.outer.Configuration) ]
  syntax KSentence ::= "rule"          K "requires" K "ensures" K KAttributesDeclaration   [ klabel(#KRule), hook(org.kframework.kore.outer.Rule) ]
  syntax KSentence ::= "context"       K "requires" K             KAttributesDeclaration   [ klabel(#KContext), hook(org.kframework.kore.outer.Context) ]

  syntax KProduction ::= ""                      [ klabel(#emptyKProduction), onlyLabel]
  syntax KProduction ::= KProductionItem KProduction [ klabel(#KProduction), assoc, unit(#emptyKProduction) ]
  syntax KProductionItem ::= KSort               [ hook(org.kframework.kore.outer.NonTerminal) ] // non-terminal
  syntax KProductionItem ::= KString             [ hook(org.kframework.kore.outer.Terminal) ]    // terminals, including regexes
  syntax KProductionItem ::= "r" KString         [ hook(org.kframework.kore.outer.RegexTerminal) ]    // terminals, including regexes

  // We are going to use #KAttributes as top KLabel in the K term holding the attributes
  syntax KAttributesDeclaration ::= "[" K "]"   [ klabel(#KAttributesDeclaration), hook(org.kframework.kore.KAttributes) ]

  // The following can still change
  syntax KModuleName ::= r"[A-Z][A-Z\\-]*"     [ token ]
  syntax KSort       ::= r"[A-Z][A-Za-z0-9]*"  [ token, hook(org.kframework.kore.outer.Sort) ]
endmodule



module KORE
  imports OUTER-KORE
  imports K
  configuration `<k>`($PGM)  // how do I put the sort restriction for this

endmodule
