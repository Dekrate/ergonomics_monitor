package pl.dekrate.ergonomicsmonitor.config;

/**
 * Exception thrown when JSON conversion fails in database converters.
 *
 * @author dekrate
 * @version 1.0
 */
public class JsonConversionException extends RuntimeException {

    /**
     * Constructs a new JSON conversion exception.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public JsonConversionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
