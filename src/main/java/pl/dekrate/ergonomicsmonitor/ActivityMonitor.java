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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ActivityMonitor {

    private static final Logger log = LoggerFactory.getLogger(ActivityMonitor.class);
    
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private HHOOK hKeyboardHook;
    private HHOOK hMouseHook;

    private final LowLevelKeyboardProc keyboardProc = new LowLevelKeyboardProc() {
        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
            if (nCode >= 0) {
                handleActivity(ActivityType.KEYBOARD);
            }
            return User32.INSTANCE.CallNextHookEx(hKeyboardHook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    };

    private final LowLevelMouseProc mouseProc = new LowLevelMouseProc() {
        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT info) {
            if (nCode >= 0) {
                handleActivity(ActivityType.MOUSE);
            }
            return User32.INSTANCE.CallNextHookEx(hMouseHook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    };

    @PostConstruct
    public void start() {
        executor.submit(() -> {
            log.info("Starting JNA Activity Monitor on Virtual Thread (Java 23 Clean): {}", Thread.currentThread());
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

    private void handleActivity(ActivityType type) {
        log.trace("Activity detected: {}", type);
    }

    @PreDestroy
    public void stop() {
        if (hKeyboardHook != null) User32.INSTANCE.UnhookWindowsHookEx(hKeyboardHook);
        if (hMouseHook != null) User32.INSTANCE.UnhookWindowsHookEx(hMouseHook);
        executor.shutdown();
    }
}
