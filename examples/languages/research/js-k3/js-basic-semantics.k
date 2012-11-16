require "js-configuration.k"
require "js-syntax.k"

module JS-UTILITY
  imports JS-SYNTAX
  
	syntax Bool ::= isKeyPresent( Map , K ) | isKeyAbsent( Map , K )
	syntax K ::= "discard"
	syntax K ::= replaceResult( K )
	
	rule isKeyPresent(M:Map, K:K) => $hasMapping(M, K) 
		[isKeyPresent, structural, anywhere]
		
//	rule isKeyPresent(M:Map, K:K) => true
//	  when K in keys(M)					[isKeyPresent, structural, anywhere]

	rule isKeyAbsent(M:Map, K:K) => notBool($hasMapping(M, K)) 
		[isKeyAbsent, structural, anywhere]
		
//	rule isKeyAbsent(M:Map, K:K) => true
//	  when notBool(K in keys(M))		[isKeyAbsent, structural, anywhere]
		
	rule <k> Previous:V ~> discard => . ...</k>   [discard]
//	rule _:KResult ~> discard => .  [discard, structural]
	
	rule [replaceResult]:
		<k> Previous:V ~> replaceResult(Value:V) => Value ...</k>
		
//	rule _:KResult ~> replaceResult(Value:V) => Value		[replaceResult, structural]
		
endmodule

module JS-TYPE 
  imports JS-UTILITY	
	
	syntax String ::= jsType( V )
/*	syntax TypeVals ::= "undefined"
					| "null"
					| "boolean"
					| "finite"
					| "infinite"
					| "nan"
					| "string"
					| "object"
					| "binding"
					| "function" */
					
	
	rule jsType($undefined)			=> "undefined"	[structural, anywhere]
	rule jsType($null) 				=> "null"		[structural, anywhere]
	rule jsType($b(_)) 				=> "boolean"	[structural, anywhere]
	rule jsType($z(_)) 				=> "finite"		[structural, anywhere]
	rule jsType($f(_)) 				=> "finite"		[structural, anywhere]
	rule jsType($inf(_)) 			=> "infinite"	[structural, anywhere]
	rule jsType($nan) 				=> "nan"		[structural, anywhere]
	rule jsType($s(_)) 				=> "string"		[structural, anywhere]
	rule jsType($obj(_)) 			=> "object"		[structural, anywhere]
	rule jsType($binding(_,_))		=> "binding"	[structural, anywhere]
//	rule jsType($function(_,_,_,_))	=> "function"	[structural, anywhere]

endmodule

module JS-TYPEOF 
  imports JS-TYPE
	
	syntax String ::= typeof( V )
	
	rule typeof($undefined) 		=> "undefined"	[structural, anywhere]
	rule typeof($null) 				=> "object"		[structural, anywhere]
	rule typeof($b(_)) 				=> "boolean"	[structural, anywhere]
	rule typeof($z(_)) 				=> "number"		[structural, anywhere]
	rule typeof($f(_)) 				=> "number"		[structural, anywhere]
	rule typeof($inf(_)) 			=> "number"		[structural, anywhere]
	rule typeof($nan) 				=> "number"		[structural, anywhere]
	rule typeof($s(_)) 				=> "string"		[structural, anywhere]
	rule typeof($obj(_)) 			=> "object"		[structural, anywhere]
	
//	--- ECMA-262 11.4.3
	rule $typeof(Value:V) => $s(typeof(Value)) 					[typeof]
	

//	--- rule $typeof($null) 	=> $s("object")
//	--- rule $typeof(Value:V) 	=> $s(jsType(Value)) when jsType(Value) =/=K "null"

//	---	rule [typeof]: $typeof(Value:V)		=> $s(jsTypeNameOf(jsType(V)))
//	--- rule [jsTypeNameOf]: jsTypeNameOf(Name:String)	=> 
//	--- 	#if Name ==K "numeric" 
//	--- 		#then "number" 
//	--- 		#else #if Name ==K "null" 
//	--- 			#then "object" 
//	--- 			#else Name
//	--- 		#fi
//	--- 	#fi 	[structural]
endmodule

