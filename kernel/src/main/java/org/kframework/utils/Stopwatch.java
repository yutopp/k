// Copyright (c) 2014-2015 K Team. All Rights Reserved.
package org.kframework.utils;

import org.kframework.main.GlobalOptions;
import org.kframework.utils.inject.RequestScoped;

import com.google.inject.Inject;
import java.util.Formatter;

/**
 * To use, access {@link #instance()} after calling {@link #init(GlobalOptions) init()}.
 */
@RequestScoped
public class Stopwatch {
    private long start;
    private long lastIntermediate;
    Formatter f = new Formatter(System.out);
    private final boolean verbose;

    @Inject
    public Stopwatch(GlobalOptions options) {
        this.verbose = options.verbose;
        start = System.currentTimeMillis();
        lastIntermediate = start;
    }

    public Stopwatch(boolean verbose) {
        this.verbose = verbose;
        start = System.currentTimeMillis();
        lastIntermediate = start;
    }

    public void start() {
        printIntermediate("Init");
    }

    public void printIntermediate(String message) {
        long current = System.currentTimeMillis();
        if (verbose)
            f.format("%-60s = %5d%n", message, current - lastIntermediate);
        lastIntermediate = current;
    }

    public void printTotal(String message) {
        printIntermediate("Cleanup");
        if (verbose)
            f.format("%-60s = %5d%n", message, lastIntermediate - start);
    }

    public long getIntermediateMilliseconds() {
        long endd = System.currentTimeMillis();
        long rez = lastIntermediate - endd;
        lastIntermediate = endd;
        return rez;
    }
}
