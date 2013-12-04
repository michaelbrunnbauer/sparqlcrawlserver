package de.netestate.sparqlcrawlserver;

import java.net.InetAddress;

public final class UrlAndIp {
    private final String url;
    private final InetAddress ip;

    public UrlAndIp(final String url, final InetAddress ip) {
        this.url = url;
        this.ip = ip;
    }

    public String getUrl() {
        return url;
    }

    public InetAddress getIp() {
        return ip;
    }
}
