package org.kframework.backend.provers.ast;

public class TokenBuiltin extends KItem {
	public final String sort;
    public final String value;    
    public TokenBuiltin(String sort, String value) {
		super();
		this.sort = sort;
		this.value = value;
	}

	@Override
    public <R> R accept(KItemVisitor<R> visitor) {
        return visitor.visit(this);
    }

	@Override
	public String toString() {
		return "TokenBuiltin [sort=" + sort + ", value=" + value + "]";
	}
}