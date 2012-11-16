// $K_BASE/core/kompile -lint js

require "js-syntax.k"
require "js-configuration.k"
require "js-basic-semantics.k"
require "js-ssemantics.k"
// require ../json/json
// require js-json-ast-ids
// require json2prefix
require "js-programs.k"

// K				4 ~> 5 ~> 6
// List{K}			3 ,, 5 ,, 6
// List			ListItem(3) ListItem(5) ListItem(5)
// Set				SetItem(1) SetItem(1) SetItem(1)
// Bag				BagItem(1) BagItem(1) BagItem(1)
// Map 	|->
// Bag		<bsg> </bsg>  -- config style bag

module JS-INIT-JS 
  imports JS-SSEMANTICS
  
	syntax K ::= "initJS"
			   | initGlobalLexEnv( K ) [strict]  // Ask Traian
			   | "initUndefined"

	rule initJS => initGlobalLexEnv(allocNewObj) ~> initUndefined
		 [initJS, structural]

	rule <k> initGlobalLexEnv(Obj:V) => . ...</k> 
		 <lexEnv> .None => $env(.None, Obj) </lexEnv> 
		 <globalEnv> .None => Obj </globalEnv>
		 [initGlobalLexEnv, structural]
	
	rule <k> initUndefined => setPropertyValue(Obj, "undefined", $undefined) ~> discard ...</k>
		 <globalEnv> Obj:V </globalEnv>
		 [initUndefined, structural]

endmodule


module JS 
  imports JS-INIT-JS
  imports JS-PROGRAMS
	// syntax Bag ::= go( K )
	// syntax Bag ::= run( KLabel )
	// 
	// rule go(Program:K) => <T>... <k> initJS ~> Program </k> ...</T> 
	// 
	// rule [run-program]:
	// 	run (L:KLabel) => <T>... <k> initJS ~> L(.List{K}) </k> ...</T>  
	// 	[structural]
	
	// rule [start-js]:
	// 	<code> Code:K => . </code>
	// 	<k> . => initJS ~> Code </k>
	
	rule startJS(Program:K) => initJS ~> Program  [start-js]
	
	rule <k> Result:V => . </k> 
		 <out> _ => ListItem(stringValue(toString(Result))) </out>  [final-output]
		 //ListItem in out cell!
	
	  // rule <T>... <threads> .Bag </threads> <out> S:String </out> ...</T> 
	  //   => <result> S </result>
	
	
  	// macro pgm = [['program1]]
endmodule