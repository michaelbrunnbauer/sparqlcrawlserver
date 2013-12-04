package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public final class MemCache<K, V> {
    final Map<K, V> map = new Object2ObjectOpenHashMap<>();
    long born = System.nanoTime();
    final int maxAge;
    final int maxSize;

    public MemCache(final int maxSize, final int maxAge) {
        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    public synchronized V get(final K key) {
        checkAge();
        return map.get(key);
    }

    public synchronized void put(final K key, final V value) {
        checkSize();
        map.put(key, value);
    }

    private void checkAge() {
        final long now = System.nanoTime();
        long age = now - born;
        if (age >= 0)
            age /= 1000000000L;
        else
            age = Integer.MAX_VALUE;
        if (age >= maxAge) {
            map.clear();
            born = now;
        }
    }

    private void checkSize() {
        if (map.size() >= maxSize) {
            map.clear();
            born = System.nanoTime();
        }
    }
}
