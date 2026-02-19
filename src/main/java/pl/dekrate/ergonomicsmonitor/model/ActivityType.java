package pl.dekrate.ergonomicsmonitor.model;

/**
 * Types of user activity that can be monitored.
 *
 * @author dekrate
 * @version 1.0
 */
public enum ActivityType {
    /** Keyboard input activity. */
    KEYBOARD,

    /** Mouse movement or click activity. */
    MOUSE,

	/** System-generated aggregated event. */
    SYSTEM_EVENT
}
