package de.netestate.sparqlcrawlserver;

public final class IriUrl {
    private final String iri;
    private final String url;

    public IriUrl(final String iri, final String url) {
        this.iri = iri;
        this.url = url;
    }

    public String getIri() {
        return iri;
    }

    public String getUrl() {
        return url;
    }
}
