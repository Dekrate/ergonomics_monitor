package pl.dekrate.ergonomicsmonitor.model;

import java.util.Map;

/**
 * Summary of activity data.
 *
 * @param description description of the summary
 * @param details additional details map
 * @param durationMillis duration in milliseconds
 * @author dekrate
 * @version 1.0
 */
public record ActivitySummary(
    String description,
    Map<String, Object> details,
    long durationMillis
) { }
