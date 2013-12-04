package de.netestate.sparqlcrawlserver.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

public final class SoftValueMap<K, V> {
    private static final class ValueRef<K, V> extends SoftReference<V> {
        private K key;

        public ValueRef(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        public K getKey() {
            return key;
        }
    }

    private final Map<K, ValueRef<K, V>> map = new Object2ReferenceOpenHashMap<>();
    private final ReferenceQueue<V> refQueue = new ReferenceQueue<>();

    public V get(K key) {
        synchronized (map) {
            purge();
            final ValueRef<K, V> ref = map.get(key);
            return ref == null ? null : ref.get();
        }
    }

    public void put(K key, V value) {
        synchronized (map) {
            purge();
            map.put(key, new ValueRef<>(key, value, refQueue));
        }
     }

    private void purge() {
        for (;;) {
            final ValueRef<?, ?> ref = (ValueRef<?, ?>) refQueue.poll();
            if (ref == null)
                break;
            final Object key = ref.getKey();
            if (map.get(key) == ref)
                map.remove(key);
        }
    }
}
