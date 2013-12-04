package de.netestate.sparqlcrawlserver;

import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.Callable;

public final class DnsLookupTask implements Callable<UrlAndIp> {
    private volatile boolean cancelled = false;
    private final String url;

    public DnsLookupTask(final String url) {
        this.url = url;
    }

    @Override
    public UrlAndIp call() {
        if (cancelled)
            return null;
        InetAddress ip;
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost();
            ip = InetAddress.getByName(host);
        } catch (final Exception ex) {
            ip = null;
        }
        return new UrlAndIp(url, ip);
    }

    public void cancel() {
        cancelled = true;
    }
}
