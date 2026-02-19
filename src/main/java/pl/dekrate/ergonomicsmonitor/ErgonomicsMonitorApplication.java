package pl.dekrate.ergonomicsmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Ergonomics Monitor.
 * <p>
 * This class serves as the entry point for the Spring Boot application
 * and enables auto-configuration, component scanning, and configuration.
 *
 * @author dekrate
 * @version 1.0
 */
@SpringBootApplication
public class ErgonomicsMonitorApplication {

    /**
     * Default constructor for Spring Boot.
     */
    public ErgonomicsMonitorApplication() {
        // Default constructor for Spring instantiation
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
