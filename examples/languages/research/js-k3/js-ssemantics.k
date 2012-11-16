// ../../../tools/kompile.pl -lint js


module JS-UNARY 
  imports JS-BASIC-SEMANTICS
  
	syntax V ::= not ( K )
	syntax V ::= neg ( K )

	rule not($b(B:Bool)) => $b(notBool(B))									[structural, anywhere]
	
	rule neg(Value:V) 		=> neg(toNumber(Value))	 when notNumber(Value)	[structural, anywhere]
	rule neg($f(F:Float)) 	=> $f(0.0 -Float F)								[structural, anywhere]
	rule neg($z(Sign:Int))	=> $z(0 -Int Sign)								[structural, anywhere]
	rule neg($inf(Sign)) 	=> $inf(0 -Int Sign)							[structural, anywhere]
	rule neg($nan) 			=> $nan											[structural, anywhere]
	
	// ECMA-262 11.4.6
	rule $plus(Value:V) => toNumber(Value)

	// ECMA-262 11.4.7
	rule $neg(Value) => neg(Value)
	
	// ECMA-262 11.4.8
	rule $not(Value) => not(toBoolean(Value))
	
endmodule

module JS-ARITHMETIC 
  imports JS-UNARY 

	syntax K ::= arith( String , K , K )	[strict]
	syntax K ::= newSign( Int , Int ) // Int
	syntax K ::= newInfinity( Int , Int ) 
	syntax K ::= newZero( Int , Int )
	syntax K ::= strAdd( K , K )			[strict]
	syntax K ::= add( K , K )
	
	rule newSign(Sign1:Int, Sign2:Int) 		=> Sign1 *Int Sign2				[newSign, structural]
	rule newInfinity(Sign1:Int, Sign2:Int) 	=> $inf(newSign(Sign1, Sign2))	[newInfinity, structural]
	rule newZero(Sign1:Int, Sign2:Int) 		=> $z(newSign(Sign1, Sign2))	[newZero, structural]
	
	// arithmetic expressions
	// TO DOS: Add mod
	rule $div(Left:V, Right:V) 	=> arith("div", getValue(Left), getValue(Right))  [div]
	rule $mul(Left:V, Right:V) 	=> arith("mul", getValue(Left), getValue(Right))  [mul]
	rule $sub(Left:V, Right:V) 	=> arith("sub", getValue(Left), getValue(Right))  [sub]
	rule $add(Left:V, Right:V) 	
			=> strAdd(toPrimitive(getValue(Left), "N"), toPrimitive(getValue(Right), "N"))  [add]


	// arithmetic with NaN
	rule arith(OpName:String, $nan, _)	=> $nan					[nan-num-arith, structural]
	rule arith(OpName:String, _, $nan)	=> $nan					[num-nan-arith, structural]


	// arithmetic conversions
	rule arith(OpName:String, Left:V, Right:V) 	=> arith(OpName, toNumber(Left), Right) 
	  when notNumber(Left)	 		[arith-coerce-left-to-num, structural]
	rule arith(OpName:String, Left:V, Right:V) 	=> arith(OpName, Left, toNumber(Right)) 
	  when notNumber(Right) 		[arith-coerce-right-to-num, structural]
	
	
	// arithmetic base cases
	rule arith("div", $f(F1:Float), $f(F2:Float)) => $n(F1 /Float F2)	[f-div, structural]
	rule arith("mod", $f(F1:Float), $f(F2:Float)) => $n(F1 %Float F2)	[f-mod, structural]
	rule arith("mul", $f(F1:Float), $f(F2:Float)) => $n(F1 *Float F2)	[f-mul, structural]
	// rule arith("sub", $f(F1:Float), $f(F2:Float)) => $n(F1 -Float F2)	[f-sub, structural]
	rule arith("add", $f(F1:Float), $f(F2:Float)) => $n(F1 +Float F2)	[f-add, structural]
	

	// ECMA-262 11.5.1
	rule arith("mul", $inf(Sign1:Int), $inf(Sign2:Int)) => newInfinity(Sign1, Sign2) [structural]
	rule arith("mul", $inf(Sign:Int), $f(F:Float))	=> newInfinity(Sign, sign(F))	[structural]
	rule arith("mul", $f(F:Float), $inf(Sign:Int))	=> newInfinity(sign(F), Sign)	[structural]
	rule arith("mul", $z(_), $inf(_)) 					=> $nan 					[structural]
	rule arith("mul", $inf(_), $z(_)) 					=> $nan 					[structural]
	rule arith("mul", $z(Sign:Int), $f(F:Float))		=> newZero(Sign, sign(F))	[structural]
	rule arith("mul", $f(F:Float), $z(Sign:Int))		=> newZero(sign(F), Sign)	[structural]
	rule arith("mul", $z(Sign1:Int), $z(Sign2:Int)) 	=> newZero(Sign1, Sign2)	[structural]
	
	
	// ECMA-262 11.5.2
	rule arith("div", $inf(_), $inf(_)) 				=> $nan 						[structural]
	rule arith("div", $inf(Sign:Int), $f(F:Float)) 		=> newInfinity(Sign, sign(F))	[structural]
	rule arith("div", $inf(Sign1:Int), $z(Sign2:Int))	=> newInfinity(Sign1, Sign2) 	[structural]
	rule arith("div", Num:V, $inf(Sign:Int)) 			=> newZero(sign(Num), Sign) 
														when isFinite(Num)				[structural]
	rule arith("div", $z(Sign:Int), $f(F:Float)) 		=> newZero(Sign, sign(F))		[structural]
	rule arith("div", $z(_), $z(_)) 					=> $nan							[structural]
	rule arith("div", $f(F:Float), $z(Sign:Int)) 		=> newInfinity(sign(F), Sign)	[structural]


	// ECMA-262 11.5.3
	rule arith("mod", $inf(_), Num:V) 		=> $nan		when notNan(Num)		[structural]
	rule arith("mod", Num:V, $z(_)) 		=> $nan		when notNan(Num)		[structural]
	rule arith("mod", Num:V, $inf(_)) 		=> Num 		when isFinite(Num) 		[structural]
	rule arith("mod", $z(Sign:Int), Num:V)  => $z(Sign)	when isFinite(Num) 		[structural]


	// ECMA-262 11.6.1
	rule strAdd($s(S1:String), $s(S2:String)) => $s(S1 +String S2)	 				[structural]
	rule strAdd($s(S1), Value:V) 	=> strAdd($s(S1), toString(Value))				[structural]
	rule strAdd(Value:V, $s(S2)) 	=> strAdd(toString(Value), $s(S2))				[structural]
	rule strAdd(Left:V, Right:V) 	=> arith("add", Left, Right) 
									when (notString(Left) andBool notString(Right))	[structural]
	
	// ECMA-262 11.6.2
	rule arith("sub", Left, Right) => arith("add", Left, neg(Right))				[structural]
	
	// ECMA-262 11.6.3
	rule arith("add", $inf(Sign:Int), $inf(Sign2:Int)) 
		=> #if Sign ==K Sign2 #then $inf(Sign) #else $nan #fi						[structural]
	rule arith("add", $inf(Sign:Int), Num:V) 	=> $inf(Sign)	when isFinite(Num)	[structural]
	rule arith("add", Num:V, $inf(Sign:Int)) 	=> $inf(Sign)	when isFinite(Num)	[structural]
	rule arith("add", $z(Sign), $z(Sign2)) 			
		=> #if Sign ==K Sign2 #then $z(Sign) #else $z(1) #fi						[structural]
	rule arith("add", $z(_), $f(F:Float)) 		=> $f(F)							[structural]
	rule arith("add", $f(F:Float), $z(_)) 		=> $f(F)							[structural]
	
	
	// add shortcut
	rule add(Left:V, Right:V) => arith("add", Left, Right)					[structural, anywhere]
	
