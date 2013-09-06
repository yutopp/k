package org.kframework.backend.acl2;

import org.kframework.backend.provers.ast.KItem;

/**
 * Represents a submatch in an ACL2 seqmatch case.
 * 
 * As part of a case pattern, an item like this evaluates
 * an expression with access to the values bound so far,
 * binds the result to a temporary name, and matches
 * that name against a further pattern.
 * 
 * matched is the expression being matched
 * tempName is a name for the result of the expression
 * pattern is the pattern the result is matched against
 */
public class Acl2SeqmatchItem {
    public final String matched;
    public final KItem key;
    public final String tempName;
    public final KItem value;
    public final String rest;
    public Acl2SeqmatchItem(String matched, KItem key, String tempName,
            KItem value, String rest) {
        super();
        this.matched = matched;
        this.key = key;
        this.tempName = tempName;
        this.value = value;
        this.rest = rest;
    }
}