module JS-TESTING 
  imports JS-TYPEOF
	
	syntax Bool ::= isTruthy( V ) 
				  | isFinite( V ) 
				  | notNan( V ) 
				  | isNumber( V ) 
				  | notNumber( V ) 
				  | isString( V ) 
				  | notString( V ) 
				  | isBinding( V ) 
				  | notBinding( V ) 
				  | isNone( V ) 
				  | notNone( V ) 
				  | isAbrupt( V ) 
				  | notAbrupt( V ) 
	
	rule isTruthy($undefined) 			=> false		 [structural, anywhere]
	rule isTruthy($null) 				=> false		 [structural, anywhere]
	rule isTruthy($b(B:Bool)) 			=> B			 [structural, anywhere]
	rule isTruthy($z(_)) 				=> false		 [structural, anywhere]
	rule isTruthy($f(_)) 				=> true			 [structural, anywhere]
	rule isTruthy($inf(_)) 				=> true			 [structural, anywhere]
	rule isTruthy($nan) 				=> false		 [structural, anywhere]
	rule isTruthy($s(S:String)) 		=> S =/=K ""	 [structural, anywhere]
	rule isTruthy($obj(_)) 				=> true			 [structural, anywhere]
//	--- rule jsType($function(_,_,_,_))
	
	rule isFinite(Value:V) 		=> jsType(Value) ==K "finite"	[isFinite, structural, anywhere]
	rule notNan(Value:V) 		=> jsType(Value) =/=K "nan"		[notNan, structural, anywhere]
	
	rule isNumber(Value:V) 		=> typeof(Value) ==K "number"	[isNumber, structural, anywhere]
	rule notNumber(Value:V) 	=> typeof(Value) =/=K "number"	[notNumber, structural, anywhere]
	
	rule isString(Value:V) 		=>	jsType(Value) ==K "string"	[isString, structural, anywhere]
	rule notString(Value:V) 	=>	jsType(Value) =/=K "string"	[notString, structural, anywhere]
	
	rule isBinding(Value:V)		=>	jsType(Value) ==K "binding"	[isBinding, structural, anywhere]
	rule notBinding(Value:V)	=>	jsType(Value) =/=K "binding" [notBinding, structural, anywhere]
		
	rule isNone(Value:V)		=>	Value ==K .None				[isNone, structural, anywhere]
	rule isNone(Value:V)		=>	Value =/=K .None			[notNone, structural, anywhere]
		
	rule isAbrupt($completion(CompletionType:String, _, _))
		=>	CompletionType =/=K "NORMAL"									[isAbrupt, structural, anywhere]
	rule notAbrupt($completion(CompletionType:String, _, _)) 
		=>	CompletionType ==K "NORMAL"										[notAbrupt, structural, anywhere]

endmodule

module JS-VALUE-ACCESS
  imports JS-TESTING
	syntax K ::= getValue( K )				[strict]
			   | putValue( K , K )			[strict]
			   
//	--- ECMA-262 8.7.1
	rule getValue(Value:V) => Value	when notBinding(Value) 
		[getValue-value, structural]
		
	rule getValue($binding(.None, Name:String)) 
		=> $throw($ReferenceError("Unresolvable property reference: " +String Name +String "!"))  
		[getValue-unresolved, structural]
		
	rule getValue($binding(Obj:V, Name:String)) => getPropertyValue(Obj, Name)		
		[getValue-identifier, structural]
		
		
//	--- ECMA-262 8.7.2
//	--- TO DOS: These params look wrong! Check them!
	rule putValue(Value:V, Name:String) 
			=> $throw($ReferenceError(
				"Cannot assign property named -> " +String Name +String " to value!"))
		when notBinding(Value)
		[putValue-error, structural]

	rule <k> putValue($binding(Obj:V, Name:String), Value:V) 
			=> setPropertyValue(
				#if Obj ==K .None #then GlobalObj #else Obj #fi, 
				Name, Value) ...</k>
		<globalEnv> GlobalObj:V </globalEnv> 
		[putValue, structural]

//	--- rule <k> putValue($binding(Target:JSPrimitiveTarget, Name:String), Value:JSValue) 
//	--- 		=> setPropertyValue(Obj, Name, Value) ...</k>
//	--- 	<lexEnv> $env(_, Obj:JSRef) </lexEnv> 
//	--- 	[putValue-primitive-base, structural, anywhere]			   
			   
endmodule

module JS-CONVERSIONS 
  imports JS-VALUE-ACCESS
	
	syntax K ::= toPrimitive( K , String )	[strict]
			   | toBoolean( K )				[strict]
			   | toNumber( K )				[strict]
			   | toString( K )				[strict]
			   | toObject( K )				[strict]
			   | asObject( K )				[strict]
	
			   | checkObjectCoercible( K )	[strict]
			   | asPropertyName( K ) 	// --- Not strict to avoid $id from begin resolved to a binding

	
//	--- ECMA-262 8.12.8
//---	rule defaultValue(Obj:JSObject, Hint:String) --- Not yet implemented!!!
	
//	--- ECMA-262 9.1
	rule toPrimitive(Prim:V, Hint:String)	=> Prim 						[structural]
