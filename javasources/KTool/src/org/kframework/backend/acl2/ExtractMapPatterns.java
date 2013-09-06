package org.kframework.backend.acl2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.kframework.backend.provers.ast.BasicKVisitor;
import org.kframework.backend.provers.ast.Cell;
import org.kframework.backend.provers.ast.CellTransformer;
import org.kframework.backend.provers.ast.FindVariables;
import org.kframework.backend.provers.ast.K;
import org.kframework.backend.provers.ast.KItem;
import org.kframework.backend.provers.ast.KVariable;
import org.kframework.backend.provers.ast.Map;
import org.kframework.backend.provers.ast.Transformer;
import org.kframework.compile.utils.DependencyOrderQueue;
import org.kframework.compile.utils.NameSupply;

/**
 * Walk a term replacing non-empty map patterns with simple variables,
 * recursing into values of map items and asserting that
 * the keys of map items do not contain map patterns.
 * 
 * Generated variable will be distinct from any names already
 * used in the supplied terms.
 * 
 * Returns a pair of the transformed term and a map from
 * the new variable names to the Map patterns they abstract. 
 */
public class ExtractMapPatterns {


    public static Pair<Cell, List<Acl2SeqmatchItem>>
        extractMapPatterns(Cell pattern) {

        final java.util.Map<String, Map> extractedPatterns = new HashMap<String, Map>();

        final NameSupply names = new NameSupply(FindVariables.getVariables(pattern));

        final Transformer ktransformer = new Transformer() {
            public KItem visit(final Map map) {
                if (map.items.isEmpty() && map.rest == null) {
                    return map;
                }
                String replacement = names.fresh("mapPat");
                Builder<KItem, KItem> builder = ImmutableMap.builder();
                for (Entry<KItem, KItem> entry : map.items.entrySet()) {
                    final KItem key = entry.getKey();
                    key.accept(new BasicKVisitor() {
                        public Void visit(Map map) {
                            throw new IllegalArgumentException(
                                    "Map lookup key " + key
                                            + " contains a map lookup");
                        };
                    });
                    builder.put(key, entry.getValue().accept(this));
                }
                extractedPatterns.put(replacement, new Map(builder.build(),
                        map.rest));
                return new KVariable(replacement, "Map");
            }
        };
        Cell transformed = pattern.accept(new CellTransformer() {
            @Override
            public K visit(K kseqItem) {
                return kseqItem.accept(ktransformer);
            }
        });

        /*
         * Now that the map patterns have been isolated we need to order then.
         * Individual bindings in a map pattern become active when all variables
         * from the left hand side are active.
         * 
         * For simplicity we tie
         */
        
        DependencyOrderQueue<String, PendingMapItem> dependencies
         = new DependencyOrderQueue<String, PendingMapItem>();
        dependencies.provideKeys(FindVariables.getVariables(transformed));
        
        for (Entry<String,Map> e : extractedPatterns.entrySet()) {
            PendingMapPattern patternInfo =
                    new PendingMapPattern(
                            e.getValue().items.size(),
                            e.getKey(),
                            e.getValue().rest);
            for (Entry<KItem,KItem> item : e.getValue().items.entrySet()) {
                PendingMapItem pendingItem =
                        new PendingMapItem(item.getKey(), item.getValue(), patternInfo);
                Set<String> patternVars = FindVariables.getVariables(item.getKey());                
                // Add the name of the original map to the dependencies.
                // In the output, all the items from a pattern but the first
                // will perform their lookup in the "rest" variable from
                // the earlier lookup rather than the original name,
                // but in either case we shouldn't let any items be
                // emitted until the original map is available,
                // and after the original map is available, having
                // a map to perform the lookup in isn't an issue for later items
                patternVars.add(e.getKey());
                dependencies.addItem(patternVars, pendingItem);
            }
        }
        
        ArrayList<Acl2SeqmatchItem> ordered = new ArrayList<Acl2SeqmatchItem>();
        while (true) {
            PendingMapItem readyItem = dependencies.removeReady();
            if (readyItem == null) {
                break;
            }
            readyItem.parentPattern.itemsOutstaning -= 1;
            String mapName = readyItem.parentPattern.mapName;
            String tempName = names.fresh("mapTemp");
            String restName;
            if (readyItem.parentPattern.itemsOutstaning > 0) {
                restName = names.fresh("mapRest");
                readyItem.parentPattern.mapName = restName;
            } else {
                restName = readyItem.parentPattern.bodyName;
            }
            dependencies.provideKeys(FindVariables.getVariables(readyItem.value));
            ordered.add(
                    new Acl2SeqmatchItem(
                            mapName,
                            readyItem.key,
                            tempName,
                            readyItem.value,
                            restName));
        }
        return Pair.of(transformed, (List<Acl2SeqmatchItem>)ordered);
    }

    /*
     * Helper classes in the implementation of pattern ordering.
     */
    /*
     * Used to coordinate the items which originated from a single
     * map pattern.
     * 
     * Will be instantiated with the item count of the map pattern,
     * the name of the map variable to be inspected by the pattern,
     * and the body.
     * As each item becomes ready the count is decremented and
     * the mapName is replaced by the (fresh) name binding the
     * remaining elements after binding that item.
     * The last item just uses the body name from the original pattern.
     */
    private static class PendingMapPattern {
        int itemsOutstaning;
        String mapName;
        String bodyName;
        public PendingMapPattern(int itemsOutstaning, String mapName,
                String bodyName) {
            this.itemsOutstaning = itemsOutstaning;
            this.mapName = mapName;
            this.bodyName = bodyName;
        }        
    }
    /**
     * Represents a single item from a map pattern.
     */
    private static class PendingMapItem {
        KItem key;
        KItem value;
        PendingMapPattern parentPattern;
        public PendingMapItem(KItem key, KItem value,
                PendingMapPattern parentPattern) {
            this.key = key;
            this.value = value;
            this.parentPattern = parentPattern;
        }        
    }    
}
