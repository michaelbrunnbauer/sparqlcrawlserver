package de.netestate.sparqlcrawlserver;

import java.io.Serializable;

public final class ModelCacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] data;
    private final boolean error;
    private final String actualUrl;

    public ModelCacheEntry(final byte[] data, final String actualUrl) {
        this.data = data;
        error = false;
        this.actualUrl = actualUrl;
    }

    public ModelCacheEntry(final boolean error) {
        data = null;
        this.error = error;
        actualUrl = null;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isError() {
        return error;
    }

    public String getActualUrl() {
        return actualUrl;
    }
}
