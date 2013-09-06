package org.kframework.backend.provers.ast;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class FindVariablesTest {

    @Test
    public void testVariable() {
        assertEquals(ImmutableSet.of("Name"),
                FindVariables.getVariables(new KVariable("Name", "Sort")));
    }
    
    @Test
    public void testEmpty() {
        assertEquals(ImmutableSet.of(),
                FindVariables.getVariables(new KApp("Fun",ImmutableList.<KItem>of())));
    }
    
    @Test
    public void testMap() {
        assertEquals(ImmutableSet.of("K","V","B"),                
                FindVariables.getVariables(new Map(
                        ImmutableMap.<KItem, KItem>of(
                                new KVariable("K","K"),
                                new KVariable("V","K")),
                                "B")));
    }
}
