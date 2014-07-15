package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public final class DatasetBuilder {
    private boolean complete;
    private final Cache<String, ModelCacheEntry> modelCache;
    private Dataset dataset;
    private final ExecutorService crawlExecutor;
    private List<Future<?>> crawlFutures;
    private List<DownloadResult> crawlResults;
    private List<CrawlTask> crawlTasks;
    private final ExecutorService dnsExecutor;
    private final Logger logger = LoggerFactory.getLogger(DatasetBuilder.class);
    private final long requestId;
    private final Setup setup;
    private Map<String, InetAddress> urlsWithIp;
    private List<IriUrl> urlsToCrawl;

    public DatasetBuilder(final long requestId, final Setup setup) {
        this.requestId = requestId;
        this.setup = setup;
        crawlExecutor = setup.getCrawlExecutor();
        dnsExecutor = setup.getDnsExecutor();
        modelCache = setup.getModelCache();
    }

    public Dataset build(final List<IriUrl> urls, final long timeout) throws Exception {
        final long startTime = System.nanoTime();
        complete = true;
        dataset = DatasetFactory.createMemFixed();
        urlsWithIp = new Object2ObjectOpenHashMap<>(urls.size());
        addCachedModels(urls);
        if (urlsToCrawl.isEmpty())
            return dataset;
        lookupIps();
        submitCrawlTasks();
        addCrawledModels(timeout);
        final double duration = (System.nanoTime() - startTime) * 1e-9;
        logger.info(String.format("duration: %.3f s", duration));
        return dataset;
    }

    public boolean isComplete() {
        return complete;
    }

    private void addCachedModels(final List<IriUrl> urls) throws Exception {
        urlsToCrawl = new ObjectArrayList<>();
        for (final IriUrl iriUrl: urls) {
            final String url = iriUrl.getUrl();
            final ModelCacheEntry cacheEntry = modelCache.get(url);
            if (cacheEntry == null)
                urlsToCrawl.add(iriUrl);
            else if (!cacheEntry.isError()) {
                final Model model = ModelFactory.createDefaultModel();
                try {
                    model.read(new ByteArrayInputStream(cacheEntry.getData()), null, "N-TRIPLE");
                    dataset.addNamedModel(cacheEntry.getActualUrl(), model);
                } catch (final Exception ex) {
                    // ignore exceptions from model.read()
                }
            }
        }
    }

    private void addCrawledModels(final long timeout) throws Exception {
        for (final Future<?> future: crawlFutures) {
            final long waitTime = timeout - System.nanoTime();
            if (timeout <= 0) {
                complete = false;
                break;
            }
            try {
                future.get(waitTime, TimeUnit.NANOSECONDS);
            } catch (final TimeoutException ex) {
                complete = false;
            }
        }
        logger.info("is complete: {}", complete);
        for (final CrawlTask task: crawlTasks)
            task.cancel();
        final List<DownloadResult> crawlResults;
        synchronized (this.crawlResults) {
            crawlResults = new ReferenceArrayList<>(this.crawlResults);
        }
        for (final DownloadResult result: crawlResults) {
            if (result == null || result.getType() != ResultType.RESULT)
                continue;
            final String url = result.getActualUrl();
            final Model model = result.getModel();
            dataset.addNamedModel(url, model);
        }
    }

    private boolean getIpLookupResult(final CompletionService<UrlAndIp> completionService, final long waitTime)
            throws Exception {
        final Future<UrlAndIp> future = waitTime > 0L ? completionService.poll(waitTime, TimeUnit.NANOSECONDS)
                : completionService.poll();
        if (future == null)
            return false;
        final UrlAndIp urlAndIp = future.get();
        urlsWithIp.put(urlAndIp.getUrl(), urlAndIp.getIp());
        return true;
    }

    private void lookupIps() throws Exception {
        final CompletionService<UrlAndIp> completionService = new ExecutorCompletionService<>(dnsExecutor);
        final List<DnsLookupTask> tasks = new ReferenceArrayList<>(urlsToCrawl.size());
        for (final IriUrl iriUrl: urlsToCrawl) {
            final DnsLookupTask task = new DnsLookupTask(iriUrl.getUrl());
            completionService.submit(task);
            tasks.add(task);
        }
        long now = System.nanoTime();
        final long start = now;
        final long timeout = now + Settings.getDnsTimeoutNanos();
        final int taskCount = tasks.size();
        for (int n = 0; n < taskCount; ++n) {
            final long waitTime = timeout - now;
            if (!getIpLookupResult(completionService, waitTime))
                break;
            now = System.nanoTime();
        }
        final String msg = String.format("DNS lookup: %.3f s", (System.nanoTime() - start) * 1e-9);
        logger.info(msg);
        for (final DnsLookupTask task: tasks)
            task.cancel();
    }

    private void submitCrawlTask(final List<DownloadTask> downloadTasks) {
        final CrawlTask crawlTask = new CrawlTask(downloadTasks, crawlResults);
        final Future<?> future = crawlExecutor.submit(crawlTask);
        crawlFutures.add(future);
        crawlTasks.add(crawlTask);
    }

    private void submitCrawlTasks() {
        crawlFutures = new ReferenceArrayList<>();
        crawlResults = new ReferenceArrayList<>();
        crawlTasks = new ReferenceArrayList<>();
        // group into tasks by ip
        final Map<InetAddress, List<DownloadTask>> tasksWithIp = new Object2ReferenceOpenHashMap<>();
        for (final IriUrl iriUrl: urlsToCrawl) {
            final String iri = iriUrl.getIri();
            final String url = iriUrl.getUrl();
            final InetAddress ip = urlsWithIp.get(url);
            if (ip == null) {
                // ip is unresolvable or lookup timed out
                final List<DownloadTask> downloadTasks = Arrays.asList(new DownloadTask(requestId, setup, iri, url));
                submitCrawlTask(downloadTasks);
            } else {
                List<DownloadTask> downloadTasks = tasksWithIp.get(ip);
                if (downloadTasks == null) {
                    downloadTasks = new ReferenceArrayList<>();
                    tasksWithIp.put(ip, downloadTasks);
                }
                downloadTasks.add(new DownloadTask(requestId, setup, iri, url, ip));
            }
        }
        for (final List<DownloadTask> downloadTasks: tasksWithIp.values())
            submitCrawlTask(downloadTasks);
    }
}
