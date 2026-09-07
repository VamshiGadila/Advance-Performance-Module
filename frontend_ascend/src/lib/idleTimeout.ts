/**
 * ASCEND Idle Session Timeout & Activity Tracker
 * 
 * Features:
 * - Configurable custom idle timeout duration (default: 15 minutes)
 * - Real-time activity detection (mouse, keyboard, scroll, touch, clicks)
 * - Dynamic warning window (e.g., 60s for 15m; 6s for 30s testing)
 * - Visual UI Countdown Modal & Prompt (Stay Active vs Log Out)
 * - Clean browser console logs without DevTools error/warning badges
 * - Window helper methods for live console testing: window.setIdleTimeoutMinutes(n)
 */

export interface IdleTimeoutOptions {
    defaultTimeoutMinutes?: number;
    heartbeatIntervalSeconds?: number;
    warningSecondsBeforeTimeout?: number;
}

export interface IdleTrackerCallbacks {
    onTimeout: () => void;
    onWarningCountdown?: (remainingSeconds: number) => void;
    onWarningCleared?: () => void;
}

const DEFAULT_TIMEOUT_MINS = 15;
const STORAGE_KEY = "ascend_idle_timeout_mins";

let globalResetIdleTimer: (() => void) | null = null;

/**
 * Get current configured idle timeout in minutes
 */
export function getIdleTimeoutMinutes(): number {
    if (typeof window === "undefined") return DEFAULT_TIMEOUT_MINS;
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            const parsed = parseFloat(stored);
            if (!isNaN(parsed) && parsed > 0) {
                return parsed;
            }
        }
    } catch {}
    return DEFAULT_TIMEOUT_MINS;
}

/**
 * Update custom idle timeout in minutes
 */
export function setIdleTimeoutMinutes(minutes: number): void {
    if (typeof window === "undefined" || minutes <= 0) return;
    try {
        localStorage.setItem(STORAGE_KEY, minutes.toString());
        if (globalResetIdleTimer) {
            globalResetIdleTimer();
        }
        console.log(
            `%c[ASCEND Session Manager] ⚙️ Idle timeout set to: ${minutes} minute(s) (${Math.round(minutes * 60)}s)`,
            "color: #10b981; font-weight: bold;"
        );
    } catch {}
}

/**
 * Programmatically reset the idle timer (e.g. when user clicks "Stay Active")
 */
export function resetIdleTimer(): void {
    if (globalResetIdleTimer) {
        globalResetIdleTimer();
    }
}

/**
 * Format seconds into mm:ss or human-readable format
 */
function formatDuration(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    if (m === 0) return `${s}s`;
    return `${m}m ${s < 10 ? "0" : ""}${s}s`;
}

/**
 * Initialize the global Idle Session Tracker
 */