endmodule

module JS-PREFIX-POSTFIX 
  imports JS-ARITHMETIC
  
	syntax K ::= postOp( V , K , Int )	[strict]
	syntax K ::= preOp( V , K , Int )	[strict]
	
	// ECMA-262 11.3.1 Postfix Increment Operator
	rule $inc(Target:V, true) => postOp(Target, toNumber(getValue(Target)), 1)
	
 	// ECMA-262 11.3.2 Postfix Decrement Operator
	rule $dec(Target:V, true) => postOp(Target, toNumber(getValue(Target)), -1)
	
	rule postOp(Target:V, PrevNum:V, I:Int) 
	=> putValue(Target, add(PrevNum, $n(I))) ~> discard ~> PrevNum					[structural]
	
	// ECMA-262 11.4.4 Prefix Increment Operator
	rule $inc(Target:V, false) => preOp(Target, toNumber(getValue(Target)), 1)
	
	// ECMA-262 11.4.5 Prefix Increment Operator
	rule $dec(Target:V, false) => preOp(Target, toNumber(getValue(Target)), -1)
	
	rule preOp(Target:V, Num:V, I:Int) => putValue(Target, add(Num, $n(I)))		[structural]
endmodule

module JS-PROPERTY-ACCESS 
  imports JS-PREFIX-POSTFIX
  
	syntax K ::= accessPropertyAt( K , K ) [seqstrict]
	
	rule $staticAccess(Target:V, $id(Name:String)) => accessPropertyAt(Target, $s(Name))  [staticAccess]
	
	rule $dynamicAccess(Target:V, PropNameValue:V) 
		=> accessPropertyAt(Target, toString(getValue(PropNameValue)))  [dynamicAccess]
	
	rule accessPropertyAt(Target:V, $s(Name:String)) 
		=> $binding(checkObjectCoercible(getValue(Target)), Name)
		[accessPropertyAt, structural]
