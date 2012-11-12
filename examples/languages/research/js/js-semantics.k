--- ../../../tools/kompile.pl -lint js


kmod JS-UNARY is including JS-BASIC-SEMANTICS
	syntax V ::= not ( K )
	syntax V ::= neg ( K )

	rule not($b(B:Bool)) => $b(notBool(B))									[structural anywhere]
	
	rule neg(Value:V) 		=> neg(toNumber(Value))	 when notNumber(Value)	[structural anywhere]
	rule neg($f(F:Float)) 	=> $f(0 -Float F)								[structural anywhere]
	rule neg($z(Sign:Int))	=> $z(0 -Int Sign)								[structural anywhere]
	rule neg($inf(Sign)) 	=> $inf(0 -Int Sign)							[structural anywhere]
	rule neg($nan) 			=> $nan											[structural anywhere]
	
	--- ECMA-262 11.4.6
	rule $plus(Value:V) => toNumber(Value)

	--- ECMA-262 11.4.7
	rule $neg(Value) => neg(Value)
	
	--- ECMA-262 11.4.8
	rule $not(Value) => not(toBoolean(Value))
	
endkm

kmod JS-ARITHMETIC is including JS-UNARY 

	syntax K ::= arith( String , K , K )	[strict]
	syntax K ::= newSign( Int , Int ) --- Int
	syntax K ::= newInfinity( Int , Int ) 
	syntax K ::= newZero( Int , Int )
	syntax K ::= strAdd( K , K )			[strict]
	syntax K ::= add( K , K )
	
	rule [newSign]: 
		newSign(Sign1:Int, Sign2:Int) 		=> Sign1 *Int Sign2				[structural]
	rule [newInfinity]: 
		newInfinity(Sign1:Int, Sign2:Int) 	=> $inf(newSign(Sign1, Sign2))	[structural]
	rule [newZero]: 
		newZero(Sign1:Int, Sign2:Int) 		=> $z(newSign(Sign1, Sign2))	[structural]
	
	--- arithmetic expressions
	--- TO DOS: Add mod
	rule [div]: $div(Left:V, Right:V) 	=> arith("div", getValue(Left), getValue(Right))
	rule [mul]: $mul(Left:V, Right:V) 	=> arith("mul", getValue(Left), getValue(Right))
	rule [sub]: $sub(Left:V, Right:V) 	=> arith("sub", getValue(Left), getValue(Right))
	rule [add]: $add(Left:V, Right:V) 	
			=> strAdd(toPrimitive(getValue(Left), "N"), toPrimitive(getValue(Right), "N"))


	--- arithmetic with NaN
	rule [nan-num-arith]: arith(OpName:String, $nan, _)	=> $nan					[structural]
	rule [num-nan-arith]: arith(OpName:String, _, $nan)	=> $nan					[structural]


	--- arithmetic conversions
	rule [arith-coerce-left-to-num]: 
		arith(OpName:String, Left:V, Right:V) 	=> arith(OpName, toNumber(Left), Right) 
												when notNumber(Left)	 		[structural]
	rule [arith-coerce-right-to-num]: 
		arith(OpName:String, Left:V, Right:V) 	=> arith(OpName, Left, toNumber(Right)) 
												when notNumber(Right) 			[structural]
	
	
	--- arithmetic base cases
	rule [f-div]: arith("div", $f(F1:Float), $f(F2:Float)) => $n(F1 /Float F2)	[structural]
	rule [f-mod]: arith("mod", $f(F1:Float), $f(F2:Float)) => $n(F1 %Float F2)	[structural]
	rule [f-mul]: arith("mul", $f(F1:Float), $f(F2:Float)) => $n(F1 *Float F2)	[structural]
	--- rule [f-sub]: arith("sub", $f(F1:Float), $f(F2:Float)) => $n(F1 -Float F2)	[structural]
	rule [f-add]: arith("add", $f(F1:Float), $f(F2:Float)) => $n(F1 +Float F2)	[structural]
	

	--- ECMA-262 11.5.1
	rule arith("mul", $inf(Sign1:Int), $inf(Sign2:Int)) => newInfinity(Sign1, Sign2) [structural]
	rule arith("mul", $inf(Sign:Int), $f(F:Float))	=> newInfinity(Sign, sign(F))	[structural]
	rule arith("mul", $f(F:Float), $inf(Sign:Int))	=> newInfinity(sign(F), Sign)	[structural]
	rule arith("mul", $z(_), $inf(_)) 					=> $nan 					[structural]
	rule arith("mul", $inf(_), $z(_)) 					=> $nan 					[structural]
	rule arith("mul", $z(Sign:Int), $f(F:Float))		=> newZero(Sign, sign(F))	[structural]
	rule arith("mul", $f(F:Float), $z(Sign:Int))		=> newZero(sign(F), Sign)	[structural]
	rule arith("mul", $z(Sign1:Int), $z(Sign2:Int)) 	=> newZero(Sign1, Sign2)	[structural]
	
	
	--- ECMA-262 11.5.2
	rule arith("div", $inf(_), $inf(_)) 				=> $nan 						[structural]
	rule arith("div", $inf(Sign:Int), $f(F:Float)) 		=> newInfinity(Sign, sign(F))	[structural]
	rule arith("div", $inf(Sign1:Int), $z(Sign2:Int))	=> newInfinity(Sign1, Sign2) 	[structural]
	rule arith("div", Num:V, $inf(Sign:Int)) 			=> newZero(sign(Num), Sign) 
														when isFinite(Num)				[structural]
	rule arith("div", $z(Sign:Int), $f(F:Float)) 		=> newZero(Sign, sign(F))		[structural]
	rule arith("div", $z(_), $z(_)) 					=> $nan							[structural]
	rule arith("div", $f(F:Float), $z(Sign:Int)) 		=> newInfinity(sign(F), Sign)	[structural]


	--- ECMA-262 11.5.3
	rule arith("mod", $inf(_), Num:V) 		=> $nan		when notNan(Num)		[structural]
	rule arith("mod", Num:V, $z(_)) 		=> $nan		when notNan(Num)		[structural]
	rule arith("mod", Num:V, $inf(_)) 		=> Num 		when isFinite(Num) 		[structural]
	rule arith("mod", $z(Sign:Int), Num:V)  => $z(Sign)	when isFinite(Num) 		[structural]


	--- ECMA-262 11.6.1
	rule strAdd($s(S1:String), $s(S2:String)) => $s(S1 +String S2)	 				[structural]
	rule strAdd($s(S1), Value:V) 	=> strAdd($s(S1), toString(Value))				[structural]
	rule strAdd(Value:V, $s(S2)) 	=> strAdd(toString(Value), $s(S2))				[structural]
	rule strAdd(Left:V, Right:V) 	=> arith("add", Left, Right) 
									when (notString(Left) andBool notString(Right))	[structural]
	
	--- ECMA-262 11.6.2
	rule arith("sub", Left, Right) => arith("add", Left, neg(Right))				[structural]
	
	--- ECMA-262 11.6.3
	rule arith("add", $inf(Sign:Int), $inf(Sign2:Int)) 
		=> #if Sign ==K Sign2 #then $inf(Sign) #else $nan #fi						[structural]
	rule arith("add", $inf(Sign:Int), Num:V) 	=> $inf(Sign)	when isFinite(Num)	[structural]
	rule arith("add", Num:V, $inf(Sign:Int)) 	=> $inf(Sign)	when isFinite(Num)	[structural]
	rule arith("add", $z(Sign), $z(Sign2)) 			
		=> #if Sign ==K Sign2 #then $z(Sign) #else $z(1) #fi						[structural]
	rule arith("add", $z(_), $f(F:Float)) 		=> $f(F)							[structural]
	rule arith("add", $f(F:Float), $z(_)) 		=> $f(F)							[structural]
	
	
	--- add shortcut
	rule add(Left:V, Right:V) => arith("add", Left, Right)					[structural anywhere]
	
