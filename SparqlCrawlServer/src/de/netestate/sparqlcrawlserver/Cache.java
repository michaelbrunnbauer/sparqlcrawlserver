package de.netestate.sparqlcrawlserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

import de.netestate.sparqlcrawlserver.util.Serialization;

public final class Cache<K, V> {
    private final Object mutex = new Object();
    private final String folder;

    public Cache(final String folder) {
        this.folder = folder;
    }

    public V get(final K key) throws Exception {
        final byte[] skey = Serialization.serialize(key);
        final Path keyPath = getKeyPath(skey);
        synchronized (mutex) {
            if (!Files.exists(keyPath))
                return null;
            try (InputStream fin = Files.newInputStream(keyPath)) {
                final InputStream bin = new BufferedInputStream(fin);
                final InputStream gzin = new GZIPInputStream(bin);
                final ObjectInputStream objIn = new ObjectInputStream(gzin);
                final Object key2 = objIn.readObject();
                if (!key2.equals(key))
                    throw new AssertionError("Hash collision");
                final long validUntil = objIn.readLong();
                if (validUntil >= System.currentTimeMillis()) {
                    @SuppressWarnings("unchecked")
                    final V readObject = (V) objIn.readObject();
                    return readObject;
                }
            }
            Files.delete(keyPath);
            return null;
        }
    }

    private Path getKeyPath(final byte[] skey) {
        final String sha = DigestUtils.sha1Hex(skey);
        return Paths.get(folder, sha.substring(0, 2), sha.substring(2, 4), sha.substring(4));
    }

    public void put(final K key, final V value, final long lifetimeMillis) throws Exception {
        final byte[] skey = Serialization.serialize(key);
        final Path keyPath = getKeyPath(skey);
        final Path parent = keyPath.getParent();
        final Path tempPath = parent.resolve("_tmp_");
        synchronized (mutex) {
            if (!Settings.getUnixOs())
                Files.deleteIfExists(keyPath);
            Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(tempPath)) {
                final OutputStream bout = new BufferedOutputStream(out);
                final GZIPOutputStream gzout = new GZIPOutputStream(bout);
                final ObjectOutputStream objOut = new ObjectOutputStream(gzout);
                objOut.writeObject(key);
                final long validUntil = System.currentTimeMillis() + lifetimeMillis;
                objOut.writeLong(validUntil);
                objOut.writeObject(value);
                objOut.flush();
                gzout.finish();
                gzout.flush();
                bout.flush();
            }
            Files.move(tempPath, keyPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
