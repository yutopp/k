package org.kframework.backend.provers.ast;

public final class CellContentVariable extends CellContent {
    public final String name;

    public CellContentVariable(String name) {
        super();
        this.name = name;
    }
    
    public String toString() {
        return "CellContentVariable["+name+"]";
    }    
}
