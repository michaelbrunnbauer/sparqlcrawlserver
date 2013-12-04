package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.jena.riot.system.IRIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public final class DownloadTask implements Callable<DownloadResult> {
    private static final class RedirectException extends Exception {
        private static final long serialVersionUID = 1L;

        int statusCode;
        String location;

        RedirectException(final int statusCode, final String location) {
            this.statusCode = statusCode;
            this.location = location;
        }
    }

    private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    private static final String MBOX_SHA1_SUM_URI = FOAF_NS + "mbox_sha1sum";
    private static final int MAX_REDIRECTS = 5;

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DownloadTask.class);
    }

    private static boolean isIllegalUri(final Node node) {
        return node.isURI() && IRIResolver.checkIRI(node.getURI());
    }

    private static void readModel(final Model model, final HttpResult httpResult, final String url) {
        final InputStream inputStream = new ByteArrayInputStream(httpResult.getBody());
        final String contentType = httpResult.getContentType();
        if (Http.contentTypeMatches(contentType, "text/turtle"))
            model.read(inputStream, url, "TURTLE");
        else if (Http.contentTypeMatches(contentType, "text/n3"))
            model.read(inputStream, url, "N3");
        else {
            model.read(inputStream, url);
        }
    }
    private static void refineModel(final Model model) {
        final StmtIterator iter = model.listStatements();
        final List<Statement> statements = new ObjectArrayList<>();
        while (iter.hasNext())
            statements.add(iter.nextStatement());
        for (final Statement stmt: statements) {
            final String predicateUri = stmt.getPredicate().getURI();
            if (MBOX_SHA1_SUM_URI.equals(predicateUri))
                removeLangTagAndLower(stmt);
        }
    }

    private static void removeIllegalUris(final Model model) {
        final StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            final com.hp.hpl.jena.rdf.model.Statement stmt = iter.nextStatement();
            final Triple triple = stmt.asTriple();
            final Node subject = triple.getSubject();
            if (isIllegalUri(subject)) {
                iter.remove();
                continue;
            }
            final Node predicate = triple.getPredicate();
            if (isIllegalUri(predicate)) {
                iter.remove();
                continue;
            }
            final Node object = triple.getObject();
            if (isIllegalUri(object)) {
                iter.remove();
                continue;
            }
        }
    }

    private static void removeLangTagAndLower(final Statement stmt) {
        final RDFNode objNode = stmt.getObject();
        if (objNode.isLiteral()) {
            final Literal objLiteral = (Literal) objNode;
            final Object value = objLiteral.getValue();
            if (value instanceof String) {
                final String stringValue = (String) value;
                stmt.changeObject(stringValue.toLowerCase().trim()); // remove language
            }
        }
    }

    private volatile boolean cancelled = false;
    private final long requestId;
    private final Setup setup;
    private final String url;
    private final boolean ipIsKnown;
    private final InetAddress knownIp;

    public DownloadTask(final long requestId, final Setup setup, final String url) {
        this.requestId = requestId;
        this.setup = setup;
        this.url = url;
        ipIsKnown = false;
        knownIp = null;
    }

    public DownloadTask(final long requestId, final Setup setup, final String url, final InetAddress ip) {
        this.requestId = requestId;
        this.setup = setup;
        this.url = url;
        ipIsKnown = true;
        knownIp = ip;
    }

    @Override
    public DownloadResult call() throws Exception {
        final IpsInUse ipsInUse = setup.getIpsInUse();
        final InetAddress ip = getIp();
        if (ip == null) {
            putIntoCache(makeResult(ResultType.NETWORK_ERROR));
            return null;
        }
        while (!ipsInUse.tryUseIp(ip)) {
            if (cancelled)
                return null;
        }
        try {
            final DownloadResult result = run();
            putIntoCache(result);
            return result;
        } finally {
            ipsInUse.unuseIp(ip);
        }
    }

    public void cancel() {
        cancelled = true;
    }

    private boolean canFetch() throws Exception {
        final RobotsCache robotsCache = setup.getRobotsCache();
        return robotsCache.canFetch(url);
    }

    private InetAddress getIp() {
        if (ipIsKnown)
            return knownIp;
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost();
            return InetAddress.getByName(host);
        } catch (final Exception ex) {
            return null;
        }
    }

    private DownloadResult makeResult(final ResultType type) {
        return makeResult(type, 200);
    }

    private DownloadResult makeResult(final ResultType type, final int statusCode) {
        return new DownloadResult(type, url, url, null, statusCode);
    }

    private void putIntoCache(final DownloadResult result) throws Exception {
        final ResultType type = result.getType();
        final long lifetimeMillis;
        final ModelCacheEntry entry;
        final LogFile logFile = setup.getLogFile();
        switch (type) {
        case HTTP_ERROR:
            lifetimeMillis = Settings.getCacheLifetimeHttpErrorMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "HTTP " + result.getStatusCode(), url);
            break;
        case NETWORK_ERROR:
            lifetimeMillis = Settings.getCacheLifetimeNetworkErrorMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "Network Error", url);
            break;
        case PARSE_ERROR:
            lifetimeMillis = Settings.getCacheLifetimeParseErrorMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "Parse Error", url);
            break;
        case RESULT: {
            final byte[] data = serialize(result.getModel());
            if (data == null) {
                lifetimeMillis = Settings.getCacheLifetimeParseErrorMillis();
                entry = new ModelCacheEntry(true);
            } else {
                lifetimeMillis = Settings.getCacheLifetimeOkMillis();
                entry = new ModelCacheEntry(data, result.getActualUrl());
            }
            logFile.log(requestId, "OK", url);
            break;
        }
        case ROBOTS:
            lifetimeMillis = Settings.getCacheLifetimeNotAllowedByRobotsMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "Robots", url);
            break;
        case WRONG_CONTENT_TYPE:
            lifetimeMillis = Settings.getCacheLifetimeWrongContentTypeMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "Wrong Content-Type", url);
            break;
        case CONTENT_TOO_LONG:
            lifetimeMillis = Settings.getCacheLifetimeContentTooLongMillis();
            entry = new ModelCacheEntry(true);
            logFile.log(requestId, "Content Too Long", url);
            break;
        default:
            throw new AssertionError(type);
        }
        final Cache<String, ModelCacheEntry> modelCache = setup.getModelCache();
        modelCache.put(url, entry, lifetimeMillis);
    }

    private DownloadResult run() throws Exception {
        String url = this.url;
        int statusCode = 0;
        for (int n = 0; n < MAX_REDIRECTS; ++n) {
            try {
                return run(url);
            } catch (final RedirectException ex) {
                statusCode = ex.statusCode;
                url = ex.location;
            }
        }
        return makeResult(ResultType.HTTP_ERROR, statusCode);
    }

    private DownloadResult run(final String url) throws Exception {
        if (!canFetch())
            return makeResult(ResultType.ROBOTS);
        final HttpResult httpResult = Http.fetch(setup, url, Settings.getMaxRdfXmlSize(),
                Settings.getAllowedContentTypes());
        final ResultType resultType = httpResult.getResultType();
        if (resultType != ResultType.RESULT)
            return makeResult(resultType);
        final int statusCode = httpResult.getStatusCode();
        if (statusCode != 200) {
            if (statusCode >= 301 && statusCode <= 303) {
                final String location = httpResult.getLocation();
                if (location != null)
                    throw new RedirectException(statusCode, location);
            }
            return makeResult(ResultType.HTTP_ERROR, statusCode);
        }
        final Model model = ModelFactory.createDefaultModel();
        if (Http.contentTypeMatches(httpResult.getContentType(), "application/xhtml+xml", "text/html")) {
            final String lang;
            if ("application/xhtml+xml".equals(httpResult.getContentType()))
                lang = "XHTML";
            else
                lang = "HTML";
            try {
                model.read(new ByteArrayInputStream(httpResult.getBody()), url, lang);
            } catch (final Exception ex) {
                getLogger().info("Parse error for {}", url, ex);
                return makeResult(ResultType.PARSE_ERROR);
            }
        } else {
            try {
                readModel(model, httpResult, url);
            } catch (final Exception ex) {
                getLogger().info("Parse error for {}", url, ex);
                return makeResult(ResultType.PARSE_ERROR);
            }
        }
        removeIllegalUris(model);
        refineModel(model);
        return new DownloadResult(ResultType.RESULT, this.url, url, model, 200);
    }

    private byte[] serialize(final Model model) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            model.write(out, "N-TRIPLE");
        } catch (final Exception ex) {
            getLogger().info("Parse error for {}", url, ex);
            return null;
        }
        return out.toByteArray();
    }
}
