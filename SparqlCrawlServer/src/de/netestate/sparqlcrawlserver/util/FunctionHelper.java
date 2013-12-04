package de.netestate.sparqlcrawlserver.util;

import java.util.Collection;
import java.util.Iterator;

public final class FunctionHelper {
    public static void runAll(final Collection<? extends Proc0> procedures) throws Exception {
        final Iterator<? extends Proc0> iterator = procedures.iterator();
        while (iterator.hasNext()) {
            final Proc0 proc = iterator.next();
            try {
                proc.run();
            } catch (final Throwable ex) {
                while (iterator.hasNext()) {
                    final Proc0 proc1 = iterator.next();
                    try {
                        proc1.run();
                    } catch (final Throwable ex1) {
                        ex.addSuppressed(ex1);
                    }
                }
                throw ex;
            }
        }
    }

    public static void tryAndError(final Proc0 procedure, final Proc0 onError) throws Exception {
        try {
            procedure.run();
        } catch (final Throwable ex) {
            try {
                onError.run();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private FunctionHelper() {
        throw new AssertionError("unreachable"); // no instances
    }
}
