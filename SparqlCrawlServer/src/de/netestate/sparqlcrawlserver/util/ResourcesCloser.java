package de.netestate.sparqlcrawlserver.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public final class ResourcesCloser implements AutoCloseable {
    private Deque<Proc0> closers = new ArrayDeque<>();

    public void add(final AutoCloseable autoCloseable) {
        add(new Proc0() {
            @Override
            public void run() throws Exception {
                autoCloseable.close();
            }
        });
    }

    public void add(final Proc0 closer) {
        closers.addFirst(closer);
    }

    @Override
    public void close() throws Exception {
        final Collection<Proc0> closers = this.closers;
        if (closers == null)
            return;
        this.closers = null;
        FunctionHelper.runAll(closers);
    }
}
