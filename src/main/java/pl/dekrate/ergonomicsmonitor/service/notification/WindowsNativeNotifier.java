package pl.dekrate.ergonomicsmonitor.service.notification;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Windows-native notification adapter using JNA to display MessageBox dialogs. This is a concrete
 * adapter implementing the BreakNotifier port.
 *
 * <p>Implementation notes: - MessageBox calls are BLOCKING by design (Windows API limitation) - We
 * execute on boundedElastic scheduler to avoid blocking reactive pipeline - Uses MB_TOPMOST flag to
 * ensure notification is visible - Provides better UX than toast notifications (user must
 * acknowledge)
 *
 * <p>Conditional bean - only loaded on Windows platforms and when enabled in config.
 *
 * @author dekrate
 * @version 1.0
 */
@Component
@ConditionalOnProperty(
        name = "ergonomics.notifications.windows.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WindowsNativeNotifier implements BreakNotifier {

    private static final Logger log = LoggerFactory.getLogger(WindowsNativeNotifier.class);

    private static final String NOTIFICATION_TITLE =
            "Ergonomics Monitor - Przypomnienie o przerwie";

    // Windows MessageBox icon constants
    private static final int MB_ICONINFORMATION = 0x00000040;
    private static final int MB_ICONWARNING = 0x00000030;
    private static final int MB_ICONERROR = 0x00000010;
    private static final int MB_TOPMOST = 0x00040000;
    private static final int MB_SETFOREGROUND = 0x00010000;

    /** Extended User32 interface to ensure proper threading model. */
    public interface User32Extended extends StdCallLibrary {
        User32Extended INSTANCE =
                Native.load("user32", User32Extended.class, W32APIOptions.DEFAULT_OPTIONS);

        /**
         * MessageBox with all parameters for maximum control. Method name matches Windows API
         * convention.
         */
        @SuppressWarnings("checkstyle:MethodName")
        int messageBoxW(HWND hWnd, String lpText, String lpCaption, int uType);
    }

    @Override
    public Mono<Void> sendNotification(BreakRecommendation recommendation) {
        return Mono.fromRunnable(
                        () -> {
                            try {
                                int iconType = mapUrgencyToIconType(recommendation.getUrgency());
                                String message = formatMessage(recommendation);

                                log.info(
                                        "Displaying Windows MessageBox: urgency={}",
                                        recommendation.getUrgency());

                                // MessageBox is BLOCKING - but we're on boundedElastic scheduler,
                                // so it's safe
                                // MB_TOPMOST ensures the dialog appears on top
                                // MB_SETFOREGROUND brings it to foreground
                                int flags = iconType | MB_TOPMOST | MB_SETFOREGROUND;

                                int result =
                                        User32Extended.INSTANCE.messageBoxW(
                                                null, // No parent window
                                                message,
                                                NOTIFICATION_TITLE,
                                                flags);

                                log.debug("MessageBox result: {} (1=OK)", result);

                            } catch (UnsatisfiedLinkError e) {
                                // Native library loading failed
                                log.error(
                                        "Failed to display Windows notification: JNA library not available",
                                        e);
                            } catch (IllegalArgumentException e) {
                                // Invalid arguments to JNA call
                                log.error(
                                        "Failed to display Windows notification: invalid arguments",
                                        e);
                            } catch (RuntimeException e) {
                                // Unexpected runtime error
                                log.error("Unexpected error displaying Windows notification", e);
                            }
                        })
                .subscribeOn(Schedulers.boundedElastic()) // Execute on elastic thread pool
                .then(); // Convert to Mono<Void>
    }

    /** Maps our domain urgency levels to Windows MessageBox icon constants. */
    private int mapUrgencyToIconType(BreakUrgency urgency) {
        return switch (urgency) {
            case LOW -> MB_ICONINFORMATION; // Blue (i) icon
            case MEDIUM -> MB_ICONWARNING; // Yellow (!) icon
            case HIGH, CRITICAL -> MB_ICONERROR; // Red (X) icon
        };
    }

    /**
     * Formats the recommendation into a user-friendly message. Uses Windows-style line breaks for
     * better readability.
     */
    private String formatMessage(BreakRecommendation recommendation) {
        long minutes = recommendation.getSuggestedBreakDuration().toMinutes();

        return """
                %s\r
                \r
                Zalecana dlugosc przerwy: %d minut\r
                \r
                Statystyki aktywnosci:\r
                  - Laczna liczba zdarzen: %,d\r
                  - Zdarzenia klawiatury: %,d\r
                  - Zdarzenia myszy: %,d\r
                  - Intensywnosc: %.1f zdarzen/min"""
                .formatted(
                        recommendation.getReason(),
                        minutes,
                        recommendation.getMetrics().getTotalEvents(),
                        recommendation.getMetrics().getKeyboardEvents(),
                        recommendation.getMetrics().getMouseEvents(),
                        recommendation.getMetrics().getEventsPerMinute());
    }

    @Override
    public String getNotifierType() {
        return "Windows Native MessageBox";
    }
}
