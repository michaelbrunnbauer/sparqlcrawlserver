package de.netestate.sparqlcrawlserver;

import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import de.netestate.sparqlcrawlserver.util.FunctionHelper;
import de.netestate.sparqlcrawlserver.util.Proc0;
import de.netestate.sparqlcrawlserver.util.ResourcesCloser;

public final class Setup implements AutoCloseable {
    private static final int MAX_HTTP_LINE_LENGTH = 16384;
    private static final int MAX_HTTP_HEADER_COUNT = 128;

    private static final String SERVLET_CONTEXT_SETUP_NAME = "Setup";

    public static Setup getInstance(final ServletContext servletContext) {
        return (Setup) servletContext.getAttribute(SERVLET_CONTEXT_SETUP_NAME);
    }

    public static void destroy(final ServletContext servletContext) throws Exception {
        final Setup setup = getInstance(servletContext);
        if (setup == null)
            return;
        try {
            setup.close();
        } finally {
            servletContext.removeAttribute(SERVLET_CONTEXT_SETUP_NAME);
        }
    }

    public static void init(final ServletContext servletContext) throws Exception {
        servletContext.setAttribute(SERVLET_CONTEXT_SETUP_NAME, new Setup());
    }

    private static CloseableHttpClient makeHttpClient() throws Exception {
        final SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(final X509Certificate[] chain, final String authType) {
                return true;
            }
        });
        final ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        final SSLContext sslContext = sslContextBuilder.build();
        final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        final int maxConnections = Settings.getCrawlThreads();
        cm.setMaxTotal(maxConnections);
        cm.setDefaultMaxPerRoute(maxConnections);
        final MessageConstraints messageConstraints = MessageConstraints.custom()
                .setMaxHeaderCount(MAX_HTTP_HEADER_COUNT)
                .setMaxLineLength(MAX_HTTP_LINE_LENGTH)
                .build();
        final ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMessageConstraints(messageConstraints)
                .build();
        cm.setDefaultConnectionConfig(connectionConfig);
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(Settings.getSocketTimeoutMillis())
                .setConnectTimeout(Settings.getSocketTimeoutMillis())
                .build();
        return HttpClients.custom()
                .setConnectionManager(cm)
                .disableContentCompression()
                .disableRedirectHandling()
                .disableCookieManagement()
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(Settings.getUserAgent())
                .build();
    }

    private final DatasetCache datasetCache = new DatasetCache();
    private final Cache<String, ModelCacheEntry> modelCache = new Cache<>(Settings.getModelCachePath());
    private final ExecutorService crawlExecutor;
    private final ExecutorService dnsExecutor;
    private final HttpClient httpClient;
    private final IpsInUse ipsInUse;
    private final LogFile logFile;
    private final ResourcesCloser resourcesCloser;
    private final RobotsCache robotsCache;

    private Setup() throws Exception {
        resourcesCloser = new ResourcesCloser();
        final MutableObject<ExecutorService> crawlExecutorHolder = new MutableObject<>();
        final MutableObject<ExecutorService> dnsExecutorHolder = new MutableObject<>();
        final MutableObject<HttpClient> httpClientHolder = new MutableObject<>();
        final MutableObject<LogFile> logFileHolder = new MutableObject<>();
        FunctionHelper.tryAndError(new Proc0() {
            @Override
            public void run() throws Exception {
                final ExecutorService crawlExecutor = makeCrawlExecutor();
                final ExecutorService dnsExecutor = makeDnsExecutor();
                final CloseableHttpClient httpClient = makeHttpClient();
                resourcesCloser.add(httpClient);
                httpClientHolder.setValue(httpClient);
                crawlExecutorHolder.setValue(crawlExecutor);
                dnsExecutorHolder.setValue(dnsExecutor);
                final LogFile logFile = new LogFile();
                resourcesCloser.add(logFile);
                logFileHolder.setValue(logFile);
            }

            private ExecutorService makeDnsExecutor() {
                final AtomicInteger dnsExecutorThreadNr = new AtomicInteger(1);
                final ExecutorService dnsExecutor = Executors.newFixedThreadPool(Settings.getDnsThreads(),
                        new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable target) {
                        final String name = "DNS thread #" + dnsExecutorThreadNr.getAndIncrement();
                        return new Thread(null, target, name, Settings.getDnsThreadStackSize());
                    }
                });
                resourcesCloser.add(new Proc0() {
                    @Override
                    public void run() {
                        dnsExecutor.shutdown();
                    }
                });
                return dnsExecutor;
            }

            private ExecutorService makeCrawlExecutor() {
                final AtomicInteger crawlExecutorThreadNr = new AtomicInteger(1);
                final ExecutorService crawlExecutor = Executors.newFixedThreadPool(Settings.getCrawlThreads(),
                        new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable target) {
                        final String name = "Crawl thread #" + crawlExecutorThreadNr.getAndIncrement();
                        return new Thread(null, target, name, Settings.getCrawlThreadStackSize());
                    }
                });
                resourcesCloser.add(new Proc0() {
                    @Override
                    public void run() {
                        crawlExecutor.shutdown();
                    }
                });
                return crawlExecutor;
            }
        }, new Proc0() {
            @Override
            public void run() throws Exception {
                resourcesCloser.close();
            }
        });
        crawlExecutor = crawlExecutorHolder.getValue();
        dnsExecutor = dnsExecutorHolder.getValue();
        httpClient = httpClientHolder.getValue();
        ipsInUse = new IpsInUse();
        logFile = logFileHolder.getValue();
        robotsCache = new RobotsCache(this);
    }

    @Override
    public void close() throws Exception {
        resourcesCloser.close();
    }

    public DatasetCache getDatasetCache() {
        return datasetCache;
    }

    public ExecutorService getCrawlExecutor() {
        return crawlExecutor;
    }

    public ExecutorService getDnsExecutor() {
        return dnsExecutor;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public IpsInUse getIpsInUse() {
        return ipsInUse;
    }

    public LogFile getLogFile() {
        return logFile;
    }

    public Cache<String, ModelCacheEntry> getModelCache() {
        return modelCache;
    }

    public RobotsCache getRobotsCache() {
        return robotsCache;
    }
}