//---	rule toPrimitive(Obj:JSObject, Hint)			=> defaultValue(Obj, Hint) [structural]
	
//	--- ECMA-262 9.2
	rule toBoolean(Value:V) 		=> $b(isTruthy(Value)) 					[structural]
	
//	--- ECMA-262 9.3
	rule toNumber(Value:V)			=> Value 		when isNumber(Value) 	[structural]
	rule toNumber($undefined) 		=> $nan									[structural]
	rule toNumber($null) 			=> $n(0)								[structural]
	rule toNumber($b(true)) 		=> $n(1)								[structural]
	rule toNumber($b(false)) 		=> $n(0)								[structural]
	rule toNumber($b(false)) 		=> $n(0)								[structural]
	rule toNumber($s(S:String)) 	=>  #if S ==K "" #then $n(0) #else 
										#if S ==K "Infinity" #then $inf(1) #else
										#if S ==K "-Infinity" #then $inf(-1) #else
										$n(String2Float(S)) #fi #fi #fi		[structural]
//	--- TODOS: need to handle non-numeric strings !!!
//	--- rule toNumber($obj(Loc:Int))	=> toNumber(toPrimitive($obj(Loc), "N")) [structural]
//	--- NOTE: some objects (such as arrays & numbers) map to $n(0) or $n(n)  !!!
	
//	--- ECMA-262 9.8
	rule toString($undefined)				=> $s("undefined")			[structural, anywhere]
	rule toString($null) 					=> $s("null")				[structural, anywhere]
	rule toString($b(true)) 				=> $s("true")				[structural, anywhere]
	rule toString($b(false)) 				=> $s("false")				[structural, anywhere]
	rule toString($nan) 					=> $s("NaN")				[structural, anywhere]
	rule toString($f(F:Float)) 				=> $s(Float2String(F))		[structural, anywhere]
	rule toString($inf(1)) 					=> $s("Infinity")			[structural, anywhere]
	rule toString($inf(-1)) 				=> $s("-Infinity")			[structural, anywhere]
	rule toString($z(_))					=> $s("0")					[structural, anywhere]
	rule toString($s(S:String)) 			=> $s(S)					[structural, anywhere]
//---	rule toString(Obj:JSObject)				=> $s("[object Object]")	[structural]
//	---	rule toString($s(S)) 			=> $invoke($staticAccess(newString(S),$s("toString")),$comma(.List{JS}))
	
	rule toObject(Value:V)	=> asObject(checkObjectCoercible(Value))
	
//	--- rule asObject($b(B:Bool))		=>
//	--- rule asObject(Num:V)			=> 		when isNumber(Num)
//	--- rule asObject($s(S:String))	=>	
	rule asObject($obj(Loc:Int)) 	=> $obj(Loc)
	
	rule checkObjectCoercible($undefined)		=> $throw($TypeError("Cannot be undefined!"))
	rule checkObjectCoercible($null)			=> $throw($TypeError("Cannot be null!"))
	rule checkObjectCoercible($b(B:Bool))		=> $b(B)
	rule checkObjectCoercible(Num:V)			=> Num				when isNumber(Num)
	rule checkObjectCoercible($s(S:String))		=> $s(S)
	rule checkObjectCoercible($obj(Loc:Int)) 	=> $obj(Loc)
	
	rule asPropertyName($s(Name:String))		=> Name
	rule asPropertyName($id(Name:String))		=> Name
	rule asPropertyName(Num:V)					=> toString(Num)	when isNumber(Num)
endmodule 

module JS-LOGIC 
  imports JS-CONVERSIONS
//	--- TODO: write this in terms of using 'if' below.
	
//	--- ECMA-262 11.11
	rule $and(Value:V, Exp:K)	=> #if isTruthy(Value) #then Exp #else Value #fi	[and]
	rule $or(Value:V, Exp:K) 	=> #if isTruthy(Value) #then Value #else Exp #fi	[or]
	
endmodule

module JS-CONDITIONAL 
  imports JS-LOGIC
//	--- TODO: add getValue calls in the right places
//	--- DONE: removed <k>  ...</k>
	syntax K ::= if( K , K , K ) 							[strict(1)]
	
	rule if(true, TrueBranch:K, _) 		=> TrueBranch 	[if-true, structural]
	rule if(false, _, FalseBranch:K) 	=> FalseBranch 	[if-false, structural]
	
//	--- ECMA-262 11.11
	rule $ternary(Exp:V, TrueBranch:K , FalseBranch:K)
			=> if(isTruthy(getValue(Exp)), TrueBranch, FalseBranch)		[ternary]
