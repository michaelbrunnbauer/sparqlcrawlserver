package de.netestate.sparqlcrawlserver;

import java.net.URI;
import java.net.URISyntaxException;

public final class RobotsCache {
    private final MemCache<String, RobotsDirectives> cache = new MemCache<>(Settings.getMaxRobotsCacheSize(),
            Settings.getMaxRobotsAge());

    private final LockSet<String> fetchLocks = new LockSet<>();
    private final Setup setup;

    public RobotsCache(final Setup setup) {
        this.setup = setup;
    }

    public boolean canFetch(final String url) throws Exception {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException ex) {
            return false;
        }
        final String scheme = uri.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme))
            return false;
        final String authority = uri.getRawAuthority();
        final String path = uri.getRawPath();
        RobotsDirectives directives = cache.get(authority);
        if (directives == null) {
            directives = fetchRobots(scheme, authority);
            cache.put(authority, directives);
        }
        return directives.canFetch(path);
    }

    private RobotsDirectives fetchRobots(final String scheme, final String authority) throws Exception {
        fetchLocks.lock(authority);
        try {
            RobotsDirectives directives = cache.get(authority);
            if (directives != null)
                return directives;
            return fetchRobots2(scheme, authority);
        } finally {
            fetchLocks.unlock(authority);
        }
    }

    private RobotsDirectives fetchRobots2(final String scheme, final String authority) throws Exception {
        final HttpResult httpResult = Http.fetch(setup, scheme + "://" + authority + "/robots.txt",
                Settings.getMaxRobotsSize());
        return RobotsParser.parse(Settings.getUserAgent(), httpResult.getBody(), httpResult.getStatusCode());
    }
}