endkm

kmod JS-PREFIX-POSTFIX is including JS-ARITHMETIC
	syntax K ::= postOp( V , K , Int )	[strict]
	syntax K ::= preOp( V , K , Int )	[strict]
	
	--- ECMA-262 11.3.1 Postfix Increment Operator
	rule $inc(Target:V, true) => postOp(Target, toNumber(getValue(Target)), 1)
	
 	--- ECMA-262 11.3.2 Postfix Decrement Operator
	rule $dec(Target:V, true) => postOp(Target, toNumber(getValue(Target)), -1)
	
	rule postOp(Target:V, PrevNum:V, I:Int) 
	=> putValue(Target, add(PrevNum, $n(I))) ~> discard ~> PrevNum					[structural]
	
	--- ECMA-262 11.4.4 Prefix Increment Operator
	rule $inc(Target:V, false) => preOp(Target, toNumber(getValue(Target)), 1)
	
	--- ECMA-262 11.4.5 Prefix Increment Operator
	rule $dec(Target:V, false) => preOp(Target, toNumber(getValue(Target)), -1)
	
	rule preOp(Target:V, Num:V, I:Int) => putValue(Target, add(Num, $n(I)))		[structural]
endkm

kmod JS-PROPERTY-ACCESS is including JS-PREFIX-POSTFIX
	syntax K ::= accessPropertyAt( K , K ) [seqstrict]
	
	rule [staticAccess]:
		$staticAccess(Target:V, $id(Name:String)) => accessPropertyAt(Target, $s(Name))
	
	rule [dynamicAccess]:
		$dynamicAccess(Target:V, PropNameValue:V) 
		=> accessPropertyAt(Target, toString(getValue(PropNameValue)))
	
	rule [accessPropertyAt]:
		accessPropertyAt(Target:V, $s(Name:String)) 
		=> $binding(checkObjectCoercible(getValue(Target)), Name)
		[structural]
