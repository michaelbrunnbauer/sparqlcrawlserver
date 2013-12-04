package de.netestate.sparqlcrawlserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Http {
    public static boolean contentTypeMatches(final String ct, final String... contentTypes) {
        if (ct == null)
            return false;
        for (final String contentType: contentTypes) {
            if (ct.startsWith(contentType))
                return true;
        }
        return false;
    }

    public static HttpResult fetch(final Setup setup, final String url, final int maxSize) {
        return fetch(setup, url, maxSize, null);
    }

    public static HttpResult fetch(final Setup setup, final String url, final int maxSize,
            final String[] contentTypes) {
        final HttpClient client = setup.getHttpClient();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(Settings.getSocketTimeoutMillis())
                .setConnectTimeout(Settings.getSocketTimeoutMillis())
                .build();
        final HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);
        method.setHeader("User-Agent", Settings.getUserAgent());
        method.setHeader("Accept", Settings.getAcceptHeader());
        try {
            return fetch2(url, maxSize, contentTypes, client, method);
        } catch (final Exception ex) {
            final Logger logger = getLogger();
            logger.info("Exception when fetching URL {}", url, ex);
        }
        return new HttpResult(ResultType.NETWORK_ERROR);
    }

    public static String resolveUrl(final String base, final String rel) {
        try {
            final URI uri = URI.create(base);
            return uri.resolve(rel).toString();
        } catch (final Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private static HttpResult fetch2(final String url, final int maxSize, final String[] contentTypes,
            final HttpClient client, final HttpGet method) throws Exception {
        return client.execute(method, new ResponseHandler<HttpResult>() {
            @Override
            public HttpResult handleResponse(final HttpResponse response) throws IOException {
                final int statusCode = response.getStatusLine().getStatusCode();
                final Header locationHeader = response.getFirstHeader("Location");
                final String location;
                if (locationHeader == null)
                    location = null;
                else
                    location = resolveUrl(url, locationHeader.getValue());
                final HttpEntity entity = response.getEntity();
                final String contentType = getContentType(entity);
                final long contentLength = entity.getContentLength();
                if (contentLength > maxSize)
                    return new HttpResult(ResultType.CONTENT_TOO_LONG);
                if (contentTypes != null && !contentTypeMatches(contentType, contentTypes))
                    return new HttpResult(ResultType.WRONG_CONTENT_TYPE);
                final byte[] body = getBodyBytes(entity, maxSize);
                if (body.length > maxSize)
                    return new HttpResult(ResultType.CONTENT_TOO_LONG);
                return new HttpResult(ResultType.RESULT, statusCode, contentType, contentLength, body, location);
            }
        });
    }

    private static byte[] getBodyBytes(final HttpEntity entity, final int maxSize) throws IOException {
        final InputStream in = entity.getContent();
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int size = 0;
        for (;;) {
            final int r = in.read(buffer);
            if (r < 0) {
                break;
            }
            out.write(buffer, 0, r);
            size += r;
            if (size > maxSize)
                break;
        }
        return out.toByteArray();
    }

    private static String getContentType(final HttpEntity entity) {
        final Header ct = entity.getContentType();
        if (ct == null)
            return null;
        return ct.getValue();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(Http.class);
    }

    private Http() {
        throw new AssertionError("unreachable"); // no instances
    }
}
