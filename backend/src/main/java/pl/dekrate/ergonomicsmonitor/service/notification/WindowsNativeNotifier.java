package pl.dekrate.ergonomicsmonitor.service.notification;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Windows-native notification adapter using system tray notifications. This is a concrete adapter
 * implementing the BreakNotifier port.
 *
 * <p>Implementation notes: - Uses TrayIcon.displayMessage (non-blocking) - Executes on
 * boundedElastic scheduler to avoid blocking reactive pipeline - Creates and reuses a single tray
 * icon instance for all notifications
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
    private static final Object TRAY_LOCK = new Object();
    private static volatile TrayIcon trayIcon;

    @Override
    public Mono<Void> sendNotification(BreakRecommendation recommendation) {
        return Mono.fromRunnable(
                        () -> {
                            String message = formatMessage(recommendation);
                            boolean delivered = false;

                            if (initializeTrayIfNeeded()) {
                                try {
                                    TrayIcon.MessageType iconType =
                                            mapUrgencyToMessageType(recommendation.getUrgency());
                                    log.info(
                                            "Displaying Windows tray notification: urgency={}",
                                            recommendation.getUrgency());
                                    trayIcon.displayMessage(NOTIFICATION_TITLE, message, iconType);
                                    delivered = true;
                                } catch (IllegalArgumentException e) {
                                    log.error("Tray notification failed", e);
                                }
                            } else {
                                log.warn("System tray is not available for this runtime session");
                            }
                            if (!delivered) {
                                throw new IllegalStateException(
                                        "No Windows notification mechanism succeeded (system tray unavailable)");
                            }
                        })
                .subscribeOn(Schedulers.boundedElastic()) // Execute on elastic thread pool
                .then(); // Convert to Mono<Void>
    }

    private boolean initializeTrayIfNeeded() {
        if (trayIcon != null) {
            return true;
        }

        synchronized (TRAY_LOCK) {
            if (trayIcon != null) {
                return true;
            }
            if (!SystemTray.isSupported()) {
                return false;
            }

            try {
                TrayIcon newTrayIcon = new TrayIcon(createTrayImage(), "Ergonomics Monitor");
                newTrayIcon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(newTrayIcon);
                trayIcon = newTrayIcon;
                return true;
            } catch (AWTException | UnsupportedOperationException e) {
                log.error("Cannot initialize system tray notifier", e);
                return false;
            }
        }
    }

    private BufferedImage createTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(28, 107, 160));
            graphics.fillOval(0, 0, 15, 15);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(7, 3, 2, 8);
            graphics.fillRect(7, 12, 2, 2);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Maps our domain urgency levels to tray notification types. */
    private TrayIcon.MessageType mapUrgencyToMessageType(BreakUrgency urgency) {
        return switch (urgency) {
            case LOW -> TrayIcon.MessageType.INFO;
            case MEDIUM -> TrayIcon.MessageType.WARNING;
            case HIGH, CRITICAL -> TrayIcon.MessageType.ERROR;
        };
    }

    /**
     * Formats the recommendation into a user-friendly message. Uses Windows-style line breaks for
     * better readability.
     */
    private String formatMessage(BreakRecommendation recommendation) {
        long minutes = recommendation.getSuggestedBreakDuration().toMinutes();

        return "Przerwa: %d min | Intensywnosc: %.1f zdarzen/min | %s"
                .formatted(
                        minutes,
                        recommendation.getMetrics().getEventsPerMinute(),
                        recommendation.getReason());
    }

    @Override
    public String getNotifierType() {
        return "Windows Tray Notification";
    }
}
