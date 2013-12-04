package de.netestate.sparqlcrawlserver;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.codec.binary.StringUtils;
import org.joda.time.LocalDateTime;

public final class LogFile implements AutoCloseable {
    private OutputStream out;

    public LogFile() throws Exception {
        out = Files.newOutputStream(Settings.getLogFilePath(), CREATE, APPEND);
    }

    @Override
    public void close() throws Exception {
        final OutputStream out = this.out;
        if (out == null)
            return;
        this.out = null;
        out.close();
    }

    public void log(final long requestId, final String error, final String url) throws Exception {
        synchronized (out) {
            final String line = requestId + "|" + new LocalDateTime() + '|' + error + ": " + PythonRepr.repr(url)
                    + '\n';
            out.write(StringUtils.getBytesUsAscii(line));
            out.flush();
        }
    }
}