//		---	  => if isTruthy(Condition) then TrueExp else FalseExp fi

endmodule

//--- kmod JS-VAR is including JS-SYNTAX + JS-CONVERSIONS
//--- 
//--- $varDeclarations ( List{K} ) [strict]
//--- 
//--- macro o(type : "VAR" children : a(Exps) Extra) = $varDeclarations(Exps)
//--- 
//--- var count = elements.length;
//--- var index, element, separator;
//--- var accumulator = accumulator__ || [];
//--- 
//--- separator_ = separator_ || ',';
//--- 
//--- program(
//--- 	varDeclarations(id("count")),
//--- 	varDeclarations(id("index"),id("element"),id("separator")),
//--- 	varDeclarations(id("accumulator")),
//--- 	exp(assignment(id("separator_"),.undefined,or(id("separator_"),s(","))))) .


//---  rule <k>X:Id => I:Int<_/k> <state_>X|->I<_/state> 
//---  rule I1:Int + I2:Int => I1 +Int I2


module JS-ENV-REC 
  imports JS-CONDITIONAL
//	--- TO DOS: Add get set property methods.
		
	syntax K ::= hasRecProperty( K , String ) 				[strict]
			   | hasNoRecProperty( K , String ) 			[strict]
//---	syntax JSValue ::= 
	syntax K ::= getRecValueIfAbsent( K , String , K ) 		[strict]
			   | getEnsuredRecValue( K , String ) 			[strict]
			   | copyRecWithValue( K , String , K ) 		[strict]
		       | copyRecWithoutProperty( K , String ) 		[strict]
	
	rule hasRecProperty($rec(Properties:Map), Name:String) 
		=> isKeyPresent(Properties, Name)  
		[hasRecProperty, structural, anywhere]

	rule hasNoRecProperty($rec(Properties:Map), Name:String) 
		=> isKeyAbsent(Properties, Name)  
		[hasNoRecProperty, structural, anywhere]

	rule getRecValueIfAbsent($rec(Properties:Map), Name:String, AbsentAction:K) 
		=> AbsentAction
		when isKeyAbsent(Properties, Name)  
		[getRecValueIfAbsent-absent, structural, anywhere]

	rule getRecValueIfAbsent($rec((Name |-> Value:V) Extra:Map), Name:String, _) 
		=> Value  
		[getRecValueIfAbsent-present, structural, anywhere]

	rule getEnsuredRecValue($rec(Properties:Map), Name:String) 
		=> Properties(Name)  
		[getEnsuredRecValue, structural, anywhere]

	rule copyRecWithValue($rec(Properties:Map), Name:String, Value:V) 
		=> $rec(Properties[Value / Name])  
		[copyRecWithValue, structural, anywhere]
	
	rule copyRecWithoutProperty(Rec:V, Name:String) => Rec
		when hasNoRecProperty(Rec, Name)  
		[copyRecWithoutProperty-absent, structural, anywhere]

	rule copyRecWithoutProperty($rec((Name |-> _) Extra:Map), Name:String) 
		=> $rec(Extra)
		[copyRecWithoutProperty-present, structural, anywhere]
endmodule

module JS-OBJ 
  imports JS-ENV-REC
  imports JS-CONFIGURATION
	
	syntax K ::= hasProperty( K , String )		//--- [strict]
			   | hasNoProperty( K , String )	//--- [strict]
			   | "allocNewObj"
			   | allocNewObjFor( V )
			   | getEnsuredPropertyValue( K , String )
			   | getPropertyValueIfAbsent( K , String , K )
			   | getPropertyValue( K , String )
			   | setPropertyValue( K , String , K )
			   | deleteProperty( K , String )
	
	rule allocNewObj => allocNewObjFor($rec(.Map))
		 [allocNewObj, structural]

	rule <k> allocNewObjFor(Rec:V) => $obj(Loc) ...</k>
		 <nextLoc> Loc:Int => Loc +Int 1 </nextLoc>  
		 <heap> Heap:Map => Heap[Rec/Loc]  </heap>
		 [allocNewObjFor, structural]
	
