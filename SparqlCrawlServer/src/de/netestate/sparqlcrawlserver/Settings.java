package de.netestate.sparqlcrawlserver;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class Settings {
    private static String acceptHeader;
    private static String[] allowedContentTypes;
    private static long cacheLifetimeContentTooLongMillis;
    private static long cacheLifetimeHttpErrorMillis;
    private static long cacheLifetimeNetworkErrorMillis;
    private static long cacheLifetimeNotAllowedByRobotsMillis;
    private static long cacheLifetimeOkMillis;
    private static long cacheLifetimeParseErrorMillis;
    private static long cacheLifetimeWrongContentTypeMillis;
    private static int crawlThreads;
    private static long crawlThreadStackSize;
    private static int dnsThreads;
    private static long dnsThreadStackSize;
    private static long dnsTimeoutNanos;
    private static long datasetCacheLifetimeCompleteNanos;
    private static long datasetCacheLifetimeIncompleteNanos;
    private static Path logFilePath;
    private static int maxRdfXmlSize;
    private static int maxRobotsAge;
    private static int maxRobotsCacheSize;
    private static int maxRobotsSize;
    private static String modelCachePath;
    private static int socketTimeoutMillis;
    private static boolean unixOs;
    private static String userAgent;

    public static void load(final String settingsFile) throws Exception {
        final Binding binding = new Binding();
        final GroovyShell shell = new GroovyShell(binding);
        try (Reader reader = Files.newBufferedReader(Paths.get(settingsFile), Charset.forName("UTF-8"))) {
            shell.evaluate(reader);
        }
        allowedContentTypes = getStringArray(binding, "allowedContentTypes");
        cacheLifetimeContentTooLongMillis = (long) (getDouble(binding, "cacheLifetimeContentTooLong") * 1e3);
        cacheLifetimeHttpErrorMillis = (long) (getDouble(binding, "cacheLifetimeHttpError") * 1e3);
        cacheLifetimeNetworkErrorMillis = (long) (getDouble(binding, "cacheLifetimeNetworkError") * 1e3);
        cacheLifetimeParseErrorMillis = (long) (getDouble(binding, "cacheLifetimeParseError") * 1e3);
        cacheLifetimeNotAllowedByRobotsMillis = (long) (getDouble(binding, "cacheLifetimeNotAllowedByRobots") * 1e3);
        cacheLifetimeOkMillis = (long) (getDouble(binding, "cacheLifetimeOk") * 1e3);
        cacheLifetimeWrongContentTypeMillis = (long) (getDouble(binding, "cacheLifetimeWrongContentType") * 1e3);
        datasetCacheLifetimeCompleteNanos = (long) (getDouble(binding, "datasetCacheLifetimeComplete") * 1e9);
        datasetCacheLifetimeIncompleteNanos = (long) (getDouble(binding, "datasetCacheLifetimeIncomplete") * 1e9);
        logFilePath = Paths.get(getString(binding, "logFilePath"));
        crawlThreads = getInt(binding, "crawlThreads");
        crawlThreadStackSize = getLong(binding, "crawlThreadStackSize");
        dnsThreads = getInt(binding, "dnsThreads");
        dnsThreadStackSize = getLong(binding, "dnsThreadStackSize");
        dnsTimeoutNanos = (long) (getDouble(binding, "dnsTimeout") * 1e9);
        maxRdfXmlSize = getInt(binding, "maxRdfXmlSize");
        maxRobotsAge = getInt(binding, "maxRobotsAge");
        maxRobotsCacheSize = getInt(binding, "maxRobotsCacheSize");
        maxRobotsSize = getInt(binding, "maxRobotsSize");
        modelCachePath = getString(binding, "modelCachePath");
        socketTimeoutMillis = (int) (getDouble(binding, "socketTimeout") * 1e3);
        unixOs = getBoolean(binding, "unixOs");
        userAgent = getString(binding, "userAgent");
        final StringBuilder ah = new StringBuilder();
        for (final String ct: allowedContentTypes) {
            ah.append(ct);
            ah.append(',');
        }
        ah.append("*/*");
        acceptHeader = ah.toString();
    }

    public static String getAcceptHeader() {
        return acceptHeader;
    }

    public static String[] getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public static long getCacheLifetimeContentTooLongMillis() {
        return cacheLifetimeContentTooLongMillis;
    }

    public static long getCacheLifetimeHttpErrorMillis() {
        return cacheLifetimeHttpErrorMillis;
    }

    public static long getCacheLifetimeNetworkErrorMillis() {
        return cacheLifetimeNetworkErrorMillis;
    }

    public static long getCacheLifetimeNotAllowedByRobotsMillis() {
        return cacheLifetimeNotAllowedByRobotsMillis;
    }

    public static long getCacheLifetimeOkMillis() {
        return cacheLifetimeOkMillis;
    }

    public static long getCacheLifetimeParseErrorMillis() {
        return cacheLifetimeParseErrorMillis;
    }

    public static long getCacheLifetimeWrongContentTypeMillis() {
        return cacheLifetimeWrongContentTypeMillis;
    }

    public static long getDatasetCacheLifetimeCompleteNanos() {
        return datasetCacheLifetimeCompleteNanos;
    }

    public static long getDatasetCacheLifetimeIncompleteNanos() {
        return datasetCacheLifetimeIncompleteNanos;
    }

    public static long getDnsTimeoutNanos() {
        return dnsTimeoutNanos;
    }

    public static Path getLogFilePath() {
        return logFilePath;
    }

    public static int getCrawlThreads() {
        return crawlThreads;
    }

    public static long getCrawlThreadStackSize() {
        return crawlThreadStackSize;
    }

    public static int getDnsThreads() {
        return dnsThreads;
    }

    public static long getDnsThreadStackSize() {
        return dnsThreadStackSize;
    }

    public static int getMaxRdfXmlSize() {
        return maxRdfXmlSize;
    }

    public static int getMaxRobotsAge() {
        return maxRobotsAge;
    }

    public static int getMaxRobotsCacheSize() {
        return maxRobotsCacheSize;
    }

    public static int getMaxRobotsSize() {
        return maxRobotsSize;
    }

    public static String getModelCachePath() {
        return modelCachePath;
    }

    public static int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public static boolean getUnixOs() {
        return unixOs;
    }

    public static String getUserAgent() {
        return userAgent;
    }

    private static double getDouble(final Binding binding, final String name) {
        return ((Number) binding.getVariable(name)).doubleValue();
    }

    private static boolean getBoolean(final Binding binding, final String name) {
        return (boolean) binding.getVariable(name);
    }

    private static int getInt(final Binding binding, final String name) {
        return ((Number) binding.getVariable(name)).intValue();
    }

    private static long getLong(final Binding binding, final String name) {
        return ((Number) binding.getVariable(name)).longValue();
    }

    private static String getString(final Binding binding, final String name) {
        return (String) binding.getVariable(name);
    }

    private static String[] getStringArray(final Binding binding, final String name) {
        @SuppressWarnings("unchecked")
        final List<String> list = (List<String>) binding.getVariable(name);
        return list.toArray(new String[list.size()]);
    }

    private Settings() {
        throw new AssertionError("unreachable"); // no instances
    }
}
