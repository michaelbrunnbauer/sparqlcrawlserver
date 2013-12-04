package de.netestate.sparqlcrawlserver.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.riot.system.IRIResolver;

public final class Urls {
    public static boolean isValid(final String url) {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException ex) {
            return false;
        }
        final String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            return false;
        if (uri.getAuthority() == null)
            return false;
        String path = uri.getPath();
        if (path == null)
            path = "/";
        final int length = url.length();
        for (int n = 0; n < length; ++n) {
            final char c = url.charAt(n);
            if (c == ' ' || c == '\r' || c == '\n' || c == '\\')
                return false;
        }
        return true;
    }

    public static String normalizeIri(final String url) throws URISyntaxException {
        final URI uri = new URI(url);
        final String scheme = uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme))
            throw new URISyntaxException(url, "scheme");
        String path = uri.getPath();
        if (path == null || path.isEmpty())
            path = "/";
        String auth = uri.getAuthority();
        while (auth.endsWith("."))
            auth = auth.substring(0, auth.length() - 1);
        return new URI(scheme, auth, path, uri.getQuery(), null).toString();
    }

    public static String normalizeUrl(String url) {
        if (url == null)
            return null;
        try {
            @SuppressWarnings("unused") // only check for exception
            final URI uri = new URI(url);
        } catch (final URISyntaxException ex) {
            return null;
        }
        url = normalizeUrl2(url);
        if (url == null)
            return null;

        // fragment entfernen
        int p = url.indexOf('#');
        if (p >= 0)
            url = url.substring(0, p);

        if (repeatedSegementsInUrl(url))
            return null;
        if (IRIResolver.checkIRI(url))
            return null;
        return ensureNonemptyUrlPath(url);
    }

    private static String ensureNonemptyUrlPath(String url) {
        if (url == null)
            return null;
        final URI uri = URI.create(url);
        if (uri.getPath().isEmpty()) {
            url = uri.getScheme() + "://" + uri.getRawAuthority() + '/';
            if (uri.getRawQuery() != null)
                url += '?' + uri.getQuery();
        }
        return url;
    }

    private static boolean isIpAddress(final String host) {
        return host.indexOf('[') >= 0 ||
               host.matches("[0-9]++.[0-9]++.[0-9]++.[0-9]++");
    }

    private static String normalizeAuth(String auth, String scheme) {
        auth = auth.toLowerCase();
        int p = auth.lastIndexOf(':');
        final String auth1 = p >= 0 ? auth.substring(0, p) : auth;
        if (isIpAddress(auth1))
            return null;
        if (p >= 0) {
            String port = auth.substring(p + 1);
            try {
                int porti = Integer.parseInt(port);
                if (porti >= 1024)
                    return auth;
                if (porti == 80 && scheme.equals("http://"))
                    return auth1;
                if (porti == 443 && scheme.equals("https://"))
                    return auth1;
                return null;
            } catch (final NumberFormatException ex) {
                return auth;
            }
        }
        return auth;
    }

    private static String normalizeRemainder(String rem) {
        int p = rem.indexOf('%');
        while (p >= 0) {
            if (rem.length() >= p + 3) {
                if (rem.length() > p + 3) {
                    rem = rem.substring(0, p) + rem.substring(p, p + 3).toUpperCase()
                            + rem.substring(p + 3);
                } else {
                    rem = rem.substring(0, p) + rem.substring(p, p + 3).toUpperCase();
                }
            }

            if (rem.length() > p) {
                p = rem.indexOf('%', p + 1);
            } else {
                p = -1;
            }
        }
        return rem;
    }

    private static String normalizeUrl2(String url) {
        String scheme;
        if (url.toLowerCase().startsWith("http://")) {
            scheme = url.substring(0, 7).toLowerCase();
            url = url.substring(7);
        } else if (url.toLowerCase().startsWith("https://")) {
            scheme = url.substring(0, 8).toLowerCase();
            url = url.substring(8);
        } else {
            return null;
        }

        String auth;
        int p = url.indexOf('/');
        if (p < 0)
            p = url.indexOf('?');
        if (p < 0) {
            auth = url;
            url = "";
        } else {
            auth = url.substring(0, p);
            url = url.substring(p);
        }
        auth = normalizeAuth(auth, scheme);
        if (auth == null)
            return null;
        return scheme + auth + normalizeRemainder(url);
    }

    private static boolean repeatedSegementsInUrl(final String url) {
        if (url.indexOf("?view=foaf?view=foaf") >= 0)
            return true;
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException ex) {
            return false;
        }
        final String path = uri.getPath();
        if (path == null)
            return false;
        final Map<String, Integer> pathCounts = new HashMap<>();
        for (final String c: path.split("/", -1)) {
            final Integer countBoxed = pathCounts.get(c);
            final int count = countBoxed == null ? 0 : countBoxed;
            if (count >= 2)
                return true;
            pathCounts.put(c, count + 1);
        }
        final String query = uri.getQuery();
        if (query == null)
            return false;
        final Map<String, Integer> queryCounts = new HashMap<>();
        for (final String c: query.split("&", -1)) {
            final Integer countBoxed = queryCounts.get(c);
            final int count = countBoxed == null ? 0 : countBoxed;
            if (count >= 1)
                return true;
            queryCounts.put(c, count + 1);
        }
        return false;
    }

    private Urls() {
        throw new AssertionError("unreachable"); // no instances
    }
}
