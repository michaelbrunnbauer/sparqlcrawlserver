package de.netestate.sparqlcrawlserver;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.jena.riot.IO_Jena;

@WebListener
public final class MyServletContextListener implements ServletContextListener {
    private static final String SETTINGS_FILE_PARAMETER = "settings-file";

    private static void initialize(final ServletContext servletContext) throws Exception {
        IO_Jena.wireIntoJena();
        Class.forName("net.rootdev.javardfa.jena.RDFaReader");
        final String settingsFile = servletContext.getInitParameter(SETTINGS_FILE_PARAMETER);
        Settings.load(settingsFile);
        Setup.init(servletContext);
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        final ServletContext servletContext = event.getServletContext();
        try {
            initialize(servletContext);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        final ServletContext servletContext = event.getServletContext();
        try {
            Setup.destroy(servletContext);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
