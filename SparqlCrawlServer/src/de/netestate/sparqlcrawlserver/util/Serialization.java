package de.netestate.sparqlcrawlserver.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public final class Serialization {
    public static byte[] serialize(final Object obj) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(obj);
        objOut.flush();
        return out.toByteArray();
    }

    private Serialization() {
        throw new AssertionError("unreachable"); // no instances
    }
}
