package org.kframework.backend.acl2;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.kframework.kil.loader.Context;

/**
 * Store a K context, and additional information used in the ACL2 backend
 */
public class Acl2Context {
	public final Context context;
	/**
	 * string name of the ACL2 package we've generated for the definition[
	 */
	private String acl2Package = null;
	/**
	 * string name of the ACL2 function that returns the grammar descriptions
	 */
	private String grammarName = null;
	public String getGrammarName() {
		if (grammarName == null) {
			throw new IllegalStateException("grammarName hasn't been set yet");
		}
		return grammarName;
	}
	public void setGrammarName(String grammarName) {
		if (this.grammarName != null) {
			throw new IllegalStateException(
					"grammarName already set to "+grammarName
					+" trying to change to "+grammarName);
		}
		this.grammarName = grammarName;
	}

	private Map<String, String> functionHooks;
	private final static Map<String,String> defaultFunctionHooks=
	        ImmutableMap.<String,String>builder()
	        .put("#INT:_+Int_","+Int")
	        .put("#INT:_=/=Int_","=/=Int")
	        .put("#INT:_/Int_","/Int")
	        .put("#INT:_<=Int_","<=Int")
	        .put("#BOOL:notBool_","not-bool")
	        .put("#SET:_inSet_","set-member")
	        .put("#MAP:keys", "k::map-keys")
	        .build();
	
	public Acl2Context(Context context) {
		this.context = context;
		functionHooks = new HashMap<String,String>(defaultFunctionHooks);
	}
	
	/** Package name to use in lisp code */
	public String getAcl2Package() {
		return acl2Package;
	}

	/** Set package name to use in generated lisp code.
	 * May only be called once.
	 */
	public void setAcl2Package(String pkg) {
		if (acl2Package == null) {
			acl2Package = pkg;
		} else {
			throw new IllegalStateException(
					"ACL2 package was already set to "+acl2Package+", trying to set to "+pkg);
		}
	}
	
	/** Return the package-qualified name of the ACL2 function implementing a hook,
	 * or null if none is registered. */
	public String getHookImplementation(String hook) {
		return functionHooks.get(hook);
	}
	
	/** Bind a hook to a lisp function generated in the module.
	 * Unqualified names will be resolved with respect to the output package.
	 */
	public void registerHookImplementation(String hook, String impl) {
		if (functionHooks.containsKey(hook)) {
			String bound = functionHooks.get(hook);
			throw new IllegalStateException(
					"Hook "+hook+" already bound to "+bound+", trying to set to "+impl);
		}
		functionHooks.put(hook, impl);
	}

}
