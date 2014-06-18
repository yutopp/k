// Copyright (c) 2013-2014 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.kframework.backend.java.kil.TermContext;


/**
 * Table of {@code public static} methods for builtin IO operations.
 */
public class BuiltinStuck {
    public static BoolToken stuck(TermContext context) {
        return BoolToken.of(context.state().isStuck);
    }
}