endmodule

module JS-ASSIGNMENT 
  imports JS-PROPERTY-ACCESS
  
	// ECMA-262 11.4.1 The delete Operator 
	rule $delete(Value:V) 							=> $b(true) when notBinding(Value)

	rule $delete($binding(.None, _)) 				=> $b(true)

	rule $delete($binding(Target:V, Name:String)) 
		=> deleteProperty(toObject(Target), Name) 	when Target =/=K .None

	// If IsPropertyReference(ref) is true, then
	// 	a.	Return the result of calling the [[Delete]] internal method on ToObject(GetBase(ref)) providing GetReferencedName(ref) and IsStrictReference(ref) as the arguments.
	// 	5.	Else, ref is a Reference to an Environment Record binding, so a.	If IsStrictReference(ref) is true, throw a SyntaxError exception. b.	Let bindings be GetBase(ref). c.	Return the result of calling the DeleteBinding concrete method of bindings, providing
	// 	GetReferencedName(ref) as the argument.


	syntax K ::= assignmentExp( String , K , K )

	rule $assignment(Target:V, Value:V) => putValue(Target, getValue(Value))  [assignment-simple]
	
	
	// Desugar shortcut assignments into and operation followed by a standard assignments
	rule $compoundAssignment(Target:V, OpName:String, Value:V) 
		=> $assignment(Target, assignmentExp(OpName, Target, Value))  
		[compoundAssignment-desugar, structural]

	// *=   /=   %=   +=   -=   <<=   >>=   >>>=  &=   ^=   |=
	rule assignmentExp("*", Target:V, Value:V)	=> $mul(Target, Value)  [assignment-mul, structural]
	rule assignmentExp("/", Target:V, Value:V)	=> $div(Target, Value)  [assignment-div, structural]
	rule assignmentExp("%", Target:V, Value:V)	=> $mod(Target, Value)  [assignment-mod, structural]
	rule assignmentExp("+", Target:V, Value:V)	=> $add(Target, Value)  [assignment-add, structural]
	rule assignmentExp("-", Target:V, Value:V)	=> $sub(Target, Value)  [assignment-sub, structural]
	rule assignmentExp("<<", Target:V, Value:V)	=> $lsl(Target, Value)  [assignment-lsl, structural]
	rule assignmentExp(">>", Target:V, Value:V)	=> $lsr(Target, Value)  [assignment-lsr, structural]
	rule assignmentExp(">>>", Target:V, Value:V)	=> $asr(Target, Value)  [assignment-asr, structural]
	rule assignmentExp("&", Target:V, Value:V)	=> $bitAnd(Target, Value)  [assignment-and, structural]
	rule assignmentExp("^", Target:V, Value:V)	=> $bitXor(Target, Value)  [assignment-xor, structural]
	rule assignmentExp("|", Target:V, Value:V)	=> $bitOr(Target, Value)  [assignment-or, structural]
	
