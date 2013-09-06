package org.kframework.backend.acl2;

import java.io.StringWriter;

import org.kframework.kil.Definition;

/**
 * Pairs a K definition with append-only lisp output,
 * so ACL2 backend passes can incrementally transform
 * K definitions to lisp.
 * Passes are expected to transform the KIL code to
 * refer through hooks to the generated lisp.
 * @author brandon
 */
public class Acl2Definition {
	private Definition definition;
	private StringWriter lispOutput;
	
	public Acl2Definition(Definition definition) {
		this.definition = definition;
		lispOutput = new StringWriter();		
	}

	public Definition getDefinition() {
		return definition;
	}

	public void setDefinition(Definition definition) {
		this.definition = definition;
	}
	
	public StringWriter getLispWriter() {
		return lispOutput;
	}
	
}
