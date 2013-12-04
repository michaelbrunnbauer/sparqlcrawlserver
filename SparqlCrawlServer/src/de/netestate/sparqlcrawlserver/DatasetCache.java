package de.netestate.sparqlcrawlserver;

import java.util.Set;

import com.hp.hpl.jena.query.Dataset;

import de.netestate.sparqlcrawlserver.util.SoftValueMap;

public final class DatasetCache {
    private static final class Entry {
        final Dataset dataset;
        final long lifespan;
        
        Entry(final Dataset dataset, final long lifespan) {
            this.dataset = dataset;
            this.lifespan = lifespan;
        }
    }

    private final SoftValueMap<Set<String>, Entry> cache = new SoftValueMap<>();

    public void put(final Set<String> graphs, final Dataset dataset, final long lifespan) {
        final Entry entry = new Entry(dataset, lifespan);
        cache.put(graphs, entry);
    }

    public Dataset get(final Set<String> graphs) {
        final Entry entry = cache.get(graphs);
        if (entry == null || System.nanoTime() - entry.lifespan >= 0)
            return null;
        return entry.dataset;
    }
}
