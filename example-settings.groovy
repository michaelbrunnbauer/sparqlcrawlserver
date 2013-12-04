allowedContentTypes = ['application/rdf+xml', 'text/turtle', 'text/n3',
                       'application/xml', 'text/xml',
                       'application/xhtml+xml', 'text/html', 'text/plain']
cacheLifetimeContentTooLong = 60 * 60 * 24 // 24h
cacheLifetimeHttpError = 60 * 60 // 1h
cacheLifetimeNetworkError = 60 * 60 // 1h
cacheLifetimeNotAllowedByRobots = 60 * 60 * 24 // 24h
cacheLifetimeOk = 60 * 60 * 24 // 24h
cacheLifetimeParseError = 60 * 60 * 24 // 24h
cacheLifetimeWrongContentType = 60 * 60 * 24 // 24h

datasetCacheLifetimeComplete = 60 * 60 // 1h
datasetCacheLifetimeIncomplete = 5 // 5s

logFilePath = 'crawlserver.log'
crawlThreads = 200
crawlThreadStackSize = 512 * 1024; // 512 KiB
dnsThreads = 200
dnsThreadStackSize = 128 * 1024; // 128 KiB
dnsTimeout = 1.0
maxRdfXmlSize = 3 * 1024 * 1024 // 3 MiB
maxRobotsAge = 60 * 60 // 1h
maxRobotsCacheSize = 1000
maxRobotsSize = 256 * 1024 // 256 KiB
modelCachePath = 'model-cache' // cache-folder
socketTimeout = 60 // seconds
unixOs = false
userAgent = 'sparql crawl server'

