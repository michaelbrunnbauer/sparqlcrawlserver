package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public final class RobotsParser {
    public static RobotsDirectives parse(final String userAgent, final byte[] robotsFile, final int httpCode)
            throws Exception {
        final String robotsFileString = robotsFile == null ? "" : new String(robotsFile, "ISO-8859-1");
        return new RobotsParser(userAgent, robotsFileString, httpCode).parse();
    }

    public static RobotsDirectives parse(final String userAgent,
                                         final String robotsFile,
                                         final int httpCode) {
        return new RobotsParser(userAgent, robotsFile, httpCode).parse();
    }

    private final List<String> disallowPrefixes = new ObjectArrayList<>();
    private String line;
    private boolean matchedSpecific = false;
    private int pos = 0;
    private boolean recordApplies = false;
    private String robotsFile;
    private final int statuscode;
    private final String userAgent;

    private RobotsParser(final String userAgent,
                         final String robotsFile,
                         final int statuscode) {
        this.userAgent = userAgent.toLowerCase();
        this.robotsFile = robotsFile;
        this.statuscode = statuscode;
    }

    private boolean fetchLine() {
        if (pos >= robotsFile.length())
            return false;
        int p = robotsFile.indexOf('\n', pos);
        if (p < 0)
            p = robotsFile.length();
        else
            ++p;
        line = robotsFile.substring(pos, p).trim();
        pos = p;
        return true;
    }

    private void normalizeLineEndings() {
        robotsFile = robotsFile.replace("\r\n", "\n");
        robotsFile = robotsFile.replace("\r", "\n");
    }

    private RobotsDirectives parse() {
        if (statuscode == 401 || statuscode == 403)
            disallowPrefixes.add("/");
        else if (statuscode < 400 && robotsFile != null)
            parseRobotsFile();
        return new RobotsDirectives(disallowPrefixes.toArray(new String[0]));
    }

    private void parseRobotsFile() {
        normalizeLineEndings();
        while (fetchLine()) {
            if (line.startsWith("#"))
                continue;
            if (line.isEmpty()) {
                recordApplies = false;
                continue;
            }
            stripComment();
            final int p = line.indexOf(':');
            if (p < 0)
                continue;
            final String header = line.substring(0, p).trim().toLowerCase();
            final String value = line.substring(p + 1).trim();
            if (header.equals("user-agent")) {
                if (value.equals("*")) {
                    if (!matchedSpecific)
                        recordApplies = true;
                } else {
                    final String ua = value.toLowerCase();
                    if (userAgent.startsWith(ua)) {
                        recordApplies = true;
                        if (!matchedSpecific) {
                            disallowPrefixes.clear();
                            matchedSpecific = true;
                        }
                    }
                }
            } else if (header.equals("disallow") && recordApplies) {
                if (!value.isEmpty())
                    disallowPrefixes.add(value);
            }
        }
    }

    private void stripComment() {
        final int p = line.indexOf('#');
        if (p >= 0)
            line = line.substring(0, p).trim();
    }
}
