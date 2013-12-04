package de.netestate.sparqlcrawlserver;

public final class PythonRepr {
    public static String repr(final String s) {
        if (s == null)
            return "None";
        final int length = s.length();
        final StringBuilder result = new StringBuilder(length + 2);
        result.append('\'');
        for (int n = 0; n < length; ++n) {
            final char c = s.charAt(n);
            switch (c) {
            case '\'':
                result.append("\\'");
                break;
            case '\\':
                result.append("\\\\");
                break;
            default:
                if (c >= ' ' && c <= '~')
                    result.append(c);
                else
                    result.append(String.format("\\u%04x", (int) c));
            }
        }
        result.append('\'');
        return result.toString();
    }

    private PythonRepr() {
        throw new AssertionError("unreachable"); // no instances
    }
}
