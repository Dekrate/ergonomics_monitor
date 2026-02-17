package pl.dekrate.ergonomicsmonitor.model;

/**
 * Enumeration representing the urgency level of a break recommendation.
 * Used to prioritize and categorize notification severity.
 *
 * @author dekrate
 * @version 1.0
 */
public enum BreakUrgency {
    /**
     * Informational - user may consider a break soon.
     */
    LOW,

    /**
     * Moderate - user should take a break within the next
     * few minutes.
     */
    MEDIUM,

    /**
     * Critical - user should take a break immediately
     * to prevent strain.
     */
    HIGH,

    /**
     * Emergency - prolonged intensive activity detected,
     * immediate break required.
     */
    CRITICAL
}

