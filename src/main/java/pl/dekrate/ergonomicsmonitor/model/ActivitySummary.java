package pl.dekrate.ergonomicsmonitor.model;

import java.util.Map;

public record ActivitySummary(
    String description,
    Map<String, Object> details,
    long durationMillis
) {}