endkm

kmod JS-ASSIGNMENT is including JS-PROPERTY-ACCESS
	--- ECMA-262 11.4.1 The delete Operator 
	rule $delete(Value:V) 							=> $b(true) when notBinding(Value)

	rule $delete($binding(.None, _)) 				=> $b(true)

	rule $delete($binding(Target:V, Name:String)) 
		=> deleteProperty(toObject(Target), Name) 	when Target =/=K .None

	--- If IsPropertyReference(ref) is true, then
	--- 	a.	Return the result of calling the [[Delete]] internal method on ToObject(GetBase(ref)) providing GetReferencedName(ref) and IsStrictReference(ref) as the arguments.
	--- 	5.	Else, ref is a Reference to an Environment Record binding, so a.	If IsStrictReference(ref) is true, throw a SyntaxError exception. b.	Let bindings be GetBase(ref). c.	Return the result of calling the DeleteBinding concrete method of bindings, providing
	--- 	GetReferencedName(ref) as the argument.


	syntax K ::= assignmentExp( String , K , K )

	rule [assignment-simple]:
		$assignment(Target:V, Value:V) => putValue(Target, getValue(Value))
	
	
	--- Desugar shortcut assignments into and operation followed by a standard assignments
	rule [compoundAssignment-desugar]:
		$compoundAssignment(Target:V, OpName:String, Value:V) 
		=> $assignment(Target, assignmentExp(OpName, Target, Value))  
		[structural]

	--- *=   /=   %=   +=   -=   <<=   >>=   >>>=  &=   ^=   |=
	rule [assignment-mul]:
		assignmentExp("*", Target:V, Value:V)	=> $mul(Target, Value)  [structural]
	rule [assignment-div]:
		assignmentExp("/", Target:V, Value:V)	=> $div(Target, Value)  [structural]
	rule [assignment-mod]:
		assignmentExp("%", Target:V, Value:V)	=> $mod(Target, Value)  [structural]
	rule [assignment-add]:
		assignmentExp("+", Target:V, Value:V)	=> $add(Target, Value)  [structural]
	rule [assignment-sub]:
		assignmentExp("-", Target:V, Value:V)	=> $sub(Target, Value)  [structural]
	rule [assignment-lsl]:
		assignmentExp("<<", Target:V, Value:V)	=> $lsl(Target, Value)  [structural]
	rule [assignment-lsr]:
		assignmentExp(">>", Target:V, Value:V)	=> $lsr(Target, Value)  [structural]
	rule [assignment-asr]:
		assignmentExp(">>>", Target:V, Value:V)	=> $asr(Target, Value)  [structural]
	rule [assignment-and]:
		assignmentExp("&", Target:V, Value:V)	=> $bitAnd(Target, Value)  [structural]
	rule [assignment-xor]:
		assignmentExp("^", Target:V, Value:V)	=> $bitXor(Target, Value)  [structural]
	rule [assignment-or]:
		assignmentExp("|", Target:V, Value:V)	=> $bitOr(Target, Value)  [structural]
	