endmodule

module JS-COMPARISON 
  imports JS-ASSIGNMENT

	// ECMA-262 11.8.1-4
	rule $lt(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Left, "N"), toPrimitive(Right, "N")), true))
	rule $gt(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Right, "N"), toPrimitive(Left, "N")), true))
	rule $lte(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Right, "N"), toPrimitive(Left, "N")), false))
	rule $gte(Left:V, Right:V) 
		=> $b(comparison(lessThan(toPrimitive(Left, "N"), toPrimitive(Right, "N")), false))


	// ECMA-262 11.8.5
	syntax Bool ::= comparison( K , Bool )
	syntax K ::= lessThan( V , V )
	// TODOS: check whether the Ks & Vs in syntax above should be different
	
	rule comparison($nan, _)			=> false		[structural]
	rule comparison(B:Bool, true)		=> B			[structural]
	rule comparison(B:Bool, false)		=> notBool(B)	[structural]
	
	// TODOS: replace primLessThan with arith
	rule lessThan($s(S1:String), $s(S2:String)) 	=> S1 <String S2				[structural]
	rule lessThan(Left:V, Right:V) 	=> arith("<", toNumber(Left), toNumber(Right))
										when (notString(Left) orBool notString(Right))	[structural]
	
	rule arith("<", $inf(Sign1:Int), $inf(Sign2:Int)) => Sign1 <Int Sign2			[structural]
	rule arith("<", $inf(Sign:Int), Num:V)	=> Sign <Int 0 when isFinite(Num)		[structural]
	rule arith("<", Num:V, $inf(Sign:Int)) 	=> 0 <Int Sign when isFinite(Num)		[structural]
	rule arith("<", Num1:V, Num2:V) 		=> finiteValue(Num1) <Float finiteValue(Num2)
	 										when isFinite(Num1) andBool isFinite(Num2)	[structural]
endmodule

module JS-IDENTITY 
  imports JS-COMPARISON
  
	syntax Bool ::= identical( V , V )
	
	// ECMA-262 11.9.4-6
	
	rule $notIdentical(Left:V, Right:V) => not($identical(Left, Right))			[structural]
	
	rule $identical($nan, _) 			=> $b(false)
	rule $identical(_, $nan) 			=> $b(false)
	rule $identical(Left:V, Right:V)	=> Left ==K Right
										when notNan(Left) andBool notNan(Right)
endmodule



module JS-EQUALITY 
  imports JS-IDENTITY
  
	syntax Bool ::= castEqual( V , V )
	
	// ECMA-262 11.9.4-6	
	rule $notEqual(Left:V, Right:V) 	=> not($equal(Left, Right))			[structural]
	
	rule $equal(Left:V, Right:V) 		
	=> if(typeof(Left) ==K typeof(Right), $identical(Left, Right), castEqual(Left, Right))
	// => #if typeof(Left) ==K typeof(Right) #then $identical(Left, Right) 
	//    #else castEqual(Left, Right) #fi
	
	rule castEqual($undefined, $null) 		=> $b(true)
	rule castEqual($null, $undefined) 		=> $b(true)
	// rule castEqual($obj(I:Int), Value:V)	=> $equal(toPrimitive($obj(I)), Value)
	// rule castEqual(Value:V, $obj(I:Int))	=> $equal(Value, toPrimitive($obj(I)))
	rule castEqual($b(B:Bool), Value:V)		=> $equal(toNumber($b(B)), Value)
	rule castEqual(Value:V, $b(B:Bool))		=> $equal(Value, toNumber($b(B)))
	
	rule castEqual($s(S:String), Value:V)	=> $equal(toNumber($s(S)), Value) 	when isNumber(Value)
	rule castEqual(Value:V, $s(S:String))	=> $equal(Value, toNumber($s(S))) 	when isNumber(Value)	
