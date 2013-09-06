package org.kframework.compile.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate fresh names.
 * Tracks a set of used names, and generates names fresh with respect to this set.
 */
public class NameSupply {
    private Set<String> used;
    
    /**
     * Create a NameSupply starting with no unused variables.
     */
    public NameSupply() {
        used = new HashSet<String>();
    }
    
    /**
     * Create a NameSupply initialized with the given set of used variables. 
     */
    public NameSupply(Collection<String> initiallyUsed) {
        used = new HashSet<String>(initiallyUsed);
    }
    
    /**
     * Mark a set of variables as used.
     * This should not be called after fresh.
     */
    public void useVariables(Collection<String> additionalUsed) {
        used.addAll(additionalUsed);
    }
    
    public String fresh() {
        return fresh("FreshVar");
    }
    
    public String fresh(String base) {
        String name = pickFresh(base);
        used.add(name);
        return name;
    }
    
    private String pickFresh(String base) {
        if (!used.contains(base)) {
            return base;
        }
        int i = 0;
        while (true) {
            String test = base+i;
            if (!used.contains(test)) {
                return test;
            }
        }
    }
}
