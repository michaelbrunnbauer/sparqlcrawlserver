package de.netestate.sparqlcrawlserver.util;

import javax.servlet.http.HttpServletRequest;

public final class ServletRequestHelper {
    public static double getDoubleParameter(final HttpServletRequest request, final String name) {
        final String valueStr = getStringParameter(request, name);
        try {
            return Double.parseDouble(valueStr);
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("parameter " + name, ex);
        }
    }

    public static String getStringParameter(final HttpServletRequest request, final String name) {
        final String value = request.getParameter(name);
        if (value == null)
            throw new IllegalArgumentException("missing parameter " + name);
        return value;
    }

    private ServletRequestHelper() {
        throw new AssertionError("unreachable"); // no instances
    }
}
