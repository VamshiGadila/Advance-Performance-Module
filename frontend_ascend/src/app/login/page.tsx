"use client";

import { FormEvent, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { login, resetPassword } from "@/services/authService";
import { ThemeToggle } from "@/context/ThemeContext";

export default function Login() {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [lockoutSeconds, setLockoutSeconds] = useState(0);

    // Direct Forgot Password Modal State (Email + Username verification)
    const [showForgotModal, setShowForgotModal] = useState(false);
    const [forgotEmail, setForgotEmail] = useState("");
    const [forgotUsername, setForgotUsername] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmNewPassword, setConfirmNewPassword] = useState("");
    const [forgotMsg, setForgotMsg] = useState("");
    const [forgotErr, setForgotErr] = useState("");
    const [forgotLoading, setForgotLoading] = useState(false);

    // Format seconds into mm:ss
    const formatLockoutTime = (seconds: number): string => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}:${s < 10 ? "0" : ""}${s}`;
    };

    // User-specific lockout checker
    const checkUserLockout = (userIdentifier: string) => {
        const key = userIdentifier.toLowerCase().trim();
        if (!key) {
            setLockoutSeconds(0);
            return;
        }
        try {
            const stored = localStorage.getItem(`ascend_lockout_${key}`);
            if (stored) {
                const data = JSON.parse(stored);
                if (data.lockoutUntil && data.lockoutUntil > Date.now()) {
                    const remaining = Math.ceil((data.lockoutUntil - Date.now()) / 1000);
                    setLockoutSeconds(remaining);
                    setError(`Account is temporarily locked. Please wait for the lockout countdown before retrying.`);
                    return;
                } else {
                    localStorage.removeItem(`ascend_lockout_${key}`);
                }
            }
        } catch { }
        setLockoutSeconds(0);
        if (error.includes("locked")) {
            setError("");
        }
    };

    // Active Countdown Timer Effect
    useEffect(() => {
        if (lockoutSeconds <= 0) return;

        const interval = setInterval(() => {
            setLockoutSeconds((prev) => {
                if (prev <= 1) {
                    clearInterval(interval);
                    if (email.trim()) {
                        localStorage.removeItem(`ascend_lockout_${email.toLowerCase().trim()}`);
                    }
                    setError("");
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(interval);
    }, [lockoutSeconds, email]);

    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (lockoutSeconds > 0) return;
        setError("");
        setLoading(true);

        try {
            const user = await login({
                email,
                password
            });

            // Clear any active lockout on successful login
            if (email.trim()) {
                localStorage.removeItem(`ascend_lockout_${email.toLowerCase().trim()}`);
            }

            if (user.role === "HR") {
                router.replace("/hr");
            } else if (user.role === "MANAGER") {
                router.replace("/manager");
            } else {
                router.replace("/employee");
            }
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Invalid email or password";
            setError(errMsg);

            // Detect account lockout response from backend
            if (errMsg.toLowerCase().includes("locked")) {
                const isoMatch = errMsg.match(/(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?)/);
                let targetMs = 0;
                if (isoMatch) {
                    const cleanIso = isoMatch[1].replace(/\.(\d{3})\d+/, ".$1");
                    const parsed = new Date(cleanIso).getTime();
                    if (!isNaN(parsed) && parsed > Date.now()) {
                        targetMs = parsed;
                    }
                }
                if (!targetMs) {
                    targetMs = Date.now() + 15 * 60 * 1000;
                }

                const remaining = Math.ceil((targetMs - Date.now()) / 1000);
                setLockoutSeconds(remaining);

                if (email.trim()) {
                    try {
                        localStorage.setItem(
                            `ascend_lockout_${email.toLowerCase().trim()}`,
                            JSON.stringify({ lockoutUntil: targetMs, email: email.trim() })
                        );
                    } catch { }
                }
            }
        } finally {
            setLoading(false);
        }
    }


    const handleResetPassword = async (e: FormEvent) => {
        e.preventDefault();
        setForgotErr("");
        setForgotMsg("");

        if (newPassword !== confirmNewPassword) {
            setForgotErr("Passwords do not match");
            return;
        }

        setForgotLoading(true);
        try {
            const res = await resetPassword({
                email: forgotEmail,
                username: forgotUsername,
                newPassword,
                confirmPassword: confirmNewPassword
            });
            setForgotMsg(res || "Password successfully reset! You can now sign in.");
            setTimeout(() => {
                setShowForgotModal(false);
                setForgotMsg("");
            }, 3000);
        } catch (err: any) {
            setForgotErr(err.message || "Failed to reset password. Please check your email and username.");
        } finally {
            setForgotLoading(false);
        }
    };

    return (
        <main className="auth-wrapper">
            <div style={{ position: "absolute", top: "24px", right: "24px", zIndex: 50 }}>
                <ThemeToggle />
            </div>

            <div className="auth-card">
                {/* BRAND HEADER */}
                <div className="auth-header">
                    <div
                        style={{
                            width: "48px",
                            height: "48px",
                            margin: "0 auto 14px",
                            background: "#4f46e5",
                            borderRadius: "12px",
                            display: "grid",
                            placeItems: "center",
                            boxShadow: "0 4px 14px rgba(79, 70, 229, 0.35)",
                            color: "#ffffff"
                        }}
                    >
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                        </svg>
                    </div>
                    <h1 className="auth-brand">ASCEND</h1>
                    <p className="auth-sub">Enterprise Performance Management Workspace</p>
                </div>

                {error && (
                    <div className="alert alert-error" style={{ display: "flex", alignItems: "flex-start", gap: "12px" }}>
                        <span className="alert-icon" style={lockoutSeconds > 0 ? { background: "rgba(239, 68, 68, 0.15)", color: "#ef4444" } : undefined}>
                            {lockoutSeconds > 0 ? "⏳" : "!"}
                        </span>
                        <div style={{ flex: 1 }}>
                            {lockoutSeconds > 0 ? (
                                <>
                                    <div style={{ fontWeight: "700", color: "#ef4444", marginBottom: "3px" }}>
                                        Account Temporarily Locked
                                    </div>
                                    <div style={{ fontSize: "0.85rem", color: "var(--text-secondary)", lineHeight: "1.4" }}>
                                        Security lockout triggered due to repeated failed logins. Sign in is disabled for this user for <strong>{formatLockoutTime(lockoutSeconds)}</strong>.
                                    </div>
                                </>
                            ) : (
                                <span>{error}</span>
                            )}
                        </div>
                    </div>
                )}

                <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
                    <div className="form-group">
                        <label className="form-label">Employee ID or Work Email</label>
                        <div style={{ position: "relative" }}>
                            <input
                                type="text"
                                className="form-input"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    checkUserLockout(e.target.value);
                                }}
                                placeholder="e.g. EMP001 or hr@ascend.local"
                                style={{ paddingLeft: "42px" }}
                                required
                            />
                            <div style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                    <circle cx="12" cy="7" r="4" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <div className="form-group">
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "4px" }}>
                            <label className="form-label" style={{ marginBottom: 0 }}>Password</label>
                            <button
                                type="button"
                                onClick={() => {
                                    setShowForgotModal(true);
                                    setForgotEmail(email || "");
                                    setForgotUsername("");
                                    setNewPassword("");
                                    setConfirmNewPassword("");
                                    setForgotMsg("");
                                    setForgotErr("");
                                }}
                                style={{
                                    background: "none",
                                    border: "none",
                                    color: "var(--primary)",
                                    fontSize: "0.775rem",
                                    fontWeight: "600",
                                    cursor: "pointer",
                                    padding: 0
                                }}
                            >
                                Forgot Password?
                            </button>
                        </div>
                        <div style={{ position: "relative" }}>
                            <input
                                type="password"
                                className="form-input"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                style={{ paddingLeft: "42px" }}
                                required
                            />
                            <div style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{
                            width: "100%",
                            padding: "13px",
                            marginTop: "4px",
                            fontSize: "0.95rem",
                            fontWeight: "700",
                            background: lockoutSeconds > 0 ? "rgba(239, 68, 68, 0.12)" : undefined,
                            borderColor: lockoutSeconds > 0 ? "rgba(239, 68, 68, 0.35)" : undefined,
                            color: lockoutSeconds > 0 ? "#ef4444" : undefined,
                            cursor: lockoutSeconds > 0 ? "not-allowed" : "pointer"
                        }}
                        disabled={loading || lockoutSeconds > 0}
                    >
                        {lockoutSeconds > 0 ? (
                            <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px" }}>
                                <span>⏳</span>
                                <span>Account Locked — Retry in {formatLockoutTime(lockoutSeconds)}</span>
                            </span>
                        ) : loading ? (
                            <span>Signing in...</span>
                        ) : (
                            <span style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                                Sign in to Workspace
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="5" y1="12" x2="19" y2="12" />
                                    <polyline points="12 5 19 12 12 19" />
                                </svg>
                            </span>
                        )}
                    </button>

                    <div style={{ textAlign: "center", marginTop: "10px", fontSize: "0.875rem", color: "var(--text-muted)" }}>
                        New team member?{" "}
                        <Link href="/signup" style={{ color: "var(--primary)", fontWeight: "600", textDecoration: "none" }}>
                            Create account →
                        </Link>
                    </div>
                </form>
            </div>

            {/* DIRECT FORGOT & RESET PASSWORD MODAL (EMAIL + USERNAME) */}
            {showForgotModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "460px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                            <h3 style={{ margin: 0, fontSize: "1.25rem", fontWeight: "700", color: "#ffffff" }}>
                                🔑 Reset Password
                            </h3>
                            <button
                                type="button"
                                onClick={() => setShowForgotModal(false)}
                                className="btn-close"
                                title="Close modal"
                            >
                                ✕
                            </button>
                        </div>

                        <p style={{ margin: "0 0 16px 0", fontSize: "0.825rem", color: "var(--text-secondary)" }}>
                            Verify your registered email and username / employee code to instantly set a new password.
                        </p>

                        {forgotMsg && (
                            <div className="alert alert-success" style={{ marginBottom: "16px", padding: "12px 16px" }}>
                                <span className="alert-icon">✓</span>
                                <span>{forgotMsg}</span>
                            </div>
                        )}

                        {forgotErr && (
                            <div className="alert alert-error" style={{ marginBottom: "16px", padding: "12px 16px" }}>
                                <span className="alert-icon">!</span>
                                <span>{forgotErr}</span>
                            </div>
                        )}

                        <form onSubmit={handleResetPassword} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                            <div className="form-group">
                                <label className="form-label" style={{ fontSize: "0.8rem" }}>Registered Work Email</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    value={forgotEmail}
                                    onChange={(e) => setForgotEmail(e.target.value)}
                                    placeholder="emp1@ascend.local or vamshi12@gmail.com"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label" style={{ fontSize: "0.8rem" }}>Username / Employee Code or Full Name</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={forgotUsername}
                                    onChange={(e) => setForgotUsername(e.target.value)}
                                    placeholder="e.g. EMP001, John Doe, or emp1"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label" style={{ fontSize: "0.8rem" }}>New Password</label>
                                <input
                                    type="password"
                                    className="form-input"
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    placeholder="Min 6 characters"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label" style={{ fontSize: "0.8rem" }}>Confirm New Password</label>
                                <input
                                    type="password"
                                    className="form-input"
                                    value={confirmNewPassword}
                                    onChange={(e) => setConfirmNewPassword(e.target.value)}
                                    placeholder="Re-enter new password"
                                    required
                                />
                            </div>

                            <div style={{ display: "flex", gap: "10px", marginTop: "8px" }}>
                                <button
                                    type="button"
                                    onClick={() => setShowForgotModal(false)}
                                    className="btn"
                                    style={{ flex: 1, padding: "10px", background: "rgba(255,255,255,0.06)", color: "var(--text-secondary)" }}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    style={{ flex: 2, padding: "10px" }}
                                    disabled={forgotLoading}
                                >
                                    {forgotLoading ? "Verifying..." : "Verify & Reset ✓"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </main>
    );
}