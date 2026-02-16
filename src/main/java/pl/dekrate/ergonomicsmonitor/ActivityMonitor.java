package pl.dekrate.ergonomicsmonitor;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.LowLevelMouseProc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.repository.ActivityEventRepository;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Low-level system monitor that uses JNA (Java Native Access) to hook into Windows input events.
 * It captures keyboard and mouse activity without blocking the main application flow.
 * 
 * Captured events are streamed into a reactive sink for asynchronous processing and aggregation.
 */
@Service
public class ActivityMonitor {

    private static final Logger log = LoggerFactory.getLogger(ActivityMonitor.class);
    
    private final ActivityEventRepository repository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Sinks.Many<ActivityType> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    
    private HHOOK hKeyboardHook;
    private HHOOK hMouseHook;

    public ActivityMonitor(ActivityEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Internal callback for low-level keyboard events.
     */
    private final LowLevelKeyboardProc keyboardProc = new LowLevelKeyboardProc() {
        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
            if (nCode >= 0) {
                eventSink.tryEmitNext(ActivityType.KEYBOARD);
            }
            return User32.INSTANCE.CallNextHookEx(hKeyboardHook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    };

    /**
     * Internal callback for low-level mouse events.
     */
    private final LowLevelMouseProc mouseProc = new LowLevelMouseProc() {
        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT info) {
            if (nCode >= 0) {
                eventSink.tryEmitNext(ActivityType.MOUSE);
            }
            return User32.INSTANCE.CallNextHookEx(hMouseHook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    };

    /**
     * Initializes the monitor. Sets up the reactive processing pipeline for event aggregation
     * and starts the native Windows message loop on a Virtual Thread.
     */
    @PostConstruct
    public void start() {
        // Event aggregation pipeline
        eventSink.asFlux()
                .bufferTimeout(50, Duration.ofSeconds(10))
                .filter(list -> !list.isEmpty())
                .flatMap(list -> {
                    log.info("Aggregating and saving {} events", list.size());
                    ActivityEvent aggregatedEvent = createAggregatedEvent(list);
                    return repository.save(aggregatedEvent);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        saved -> log.trace("Saved aggregated event: {}", saved.getId()),
                        err -> log.error("Error saving events", err)
                );

        // Start native hooks
        executor.submit(() -> {
            log.info("Starting JNA Activity Monitor on Virtual Thread: {}", Thread.currentThread());
            WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);

            hKeyboardHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardProc, hMod, 0);
            hMouseHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_MOUSE_LL, mouseProc, hMod, 0);

            if (hKeyboardHook == null || hMouseHook == null) {
                log.error("Failed to install hooks! Last Error: {}", Kernel32.INSTANCE.GetLastError());
                return;
            }

            WinUser.MSG msg = new WinUser.MSG();
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }
        });
    }

    /**
     * Creates an aggregated ActivityEvent from a list of raw activity types.
     * Separated for unit testing.
     * 
     * @param types list of captured activity types
     * @return a new aggregated ActivityEvent
     */
    protected ActivityEvent createAggregatedEvent(List<ActivityType> types) {
        long keyboards = types.stream().filter(t -> t == ActivityType.KEYBOARD).count();
        long mice = types.stream().filter(t -> t == ActivityType.MOUSE).count();

        return ActivityEvent.builder()
                .id(UUID.randomUUID())
                .timestamp(Instant.now())
                .type(ActivityType.SYSTEM_EVENT)
                .intensity((double) types.size())
                .metadata(Map.of(
                        "keyboard_count", keyboards,
                        "mouse_count", mice,
                        "total_count", types.size()
                ))
                .build();
    }

    /**
     * Uninstalls native hooks and shuts down the executor to ensure clean exit.
     */
    @PreDestroy
    public void stop() {
        if (hKeyboardHook != null) User32.INSTANCE.UnhookWindowsHookEx(hKeyboardHook);
        if (hMouseHook != null) User32.INSTANCE.UnhookWindowsHookEx(hMouseHook);
        executor.shutdown();
    }
}