//	--- ECMA-262 10.2.1.x.1 HasBinding(N)
//	--- Determine if an environment record has a binding for an identifier. 
//	--- Return true if it does and false if it does not. 
//	--- The String value N is the text of the identifier.
	
	rule <k> hasProperty($obj(Loc:Int), Name:String) => hasRecProperty(Heap(Loc), Name) ...</k>
		<heap> Heap:Map </heap>  
		[hasProperty, structural]

	rule <k> hasNoProperty($obj(Loc:Int), Name:String) => hasNoRecProperty(Heap(Loc), Name) ...</k>
		<heap> Heap:Map </heap>  
		[hasNoProperty, structural]

	rule <k> getEnsuredPropertyValue($obj(Loc:Int), Name:String) 
			=> getEnsuredRecValue(Heap(Loc), Name) ...</k>
		<heap> Heap:Map </heap>  
		[getEnsuredPropertyValue, structural]
	
	rule <k> getPropertyValueIfAbsent($obj(Loc:Int), Name:String, AbsentAction:K) 
			=> getRecValueIfAbsent(Heap(Loc), Name, AbsentAction) ...</k>
		<heap> Heap:Map </heap>  
		[getPropertyValueIfAbsent, structural]
	
//	--- ECMA-262 10.2.1.x.4 GetBindingValue(N,S)
//	--- Returns the value of an already existing binding from an environment record. 
//	--- The String value N is the text of the bound name. S is used to identify strict mode 
//	--- references. If S is true and the binding does not exist or is uninitialized throw a 
//	--- ReferenceError exception.
	
	rule getPropertyValue(Obj:V, Name:String) 
			=> getPropertyValueIfAbsent(Obj, Name, $undefined)
		[getPropertyValue, structural]
	
//	--- ECMA-262 10.2.1.x.2 CreateMutableBinding(N, D)
//	--- Create a new mutable binding in an environment record. 
//	--- The String value N is the text of the bound name. 
//	--- If the optional Boolean argument D is true the binding is may be subsequently deleted.
	
//	--- ECMA-262 10.2.1.x.3 SetMutableBinding(N,V, S)
//	--- Set the value of an already existing mutable binding in an environment record. 
//	--- The String value N is the text of the bound name. V is the value for the binding and 
//	--- may be a value of any ECMAScript language type. S is a Boolean flag. 
//	--- If S is true and the binding cannot be set throw a TypeError exception. 
//	---- S is used to identify strict mode references.
	
	rule <k> setPropertyValue($obj(Loc:Int), Name:String, Value:V) => Value ...</k>
		<heap>... Loc |-> (Rec:V => copyRecWithValue(Rec, Name, Value)) ...</heap>  
		[setPropertyValue, structural]
	
//	--- ECMA-262 10.2.1.x.5 DeleteBinding(N)
//	--- Delete a binding from an environment record. 
//	--- The String value N is the text of the bound name If a binding for N exists, 
//	--- remove the binding and return true. If the binding exists but cannot be removed return 
//	--- false. If the binding does not exist return true.

	rule <k> deleteProperty($obj(Loc:Int), Name:String) => . ...</k> 
		<heap>... Loc |-> (Rec:V => copyRecWithoutProperty(Rec, Name)) ...</heap>  
		[deleteProperty, structural]

//	--- TODOS: add Strict
//	--- TODOS: add property descs (immutability, configurability, enumerability)
//	--- TODOS: enable property key to be other than strings

//	--- ECMA-262 10.2.1.x.6 ImplicitThisValue()
//	--- Returns the value to use as the this value on calls to function objects that are obtained as binding values from this environment record.
//	--- 
//	--- ECMA-262 10.2.1.1.7 CreateImmutableBinding(N)
//	--- Create a new but uninitialized immutable binding in an environment record. The String value N is the text of the bound name.
//	--- ECMA-262 10.2.1.1.8 InitializeImmutableBinding(N,V)
//	--- Set the value of an already existing but uninitialized immutable binding in an environment record. The String value N is the text of the bound name. V is the value for the binding and is a value of any ECMAScript language type.

endmodule

module JS-IDENTIFIER-RESOLUTION 
  imports JS-OBJ
	syntax K ::= resolveIdentifier( K , String )	
		
//	--- ECMA-262 10.2.2.1	GetIdentifierReference (lex, name, strict)
//	--- GetPropertyReference
	
	rule <k> resolveIdentifier(.None, Name:String) => $binding(.None, Name) ...</k>
		[resolveIdentifier-unresolved-base-case, structural]
			
	rule <k> resolveIdentifier($env(Outer:K, Obj:V), Name:String) 
			=> if(hasProperty(Obj, Name), $binding(Obj, Name), resolveIdentifier(Outer, Name))
		...</k>
		[resolveIdentifier, structural]
	
//	--- ECMA-262 10.3.1  Identifier Resolution
	
	rule <k> $id(Name:String) => resolveIdentifier(Env, Name) ...</k>
		<lexEnv> Env:K </lexEnv>
		[identifier-resolution]
endmodule


module JS-BASIC-SEMANTICS 
  imports JS-IDENTIFIER-RESOLUTION 
endmodule