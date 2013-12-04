package de.netestate.sparqlcrawlserver;

import java.util.List;

public final class CrawlTask implements Runnable {
    private volatile boolean cancelled = false;
    private final List<DownloadTask> tasks;
    private final List<DownloadResult> results;

    public CrawlTask(final List<DownloadTask> tasks, final List<DownloadResult> results) {
        this.tasks = tasks;
        this.results = results;
    }

    public void cancel() {
        cancelled = true;
        for (final DownloadTask task: tasks)
            task.cancel();
    }

    @Override
    public void run() {
        for (final DownloadTask task: tasks) {
            if (cancelled)
                break;
            final DownloadResult result;
            try {
                result = task.call();
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            synchronized (results) {
                results.add(result);
            }
        }
    }
}
