package pl.dekrate.ergonomicsmonitor.service.notification;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
                                    log.warn(
                                            "Tray notification failed, trying PowerShell fallback",
                                            e);
                                }
                            } else {
                                log.warn(
                                        "System tray is not available, trying PowerShell fallbacks");
                            }

                            if (!delivered) {
                                delivered = sendPowerShellBalloon(NOTIFICATION_TITLE, message);
                            }
                            if (!delivered) {
                                delivered = sendPowerShellToast(NOTIFICATION_TITLE, message);
                            }
                            if (!delivered) {
                                throw new IllegalStateException(
                                        "No Windows notification mechanism succeeded");
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

    private boolean sendPowerShellToast(String title, String message) {
        String escapedTitle = escapePowerShellLiteral(title);
        String escapedMessage = escapePowerShellLiteral(message);

        String script =
                "$ErrorActionPreference='Stop';"
                        + "$title='"
                        + escapedTitle
                        + "';"
                        + "$msg='"
                        + escapedMessage
                        + "';"
                        + "[Windows.UI.Notifications.ToastNotificationManager,Windows.UI.Notifications,ContentType=WindowsRuntime] > $null;"
                        + "[Windows.Data.Xml.Dom.XmlDocument,Windows.Data.Xml.Dom.XmlDocument,ContentType=WindowsRuntime] > $null;"
                        + "$title=[Security.SecurityElement]::Escape($title);"
                        + "$msg=[Security.SecurityElement]::Escape($msg);"
                        + "$xml=New-Object Windows.Data.Xml.Dom.XmlDocument;"
                        + "$xml.LoadXml(\"<toast><visual><binding template='ToastGeneric'><text>$title</text><text>$msg</text></binding></visual></toast>\");"
                        + "$toast=[Windows.UI.Notifications.ToastNotification]::new($xml);"
                        + "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Ergonomics Monitor').Show($toast);";

        String encodedScript =
                Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-EncodedCommand", encodedScript);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (exitCode == 0) {
                log.info(
                        "PowerShell toast command executed successfully (OS may still suppress display)");
                return true;
            }

            log.warn(
                    "PowerShell toast fallback failed with exit code {} and output: {}",
                    exitCode,
                    output);
            return false;
        } catch (IOException e) {
            log.warn("PowerShell toast fallback failed to start", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PowerShell toast fallback interrupted", e);
            return false;
        }
    }

    private boolean sendPowerShellBalloon(String title, String message) {
        String escapedTitle = escapePowerShellLiteral(title);
        String escapedMessage = escapePowerShellLiteral(message);

        String script =
                "$ErrorActionPreference='Stop';"
                        + "Add-Type -AssemblyName System.Windows.Forms;"
                        + "Add-Type -AssemblyName System.Drawing;"
                        + "$title='"
                        + escapedTitle
                        + "';"
                        + "$msg='"
                        + escapedMessage
                        + "';"
                        + "$n=New-Object System.Windows.Forms.NotifyIcon;"
                        + "$n.Icon=[System.Drawing.SystemIcons]::Information;"
                        + "$n.BalloonTipIcon=[System.Windows.Forms.ToolTipIcon]::Info;"
                        + "$n.BalloonTipTitle=$title;"
                        + "$n.BalloonTipText=$msg;"
                        + "$n.Visible=$true;"
                        + "$n.ShowBalloonTip(8000);"
                        + "Start-Sleep -Seconds 4;"
                        + "$n.Dispose();";

        String encodedScript =
                Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-EncodedCommand", encodedScript);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (exitCode == 0) {
                log.info("Displayed Windows balloon notification via PowerShell fallback");
                return true;
            }

            log.warn(
                    "PowerShell balloon fallback failed with exit code {} and output: {}",
                    exitCode,
                    output);
            return false;
        } catch (IOException e) {
            log.warn("PowerShell balloon fallback failed to start", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PowerShell balloon fallback interrupted", e);
            return false;
        }
    }

    private String escapePowerShellLiteral(String input) {
        return input.replace("'", "''");
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
