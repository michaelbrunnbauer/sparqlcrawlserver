package de.netestate.sparqlcrawlserver;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.net.InetAddress;
import java.util.Set;

public final class IpsInUse {
    private static final int MAX_SLEEP_TIME = 1000; // millis
    private static final int MIN_SLEEP_TIME = 10; // millis
    private static final int SLEEP_INCREMENT = 10;

    private final Set<InetAddress> ipsInUse = new ObjectOpenHashSet<>();
    private long sleepTime = MIN_SLEEP_TIME;
    
    public boolean tryUseIp(final InetAddress ip) throws Exception {
        final long st;
        synchronized (ipsInUse) {
            if (ipsInUse.add(ip)) {
                sleepTime = MIN_SLEEP_TIME;
                return true;
            }
            st = sleepTime; 
            sleepTime = Math.min(sleepTime + SLEEP_INCREMENT, MAX_SLEEP_TIME);
        }
        Thread.sleep(st);
        return false;
    }

    public void unuseIp(final InetAddress ip) {
        synchronized (ipsInUse) {
            ipsInUse.remove(ip);
        }
    }
}