export function initIdleSessionTracker(
    callbackOrConfig: (() => void) | IdleTrackerCallbacks,
    options?: IdleTimeoutOptions
): () => void {
    if (typeof window === "undefined") {
        return () => {};
    }

    const callbacks: IdleTrackerCallbacks =
        typeof callbackOrConfig === "function"
            ? { onTimeout: callbackOrConfig }
            : callbackOrConfig;

    let lastActivityTime = Date.now();
    let warningActive = false;

    const timeoutMinutes = options?.defaultTimeoutMinutes ?? getIdleTimeoutMinutes();
    const timeoutSeconds = timeoutMinutes * 60;
    
    console.log(
        `%c[ASCEND Session Manager] ⏱️ Idle session tracker initialized.\n` +
        `• Current Timeout: ${timeoutMinutes} minute(s) (${timeoutSeconds} seconds)\n` +
        `• Status: Active & Monitoring Activity\n` +
        `💡 Tip: Test anytime in console: window.setIdleTimeoutMinutes(0.5)`,
        "color: #6366f1; font-weight: bold; line-height: 1.5;"
    );

    // Global reset trigger
    globalResetIdleTimer = () => {
        lastActivityTime = Date.now();
        if (warningActive) {
            warningActive = false;
            callbacks.onWarningCleared?.();
        }
    };

    // Throttled activity updater (at most once every 500ms)
    let lastThrottledRecord = 0;
    const handleUserActivity = (event?: Event) => {
        const now = Date.now();
        if (now - lastThrottledRecord > 500) {
            lastThrottledRecord = now;
            lastActivityTime = now;

            // If warning modal was open, user activity immediately clears it
            if (warningActive) {
                warningActive = false;
                callbacks.onWarningCleared?.();
                console.log(
                    `%c[ASCEND Session Manager] 🔄 User activity detected (${event?.type || "action"}). Session extended.`,
                    "color: #10b981;"
                );
            }
        }
    };

    // Attach DOM Activity Listeners
    const activityEvents = [
        "mousedown",
        "mousemove",
        "keydown",
        "scroll",
        "touchstart",
        "click",
        "wheel"
    ];

    activityEvents.forEach((eventType) => {
        window.addEventListener(eventType, handleUserActivity, { passive: true });
    });

    // Check interval (runs every second)
    let lastHeartbeatLoggedAt = Date.now();

    const intervalId = setInterval(() => {
        const now = Date.now();
        const currentTimeoutMins = getIdleTimeoutMinutes();
        const currentTimeoutSecs = currentTimeoutMins * 60;
        
        // Dynamic warning threshold:
        // For standard session (>= 5 min): show warning in last 60 seconds
        // For short test sessions (< 5 min): show warning only in last 25% (e.g. last 7s for 30s timeout)
        const warningThresholdSecs = options?.warningSecondsBeforeTimeout ?? (
            currentTimeoutSecs >= 300 
                ? 60 
                : Math.max(5, Math.floor(currentTimeoutSecs * 0.25))
        );

        const idleSeconds = Math.max(0, (now - lastActivityTime) / 1000);
        const remainingSeconds = Math.max(0, currentTimeoutSecs - idleSeconds);

        // 1. Timeout reached -> Trigger Auto-Logout
        if (idleSeconds >= currentTimeoutSecs) {
            console.log(
                `%c[ASCEND Session Manager] 🛑 Idle timeout reached (${currentTimeoutMins}m). Logging out...`,
                "color: #ef4444; font-weight: bold;"
            );
            clearInterval(intervalId);
            callbacks.onWarningCleared?.();
            callbacks.onTimeout();
            return;
        }

        // 2. Warning Threshold reached (Countdown is active)
        if (remainingSeconds <= warningThresholdSecs) {
            if (!warningActive) {
                warningActive = true;
                console.log(
                    `%c[ASCEND Session Manager] ⏳ Inactivity warning: ${Math.ceil(remainingSeconds)}s remaining before auto-logout.`,
                    "color: #f59e0b; font-weight: bold;"
                );
            }
            callbacks.onWarningCountdown?.(Math.ceil(remainingSeconds));
        } else if (warningActive) {
            warningActive = false;
            callbacks.onWarningCleared?.();
        }

        // 3. Periodic Console Heartbeat Log (Every 60s for long sessions, every 10s for <= 2m sessions)
        const heartbeatSecs = currentTimeoutSecs <= 120 ? 10 : 60;
        if (now - lastHeartbeatLoggedAt >= heartbeatSecs * 1000) {
            lastHeartbeatLoggedAt = now;
            console.log(
                `%c[ASCEND Session Manager]  Session active | Idle: ${formatDuration(idleSeconds)} | Remaining: ${formatDuration(remainingSeconds)}`,
                "color: #0ea5e9;"
            );
        }
    }, 1000);

    // Expose developer helpers to window object for live testing
    (window as any).setIdleTimeoutMinutes = (mins: number) => {
        setIdleTimeoutMinutes(mins);
    };

    (window as any).getIdleSessionStatus = () => {
        const now = Date.now();
        const currentTimeoutMins = getIdleTimeoutMinutes();
        const currentTimeoutSecs = currentTimeoutMins * 60;
        const idleSeconds = Math.max(0, (now - lastActivityTime) / 1000);
        const remainingSeconds = Math.max(0, currentTimeoutSecs - idleSeconds);
        const warningThresholdSecs = currentTimeoutSecs >= 300 ? 60 : Math.max(5, Math.floor(currentTimeoutSecs * 0.25));

        console.table({
            "Configured Timeout": `${currentTimeoutMins} minutes (${currentTimeoutSecs}s)`,
            "Current Inactivity (Idle)": formatDuration(idleSeconds),
            "Time Remaining Before Logout": formatDuration(remainingSeconds),
            "Warning Trigger Threshold": `${warningThresholdSecs}s remaining`,
            "Status": remainingSeconds <= warningThresholdSecs ? "⏳ Warning Modal Visible" : "✅ Active Session"
        });

        return {
            timeoutMinutes: currentTimeoutMins,
            idleSeconds: Math.round(idleSeconds),
            remainingSeconds: Math.round(remainingSeconds)
        };
    };

    // Cleanup function
    return () => {
        activityEvents.forEach((eventType) => {
            window.removeEventListener(eventType, handleUserActivity);
        });
        clearInterval(intervalId);
        globalResetIdleTimer = null;
    };
}
