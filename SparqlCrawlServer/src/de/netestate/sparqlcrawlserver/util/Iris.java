package de.netestate.sparqlcrawlserver.util;

import org.apache.commons.codec.binary.StringUtils;

public final class Iris {
    public static boolean isIri(final String iri) {
        final int iriLength = iri.length();
        for (int n = 0; n < iriLength; ++n) {
            final char c = iri.charAt(n);
            if (c <= ' ' || "<>\\\"".indexOf(c) >= 0)
                return false;
        }
        return true;
    }

    public static String toUri(final String iri) {
        final byte[] bytes = StringUtils.getBytesUtf8(iri);
        final StringBuilder uri = new StringBuilder(bytes.length);
        for (final int b: bytes) {
            if (b >= 0)
                uri.append((char) b);
            else {
                uri.append('%');
                uri.append(String.format("%02X", b & 0xFF));
            }
        }
        return uri.toString();
    }

    private Iris() {
        throw new AssertionError("unreachable"); // no instances
    }
}
