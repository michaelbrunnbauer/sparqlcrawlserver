package de.netestate.sparqlcrawlserver;

public final class RobotsDirectives {
    private final String[] disallowPrefixes;

    RobotsDirectives(final String[] disallowPrefixes) {
        this.disallowPrefixes = disallowPrefixes;
    }

    public boolean canFetch(String path) {
        if (path == null || path.isEmpty())
            path = "/";
        for (final String prefix: disallowPrefixes) {
            if (path.startsWith(prefix))
                return false;
        }
        return true;
    }
}
