package de.netestate.sparqlcrawlserver;

import com.hp.hpl.jena.rdf.model.Model;

public final class DownloadResult {
    private ResultType type;
    private final String url;
    private final String actualUrl;
    private final Model model;
    private final int statusCode;

    public DownloadResult(final ResultType type, final String url, final String actualUrl, final Model model,
            final int statusCode) {
        this.type = type;
        this.url = url;
        this.actualUrl = actualUrl;
        this.model = model;
        this.statusCode = statusCode;
    }

    public ResultType getType() {
        return type;
    }

    public String getActualUrl() {
        return actualUrl;
    }

    public String getUrl() {
        return url;
    }

    public Model getModel() {
        return model;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