endkm

kmod JS-COMPARISON is including JS-ASSIGNMENT

	--- ECMA-262 11.8.1-4
	rule $lt(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Left, "N"), toPrimitive(Right, "N")), true))
	rule $gt(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Right, "N"), toPrimitive(Left, "N")), true))
	rule $lte(Left:V, Right:V) 	
		=> $b(comparison(lessThan(toPrimitive(Right, "N"), toPrimitive(Left, "N")), false))
	rule $gte(Left:V, Right:V) 
		=> $b(comparison(lessThan(toPrimitive(Left, "N"), toPrimitive(Right, "N")), false))


	--- ECMA-262 11.8.5
	syntax Bool ::= comparison( K , Bool )
	syntax K ::= lessThan( V , V )
	--- TODOS: check whether the Ks & Vs in syntax above should be different
	
	rule comparison($nan, _)			=> false		[structural]
	rule comparison(B:Bool, true)		=> B			[structural]
	rule comparison(B:Bool, false)		=> notBool(B)	[structural]
	
	--- TODOS: replace primLessThan with arith
	rule lessThan($s(S1:String), $s(S2:String)) 	=> S1 <String S2				[structural]
	rule lessThan(Left:V, Right:V) 	=> arith("<", toNumber(Left), toNumber(Right))
										when (notString(Left) orBool notString(Right))	[structural]
	
	rule arith("<", $inf(Sign1:Int), $inf(Sign2:Int)) => Sign1 <Int Sign2			[structural]
	rule arith("<", $inf(Sign:Int), Num:V)	=> Sign <Int 0 when isFinite(Num)		[structural]
	rule arith("<", Num:V, $inf(Sign:Int)) 	=> 0 <Int Sign when isFinite(Num)		[structural]
	rule arith("<", Num1:V, Num2:V) 		=> finiteValue(Num1) <Float finiteValue(Num2)
	 										when isFinite(Num1) andBool isFinite(Num2)	[structural]
endkm

kmod JS-IDENTITY is including JS-COMPARISON
	syntax Bool ::= identical( V , V )
	
	--- ECMA-262 11.9.4-6
	
	rule $notIdentical(Left:V, Right:V) => not($identical(Left, Right))			[structural]
	
	rule $identical($nan, _) 			=> $b(false)
	rule $identical(_, $nan) 			=> $b(false)
	rule $identical(Left:V, Right:V)	=> Left ==K Right
										when notNan(Left) andBool notNan(Right)
endkm



kmod JS-EQUALITY is including JS-IDENTITY
	syntax Bool ::= castEqual( V , V )
	
	--- ECMA-262 11.9.4-6	
	rule $notEqual(Left:V, Right:V) 	=> not($equal(Left, Right))			[structural]
	
	rule $equal(Left:V, Right:V) 		
	=> if(typeof(Left) ==K typeof(Right), $identical(Left, Right), castEqual(Left, Right))
	--- => #if typeof(Left) ==K typeof(Right) #then $identical(Left, Right) 
	---    #else castEqual(Left, Right) #fi
	
	rule castEqual($undefined, $null) 		=> $b(true)
	rule castEqual($null, $undefined) 		=> $b(true)
	--- rule castEqual($obj(I:Int), Value:V)	=> $equal(toPrimitive($obj(I)), Value)
	--- rule castEqual(Value:V, $obj(I:Int))	=> $equal(Value, toPrimitive($obj(I)))
	rule castEqual($b(B:Bool), Value:V)		=> $equal(toNumber($b(B)), Value)
	rule castEqual(Value:V, $b(B:Bool))		=> $equal(Value, toNumber($b(B)))
	
	rule castEqual($s(S:String), Value:V)	=> $equal(toNumber($s(S)), Value) 	when isNumber(Value)
	rule castEqual(Value:V, $s(S:String))	=> $equal(Value, toNumber($s(S))) 	when isNumber(Value)	