endmodule


module JS-OBJECT-LITERAL 
  imports JS-EQUALITY
  
	// ECMA-262 11.1.5 Object Initialiser
	syntax K ::= composeObjLit( List{K} , V ) [strict(2)]

	rule $objectLit(PropertyLits:List{K}) => composeObjLit(PropertyLits, $rec(.Map))  [objectLit]

	rule composeObjLit(.List{K}, Rec:V) => allocNewObjFor(Rec)
		[composeObjLit-empty, structural]
		
	rule composeObjLit($propertyLit(Key:K, Exp:K) ,, Remaining:List{K}, Rec:V)
			=> composeObjLit(Remaining, copyRecWithValue(Rec, asPropertyName(Key), getValue(Exp)))
		[composeObjLit-each, structural]
endmodule

module JS-STATEMENTS 
  imports JS-OBJECT-LITERAL
  
	syntax K ::= stmts ( List{K} ) [strict]

/*
	// ECMA-262 12.1 Block Statement
	rule $block(.List{K}) => $completion("NORMAL", .None, .None)
		[block-empty, structural]

	rule $block(Stmt:K ,, Stmts:List{K}) => stmts(Stmt, Stmts, .None)
		[block, structural]


	rule stmts(.List{K}, .List{K}, Completion:V) => Completion  [stmts-last-done]

	rule stmts($completion("NORMAL", .None, _), Stmt:K ,, Stmts:List{K}, Completion:V) 
		=> stmts(Stmt, Stmts, Completion)  [stmts-normal-no-result]

	rule stmts($completion("NORMAL", Value:V, TargetLabel:String), Stmt:K ,, Stmts:List{K}, _) 
		=> stmts(Stmt, Stmts, $completion("NORMAL", Value, TargetLabel)) 	when notNone(Value)  [stmts-normal]

	rule stmts(Completion:V, Stmts:List{K}) => Completion				when isAbrupt(Completion)  [stmts-abrupt]

*/

		
		
	// // rule [statement-queue-empty]: 
	// // 	<k> stmts(.List{K}) => . ...</k>  
	// // 	[structural]
	// // 
	// // rule [statement-queue-next]: 
	// // 	<k> stmts(Stmt:K ,, Stmts:List{K}) => Stmt ~> stmts(Stmts) ...</k>  
	// // 	[structural]
	// 
	// rule [statement-final-stmt]: 
	// 	<k> stmts(Stmt:K) => Stmt ...</k>
	// 	[structural]
	// 
	// rule [statement-queue-next]: 
	// 	<k> stmts(Stmt:K ,, NextStmt:K ,, Stmts:List{K}) => Stmt ~> stmts(NextStmt ,, Stmts) ...</k>  
	// 	[structural]
	// 
	// rule [statement-consume-value]: 
	// 	<k> (Value:V => . ) ~> stmts(NextStmt:K ,, Stmts:List{K}) ...</k>  



	// ECMA-262 12.2 Variable Statement

	// Since the var declarations have already been made, the var declarations need to be stripped
	// from the execution.  However, the var initialize expressions need to be set to run.
	rule $varDeclarations(.List{K}) => $completion("NORMAL", .None, .None) 
		[strip-inline-varDeclarations-empty, structural]

	rule $varDeclarations($id(_) ,, Remaining:List{K}) => $varDeclarations(Remaining:List{K})
		[strip-inline-varDeclarations-id, structural]

	rule $varDeclarations($initNewId(Name:String, Exp:K) ,, Remaining:List{K})
		=> $assignment($id(Name), Exp) ~> discard ~> $varDeclarations(Remaining)  
		[strip-inline-varDeclarations-init, structural]


	syntax K ::= exps ( List{K} )
	
	rule exps(Exp:K) => Exp  [exps-one]
		
	rule exps(Exp:K ,, NextExp:K ,, Exps:List{K}) => Exp ~> exps(NextExp ,, Exps)  [exps-multiple]
		
	rule (Value:V => . ) ~> exps(Exp:K ,, Exps:List{K})  [exps-strip-result]

	rule (Exps:List{K}) => exps(Exps)
		[comma, structural]

	// ECMA-262 12.4 Expression Statement
	rule $expStmt(Exp:K) => $completion("NORMAL", getValue(Exp), .None)
		[expression-stmt, structural]


	// ECMA-262 12.5 If Statement
	rule $ifElse(ConditionExp:V, TrueStmt:K , FalseStmt:K) 
		=> $ternary(ConditionExp, 
			TrueStmt, #if FalseStmt ==K .None 
				#then $completion("NORMAL", .None, .None) #else FalseStmt #fi)  [ifElse]

endmodule

// module
// 
// 	rule [for]:
// 		$for(InitExp:K, .None, IncrExp:K, Body:K) => $for(InitExp, $b(true), IncrExp, Body)
// 
// 	rule [for]:
// 		$for(InitExp:K, TestExp:K, IncrExp:K, Body:K) 
// 		=> getValue(InitExp) ~> loop(isTruthy(TestExp), Body, IncrExp, .None)
// 
// 	rule [loop]:
// 		loop(TestExp:K, Body:K, IncrExp:K, Result:V) 
// 		=> $ternary(TestExp, 
// 				stmts(loop(TestExp, Body, IncrExp, loopStep(Body:K)) ,, IncrExp),
// 				completion("normal", Result, .None))
// 	
// 	rule [loopStep]:
// 		
// 		
// 	rule [loop]:
// 		loop(false, LastValue:V, _, _) => $completion("normal", LastValue, .None)
// 		
// 	rule [loop]:
// 		loop(true, LastValue:V, IncrExp:K, Stmts:K) => doLoop(Stmts, LastValue, IncrExp, TestExp)
// 		
// 	rule [doLoop]:
// 		nextLoop($completion("normal", Value:V, Label:V), LastValue:V, IncrExp, TestExp) 
// 		=> IncrExp ~> loop(TestExp, nextValue(Value, LastValue), ) 
// 
// 	nextValue() => #if Value =/=K .None #then
// 	rule [doLoop]:
// 		doLoop(NewResult:V, LastValue:V, IncrExp, TestExp) 
// 		=> IncrExp ~> loop(isTruthy(TestExp), when isEmptyResult()
// 

// 
// 
// 



module JS-SSEMANTICS 
  imports JS-STATEMENTS
// imports JS-CONTROL-STRUCTURES
// imports JS-ARITHMETIC

	syntax K ::= 
			varPreDeclarations(	List{K} )
		|	varPreDeclaration( K )
		|	funcPreDeclarations( List{K} )
		
	rule $program(VarDecls:List{K}, FuncDecls:List{K}, Stmts:List{K}) 
			=> varPreDeclarations(VarDecls) ~> funcPreDeclarations(FuncDecls) ~> stmts(Stmts)
		[begin-program, structural]


		
	// The varPreDeclarations are a list of expression that include var declarations.  
	// These var names need to be striped out and added to the envRec.
	rule varPreDeclarations(.List{K}) => . 
		[varPreDeclarations-empty, structural]
		
	rule varPreDeclarations(Decl:K ,, Remaining:List{K})
			=> varPreDeclaration(Decl) ~> varPreDeclarations(Remaining)
		[varPreDeclarations-each, structural]

	rule <k> varPreDeclaration($id(Name:String)) 
			=> putValue($binding(EnvObj, Name), $undefined) ~> discard ...</k>
		<lexEnv> $env(_, EnvObj:V) </lexEnv>
		[varPreDeclaration-identifier, structural]
				
	rule varPreDeclaration($initNewId(Name:String , _)) => varPreDeclaration($id(Name))
		[varPreDeclaration-ignore-initialization, structural]


	rule funcPreDeclarations(.List{K}) => . 
		[funcPreDeclarations-empty, structural]



endmodule


