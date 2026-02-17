package pl.dekrate.ergonomicsmonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration enabling Spring's scheduling capabilities.
 * Required for @Scheduled annotations to function.
 *
 * @author dekrate
 * @version 1.0
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring will automatically configure scheduling infrastructure
}

