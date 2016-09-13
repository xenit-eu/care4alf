package eu.xenit.care4alf.dumbster.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: rj
 * Date: Aug 28, 2011
 * Time: 6:48:14 AM
 */
public class SmtpServerFactory {
    private final static Logger logger = LoggerFactory.getLogger(SmtpServerFactory.class);

    public static SmtpServer startServer() {
        ServerOptions serverOptions = new ServerOptions();
        return startServer(serverOptions);
    }

    public static SmtpServer startServer(ServerOptions options) {
        SmtpServer server = wireUpServer(options);
        wrapInShutdownHook(server);
        startServerThread(server);
        logger.info("Dumbster SMTP Server started on port " + options.port + ".\n");
        return server;
    }

    private static SmtpServer wireUpServer(ServerOptions options) {
        SmtpServer server = new SmtpServer();
        server.setPort(options.port);
        server.setThreaded(options.threaded);
        server.setMailStore(options.mailStore);
        return server;
    }

    private static void wrapInShutdownHook(final SmtpServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                server.stop();
                logger.info("\nDumbster SMTP Server stopped");
                logger.info("\tTotal messages received: " + server.getEmailCount());
            }
         });
    }

    private static void startServerThread(SmtpServer server) {
        new Thread(server, "test SMTP server").start();
        int timeout=1000;
        while(! server.isReady()) {
            try {
                Thread.sleep(1);
                timeout--;
                if (timeout < 1) {
                    throw new RuntimeException("Server could not be started.");
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
}
