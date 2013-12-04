package de.netestate.sparqlcrawlserver;

import static de.netestate.sparqlcrawlserver.util.ServletRequestHelper.getDoubleParameter;
import static de.netestate.sparqlcrawlserver.util.ServletRequestHelper.getStringParameter;
import static java.lang.Double.isNaN;
import static java.lang.Math.min;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import de.netestate.sparqlcrawlserver.util.Iris;
import de.netestate.sparqlcrawlserver.util.Urls;

@WebServlet("/sparqlcrawl")
public class SparqlCrawlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        try {
            new SparqlCrawlServletRequest(request, response).doPost();
        } catch (final RuntimeException|ServletException|IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new ServletException(ex);
        }
	}
}

final class SparqlCrawlServletRequest {
    private static final long MAX_TIMEOUT = Long.MAX_VALUE / 4;

    private static final AtomicLong nextRequestId = new AtomicLong(1L);
    
    private static String iriToUrl(final String iri) {
        if (!Iris.isIri(iri) || !Urls.isValid(iri))
            return null;
        try {
            return Urls.normalizeUrl(Iris.toUri(Urls.normalizeIri(iri)));
        } catch (final URISyntaxException ex) {
            return null;
        }
    }

    private static void logIllegalUrl(final long requestId, final Setup setup, final String url) throws Exception {
        setup.getLogFile().log(requestId, "Illegal URL", url);
    }

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private List<String> urls;
    private Set<String> urlSet;

    SparqlCrawlServletRequest(final HttpServletRequest request, final HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    void doPost() throws Exception {
        final long requestId = nextRequestId.getAndIncrement();
        request.setCharacterEncoding("UTF-8");
        final long timeout = System.nanoTime() + getTimeout();
        final Setup setup = Setup.getInstance(request.getServletContext());
        final String query = getStringParameter(request, "query");
        final String graphs = getStringParameter(request, "graphs");
        getUrls(requestId, setup, IrisParameterParser.parse(graphs));
        final DatasetCache datasetCache = setup.getDatasetCache();
        Dataset dataset = datasetCache.get(urlSet);
        if (dataset == null) {
            final DatasetBuilder dataSetBuilder = new DatasetBuilder(requestId, setup);
            dataset = dataSetBuilder.build(urls, timeout);
            final long lifespan = dataSetBuilder.isComplete() ? Settings.getDatasetCacheLifetimeCompleteNanos()
                    : Settings.getDatasetCacheLifetimeIncompleteNanos();
            datasetCache.put(urlSet, dataset, System.nanoTime() + lifespan);
        }
        synchronized (dataset) {
            final QueryExecution queryExecution = QueryExecutionFactory.create(query, dataset);
            response.setContentType("application/sparql-results+json");
            final OutputStream out = response.getOutputStream();
            try {
                final ResultSet resultSet = queryExecution.execSelect();
                ResultSetFormatter.outputAsJSON(out, resultSet);
            } finally {
                queryExecution.close();
            }
            out.flush();
        }
    }

    private long getTimeout() {
        final double timeout = getDoubleParameter(request, "timeout");
        if (isNaN(timeout) || timeout < 0.0)
            throw new IllegalArgumentException("illegal timeout");
        return min((long) (timeout * 1e9), MAX_TIMEOUT);
    }

    private void getUrls(final long requestId, final Setup setup, final List<String> iris) throws Exception {
        urls = new ObjectArrayList<>(iris.size());
        urlSet = new ObjectOpenHashSet<>(urls);
        final Iterator<String> irisIt = iris.iterator();
        while (irisIt.hasNext()) {
            final String iri = irisIt.next();
            final String url = iriToUrl(iri);
            if (url == null)
                logIllegalUrl(requestId, setup, iri);
            else {
                if (!urlSet.contains(url)) {
                    urls.add(url);
                    urlSet.add(url);
                }
            }
        }
    }
}