endkm


kmod JS-OBJECT-LITERAL is including JS-EQUALITY
	--- ECMA-262 11.1.5 Object Initialiser
	syntax K ::= composeObjLit( List{K} , V ) [strict(2)]

	rule [objectLit]:
		<k> $objectLit(PropertyLits:List{K}) => composeObjLit(PropertyLits, $rec(.Map)) ...</k>

	rule [composeObjLit-empty]:
		<k> composeObjLit(.List{K}, Rec:V) => allocNewObjFor(Rec) ...</k>
		[structural]
		
	rule [composeObjLit-each]:
		<k> composeObjLit($propertyLit(Key:K, Exp:K) ,, Remaining:List{K}, Rec:V)
			=> composeObjLit(Remaining, copyRecWithValue(Rec, asPropertyName(Key), getValue(Exp)))
		...</k>
		[structural]
endkm

kmod JS-STATEMENTS is including JS-OBJECT-LITERAL
	syntax K ::= stmts ( K , List{K} ) [strict(1)]


	--- ECMA-262 12.1 Block Statement
	rule [block-empty]:
		$block(.List{K}) => $completion("NORMAL", .None, .None)
		[structural]

	rule [block]:
		$block(Stmt:K ,, Stmts:List{K}) => stmts(Stmt, Stmts, .None)
		[structural]


	rule [stmts-last-done]:
		stmts(.List{K}, .List{K}, Completion:V) => Completion

	rule [stmts-normal-no-result]:
		stmts($completion("NORMAL", .None, _), Stmt:K ,, Stmts:List{K}, Completion:V) 
		=> stmts(Stmt, Stmts, Completion)

	rule [stmts-normal]:
		stmts($completion("NORMAL", Value:V, TargetLabel:String), Stmt:K ,, Stmts:List{K}, _) 
		=> stmts(Stmt, Stmts, $completion("NORMAL", Value, TargetLabel)) 	when notNone(Value)

	rule [stmts-abrupt]:
		stmts(Completion:V, Stmts:List{K}) => Completion				when isAbrupt(Completion)



		
		
	--- --- rule [statement-queue-empty]: 
	--- --- 	<k> stmts(.List{K}) => . ...</k>  
	--- --- 	[structural]
	--- --- 
	--- --- rule [statement-queue-next]: 
	--- --- 	<k> stmts(Stmt:K ,, Stmts:List{K}) => Stmt ~> stmts(Stmts) ...</k>  
	--- --- 	[structural]
	--- 
	--- rule [statement-final-stmt]: 
	--- 	<k> stmts(Stmt:K) => Stmt ...</k>
	--- 	[structural]
	--- 
	--- rule [statement-queue-next]: 
	--- 	<k> stmts(Stmt:K ,, NextStmt:K ,, Stmts:List{K}) => Stmt ~> stmts(NextStmt ,, Stmts) ...</k>  
	--- 	[structural]
	--- 
	--- rule [statement-consume-value]: 
	--- 	<k> (Value:V => . ) ~> stmts(NextStmt:K ,, Stmts:List{K}) ...</k>  



	--- ECMA-262 12.2 Variable Statement

	--- Since the var declarations have already been made, the var declarations need to be stripped
	--- from the execution.  However, the var initialize expressions need to be set to run.
	rule [strip-inline-varDeclarations-empty]:
		$varDeclarations(.List{K}) => $completion("NORMAL", .None, .None) 
		[structural]

	rule [strip-inline-varDeclarations-id]: 
		$varDeclarations($id(_) ,, Remaining:List{K}) => $varDeclarations(Remaining:List{K})
		[structural]

	rule [strip-inline-varDeclarations-init]: 
		$varDeclarations($initNewId(Name:String, Exp:K) ,, Remaining:List{K})
		=> $assignment($id(Name), Exp) ~> discard ~> $varDeclarations(Remaining)  
		[structural]


	syntax K ::= exps ( List{K} )
	
	rule [exps-one]: 
		exps(Exp:K) => Exp
		
	rule [exps-multiple]: 
		exps(Exp:K ,, NextExp:K ,, Exps:List{K}) => Exp ~> exps(NextExp ,, Exps)
		
	rule [exps-strip-result]: 
		(Value:V => . ) ~> exps(Exp:K ,, Exps:List{K})

	rule [comma]:
		 (Exps:List{K}) => exps(Exps)
		[structural]

	--- ECMA-262 12.4 Expression Statement
	rule [expression-stmt]:
		<k> $expStmt(Exp:K) => $completion("NORMAL", getValue(Exp), .None) ...</k> 
		[structural]


	--- ECMA-262 12.5 If Statement
	rule [ifElse]:
		$ifElse(ConditionExp:V, TrueStmt:K , FalseStmt:K) 
		=> $ternary(ConditionExp, 
			TrueStmt, #if FalseStmt ==K .None 
				#then $completion("NORMAL", .None, .None) #else FalseStmt #fi)

endkm

--- kmod
--- 
--- 	rule [for]:
--- 		$for(InitExp:K, .None, IncrExp:K, Body:K) => $for(InitExp, $b(true), IncrExp, Body)
--- 
--- 	rule [for]:
--- 		$for(InitExp:K, TestExp:K, IncrExp:K, Body:K) 
--- 		=> getValue(InitExp) ~> loop(isTruthy(TestExp), Body, IncrExp, .None)
--- 
--- 	rule [loop]:
--- 		loop(TestExp:K, Body:K, IncrExp:K, Result:V) 
--- 		=> $ternary(TestExp, 
--- 				stmts(loop(TestExp, Body, IncrExp, loopStep(Body:K)) ,, IncrExp),
--- 				completion("normal", Result, .None))
--- 	
--- 	rule [loopStep]:
--- 		
--- 		
--- 	rule [loop]:
--- 		loop(false, LastValue:V, _, _) => $completion("normal", LastValue, .None)
--- 		
--- 	rule [loop]:
--- 		loop(true, LastValue:V, IncrExp:K, Stmts:K) => doLoop(Stmts, LastValue, IncrExp, TestExp)
--- 		
--- 	rule [doLoop]:
--- 		nextLoop($completion("normal", Value:V, Label:V), LastValue:V, IncrExp, TestExp) 
--- 		=> IncrExp ~> loop(TestExp, nextValue(Value, LastValue), ) 
--- 
--- 	nextValue() => #if Value =/=K .None #then
--- 	rule [doLoop]:
--- 		doLoop(NewResult:V, LastValue:V, IncrExp, TestExp) 
--- 		=> IncrExp ~> loop(isTruthy(TestExp), when isEmptyResult()
--- 

--- 
--- 
--- 



kmod JS-SEMANTICS is including JS-STATEMENTS
--- JS-CONTROL-STRUCTURES + JS-ARITHMETIC
	syntax K ::= 
			varPreDeclarations(	List{K} )
		|	varPreDeclaration( K )
		|	funcPreDeclarations( List{K} )
		
	rule [begin-program]: 
		<k> $program(VarDecls:List{K}, FuncDecls:List{K}, Stmts:List{K}) 
			=> varPreDeclarations(VarDecls) ~> funcPreDeclarations(FuncDecls) ~> stmts(Stmts) </k>
		[structural]


		
	--- The varPreDeclarations are a list of expression that include var declarations.  
	--- These var names need to be striped out and added to the envRec.
	rule [varPreDeclarations-empty]: 
		<k> varPreDeclarations(.List{K}) => . ...</k>  
		[structural]
		
	rule [varPreDeclarations-each]: 
		<k> varPreDeclarations(Decl:K ,, Remaining:List{K})
			=> varPreDeclaration(Decl) ~> varPreDeclarations(Remaining) ...</k>  
		[structural]

	rule [varPreDeclaration-identifier]: 
		<k> varPreDeclaration($id(Name:String)) 
			=> putValue($binding(EnvObj, Name), $undefined) ~> discard ...</k>
		<lexEnv> $env(_, EnvObj:V) </lexEnv>
		[structural]
				
	rule [varPreDeclaration-ignore-initialization]: 
		varPreDeclaration($initNewId(Name:String , _)) => varPreDeclaration($id(Name))
		[structural]


	rule [funcPreDeclarations-empty]: 
		<k> funcPreDeclarations(.List{K}) => . ...</k>  
		[structural]



endkm


