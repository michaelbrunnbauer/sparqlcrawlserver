package de.netestate.sparqlcrawlserver;

public final class HttpResult {
    private final ResultType resultType;
    private final int statusCode;
    private final String contentType;
    private final long contentLength;
    private final byte[] body;
    private final String location;

    public HttpResult(final ResultType resultType, final int statusCode, final String contentType,
            final long contentLength, final byte[] body, final String location) {
        this.resultType = resultType;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.body = body;
        this.location = location;
    }

    public HttpResult(final ResultType resultType) {
        this(resultType, 0, null, -1, null, null);
    }

    public ResultType getResultType() {
        return resultType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public byte[] getBody() {
        return body;
    }

    public String getLocation() {
        return location;
    }
}
