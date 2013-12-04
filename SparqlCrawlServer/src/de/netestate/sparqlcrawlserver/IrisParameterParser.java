package de.netestate.sparqlcrawlserver;

import static java.lang.Character.charCount;
import static java.lang.Character.isWhitespace;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public final class IrisParameterParser {
    private static final String IRIS_SYNTAX_ERROR = "iris syntax error";

    public static List<String> parse(final String input) {
        final List<String> iris = new ObjectArrayList<>();
        int p = skipWhite(input, 0);
        while (p < input.length()) {
            p = parse(input, p, iris);
            p = skipWhite(input, p);
        }
        return iris;
    }

    private static int parse(final String input, int p, final List<String> iris) {
        if (!iris.isEmpty()) {
            p = skip(input, p, ',');
            p = skipWhite(input, p);
        }
        p = skip(input, p, '<');
        final int q = input.indexOf('>', p);
        if (q < 0)
            throw new RuntimeException(IRIS_SYNTAX_ERROR);
        final String iri = input.substring(p, q);
        iris.add(iri);
        return q + 1;
    }

    private static int skip(final String input, final int p, final int cp) {
        if (p >= input.length() || input.codePointAt(p) != cp)
            throw new RuntimeException(IRIS_SYNTAX_ERROR);
        return p + charCount(cp);
    }

    private static int skipWhite(final String input, int p) {
        while (p < input.length()) {
            final int cp = input.codePointAt(p);
            if (!isWhitespace(cp))
                break;
            p += charCount(cp);
        }
        return p;
    }

    private IrisParameterParser() {
        throw new AssertionError("unreachable"); // no instances
    }
}
