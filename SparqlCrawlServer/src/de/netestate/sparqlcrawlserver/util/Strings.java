package de.netestate.sparqlcrawlserver.util;

public final class Strings {
    public static boolean isAscii(final String s) {
        final int length = s.length();
        for (int n = 0; n < length; ++n) {
            if (s.charAt(n) >= 128)
                return false;
        }
        return true;
    }

    private Strings() {
        throw new AssertionError("unreachable"); // no instances
    }
}
