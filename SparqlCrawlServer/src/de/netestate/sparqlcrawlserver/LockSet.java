package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LockSet<E> {
    private final Map<E, LockEntry> entries = new Object2ReferenceOpenHashMap<>();

    private static final class LockEntry {
        private int refCount = 0;
        private Lock theLock = new ReentrantLock();

        private void addRef() {
            ++refCount;
        }

        private void lock() {
            theLock.lock();
        }

        private boolean release() {
            --refCount;
            return refCount > 0;
        }

        private void unlock() {
            theLock.unlock();
        }
    }

    public void lock(final E element) {
        LockEntry entry;
        synchronized (entries) {
            entry = entries.get(element);
            if (entry == null) {
                entry = new LockEntry();
                entries.put(element, entry);
            }
            entry.addRef();
        }
        entry.lock();
    }

    public void unlock(final E element) {
        LockEntry entry;
        synchronized (entries) {
            entry = entries.get(element);
            if (!entry.release())
                entries.remove(element);
        }
        entry.unlock();
    }
}
