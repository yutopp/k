package org.kframework.compile.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

/**
 * Maintain a collection of values each waiting for
 * a set of dependencies, and a set of available dependencies,
 * with operations to satisfy more dependencies and dequeue
 * items whose dependencies are all satisfied.
 *
 * This can be used to construct a topological sort if the values
 * can somehow be considered to also satisfy a set of dependencies
 * when their dependencies are satisfies, by adding the set of
 * newly satisfied dependencies after each item is removed.
 * 
 * @author BrandonM
 * @param <Key> Dependencies
 * @param <A>   Values
 */
public class DependencyOrderQueue<Key, A> {
    private static class Entry<Key, A> {
        HashSet<Key> neededKeys;
        A value;
        Entry(HashSet<Key> neededKeys, A value) {
            this.neededKeys = neededKeys;
            this.value = value;
        }
    }

    private Set<Key> availableKeys;
    private Map<Key, ArrayList<Entry<Key, A>>> pendingItems;
    private ArrayDeque<A> readyItems;

    public DependencyOrderQueue() {
        availableKeys = Sets.newHashSet();
        pendingItems = Maps.newHashMap();
        readyItems = Queues.newArrayDeque();
    }
    
    /** Add a new value awaiting the given set of dependencies */
    public void addItem(Collection<? extends Key> dependencies, A value) {
        HashSet<Key> unresolved = Sets.newHashSet(dependencies);
        unresolved.removeAll(availableKeys);
        if (unresolved.isEmpty()) {
            readyItems.add(value);
        } else {
            Key someKey = unresolved.iterator().next();
            Entry<Key,A> entry = new Entry<Key,A>(unresolved, value);
            putEntry(someKey, entry);
        }
    }
    
    private void putEntry(Key key, Entry<Key, A> entry) {
        ArrayList<Entry<Key, A>> list = pendingItems.get(key);
        if (list == null) {
            list = Lists.newArrayList();
            pendingItems.put(key, list);                
        }
        list.add(entry);
    }
    
    /**
     * Reindex or mark available all items indexed under key,
     * which must already have been added to availableKeys
     * @param key
     */
    protected void releaseKey(Key key) {
        ArrayList<Entry<Key, A>> list = pendingItems.remove(key);        
        if (list != null) {
            for (Entry<Key, A> entry : list) {
                entry.neededKeys.removeAll(availableKeys);
                if (entry.neededKeys.isEmpty()) {
                    readyItems.add(entry.value);
                } else {
                    putEntry(entry.neededKeys.iterator().next(), entry);
                }
            }            
        }
    }
    
    /** Make a single new dependency available */
    public void provideKey(Key key) {
        availableKeys.add(key);
        releaseKey(key);
    }

    /** Make a collection of dependencies available */
    public void provideKeys(Collection<? extends Key> keys) {
        availableKeys.addAll(keys);
        for (Key k : keys) {
            provideKey(k);
        }
    }

    /** Returns true if any values have all dependencies satisfied */
    public boolean anyReady() {
        return !readyItems.isEmpty();
    }
    
    /** Return and dequeue an item which has all dependencies satisfied,
     * or null if there is none.
     */
    public A removeReady() {
        return readyItems.pollFirst();
    }
}
