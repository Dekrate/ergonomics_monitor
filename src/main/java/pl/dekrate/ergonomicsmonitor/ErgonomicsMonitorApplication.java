package pl.dekrate.ergonomicsmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Ergonomics Monitor.
 *
 * @author dekrate
 * @version 1.0
 */
@SpringBootApplication
public class ErgonomicsMonitorApplication {

    /**
     * Private constructor to prevent instantiation.
     */
    private ErgonomicsMonitorApplication() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(ErgonomicsMonitorApplication.class, args);
    }
}
