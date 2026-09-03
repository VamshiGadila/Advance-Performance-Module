"use client";

import { FormEvent, useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { forgotPassword, verifyResetOtp, resetPassword } from "@/services/authService";
import { ThemeToggle } from "@/context/ThemeContext";
import {
    KeyRound,
    Mail,
    ArrowLeft,
    CheckCircle2,
    AlertCircle,
    RefreshCw,
    ShieldCheck,
    Lock,
    Eye,
    EyeOff,
    Check
} from "lucide-react";

const EMAIL_REGEX = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/;

export default function ForgotPassword() {
    const router = useRouter();

    // Steps: 1 = Enter Email, 2 = Verify OTP, 3 = Set New Password, 4 = Success
    const [step, setStep] = useState<1 | 2 | 3 | 4>(1);

    const [email, setEmail] = useState("");
    const [otpDigits, setOtpDigits] = useState<string[]>(["", "", "", "", "", ""]);
    const [resetAuthorization, setResetAuthorization] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const [loading, setLoading] = useState(false);
    const [resending, setResending] = useState(false);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");
    const [resendCooldown, setResendCooldown] = useState(0);
    const [otpExpiresIn, setOtpExpiresIn] = useState(600); // 10 minutes = 600s

    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

    // Resend cooldown timer (60s)
    useEffect(() => {
        if (resendCooldown <= 0) return;
        const timer = setInterval(() => {
            setResendCooldown((prev) => (prev <= 1 ? 0 : prev - 1));
        }, 1000);
        return () => clearInterval(timer);
    }, [resendCooldown]);

    // OTP Expiry countdown timer (10 mins)
    useEffect(() => {
        if (step !== 2 || otpExpiresIn <= 0) return;
        const timer = setInterval(() => {
            setOtpExpiresIn((prev) => (prev <= 1 ? 0 : prev - 1));
        }, 1000);
        return () => clearInterval(timer);
    }, [step, otpExpiresIn]);

    const formatMinutes = (seconds: number) => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}:${s < 10 ? "0" : ""}${s}`;
    };

    // Handle 6-digit OTP Input changes
    const handleOtpChange = (index: number, value: string) => {
        if (value.length > 1) {
            // Handle pasting complete 6-digit code
            const pasted = value.replace(/\D/g, "").slice(0, 6).split("");
            const newDigits = [...otpDigits];
            pasted.forEach((char, i) => {
                if (i < 6) newDigits[i] = char;
            });
            setOtpDigits(newDigits);
            const nextIdx = Math.min(pasted.length, 5);
            inputRefs.current[nextIdx]?.focus();
            return;
        }

        const digit = value.replace(/\D/g, "");
        const newDigits = [...otpDigits];
        newDigits[index] = digit;
        setOtpDigits(newDigits);

        if (digit && index < 5) {
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handleOtpKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Backspace" && !otpDigits[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const fullOtp = otpDigits.join("");
    const isOtpLockedOut = error.toLowerCase().includes("locked") || error.toLowerCase().includes("lockout");

    // Password Policy Regex Evaluation
    const hasMinLength = newPassword.length >= 12;
    const hasUppercase = /[A-Z]/.test(newPassword);
    const hasLowercase = /[a-z]/.test(newPassword);
    const hasNumber = /[0-9]/.test(newPassword);
    const hasSpecial = /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?/~`]/.test(newPassword);
    const isNewPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;

    // Step 1: Send OTP to Email
    const handleSendOtp = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccessMsg("");

        const cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail || !EMAIL_REGEX.test(cleanEmail)) {
            setError("Please enter a valid work email address (e.g. name@company.com).");
            return;
        }

        setLoading(true);
        try {
            const message = await forgotPassword(cleanEmail);
            setSuccessMsg(message || "A verification OTP has been sent to your email.");
            setStep(2);
            setResendCooldown(60);
            setOtpExpiresIn(600); // 10 minutes fresh validity
            setTimeout(() => {
                inputRefs.current[0]?.focus();
            }, 100);
        } catch (err: any) {
            setError(err.message || "Failed to send verification code. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    // Step 2: Verify OTP
    const handleVerifyOtp = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccessMsg("");

        if (fullOtp.length !== 6) {
            setError("Please enter all 6 digits of the verification code.");
            return;
        }

        if (otpExpiresIn <= 0) {
            setError("This OTP has expired. Please request a new OTP.");
            return;
        }

        setLoading(true);
        try {
            const res = await verifyResetOtp(email.trim().toLowerCase(), fullOtp);
            setResetAuthorization(res.resetAuthorization);
            setSuccessMsg(res.message || "OTP verified successfully. You can now create a new password.");
            setStep(3);
        } catch (err: any) {
            setError(err.message || "Incorrect OTP code. Please check and try again.");
        } finally {
            setLoading(false);
        }
    };

    // Resend OTP Action
    const handleResendOtp = async () => {
        if (resendCooldown > 0 || resending) return;
        setError("");
        setSuccessMsg("");
        setResending(true);

        try {
            const message = await forgotPassword(email.trim().toLowerCase());
            setSuccessMsg(message || "A new OTP has been sent to your email.");
            setResendCooldown(60);
            setOtpExpiresIn(600); // Fresh 10 minutes on successful resend
            setOtpDigits(["", "", "", "", "", ""]);
            inputRefs.current[0]?.focus();
        } catch (err: any) {
            setError(err.message || "Please wait before requesting another OTP.");
        } finally {
            setResending(false);
        }
    };

    // Step 3: Reset Password
    const handleResetPassword = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccessMsg("");

        if (!hasMinLength) {
            setError("New password must be at least 12 characters long.");
            return;
        }

        if (!hasUppercase) {
            setError("Password must contain at least one uppercase letter (A-Z).");
            return;
        }

        if (!hasLowercase) {
            setError("Password must contain at least one lowercase letter (a-z).");
            return;
        }

        if (!hasNumber) {
            setError("Password must contain at least one number (0-9).");
            return;
        }

        if (!hasSpecial) {
            setError("Password must contain at least one special character (e.g. !@#$%^&*).");
            return;
        }

        if (newPassword !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        setLoading(true);
        try {
            await resetPassword({
                resetAuthorization,
                email: email.trim().toLowerCase(),
                otp: fullOtp,
                newPassword,
                confirmPassword
            });
            setSuccessMsg("Password reset successfully. Please log in again.");
            setStep(4);
            setTimeout(() => {
                router.push("/login");
            }, 2500);
        } catch (err: any) {
            setError(err.message || "Failed to reset password. Please check policy requirements and try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="auth-wrapper">
            {/* Theme Toggle in top-right */}
            <div style={{ position: "absolute", top: "24px", right: "24px", zIndex: 50 }}>
                <ThemeToggle />
            </div>

            <div className="auth-card" style={{ maxWidth: "460px" }}>
                {/* Brand Header */}
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
                    <p className="auth-sub">Enterprise Security & Identity Gateway</p>
                </div>

                {/* Progress Step Indicator */}
                {step < 4 && (
                    <div style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: "8px",
                        marginBottom: "22px",
                        padding: "8px 14px",
                        background: "var(--bg-subtle)",
                        borderRadius: "12px",
                        border: "1px solid var(--border)"
                    }}>
                        <div style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                            fontSize: "0.75rem",
                            fontWeight: step === 1 ? "800" : "600",
                            color: step === 1 ? "var(--primary)" : step > 1 ? "#10b981" : "var(--text-muted)"
                        }}>
                            <span style={{
                                width: "18px",
                                height: "18px",
                                borderRadius: "50%",
                                background: step > 1 ? "#10b981" : step === 1 ? "var(--primary)" : "var(--border)",
                                color: "#ffffff",
                                display: "grid",
                                placeItems: "center",
                                fontSize: "0.7rem",
                                fontWeight: "800"
                            }}>
                                {step > 1 ? "✓" : "1"}
                            </span>
                            <span>Email</span>
                        </div>

                        <span style={{ color: "var(--border)", fontSize: "0.75rem" }}>—</span>

                        <div style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                            fontSize: "0.75rem",
                            fontWeight: step === 2 ? "800" : "600",
                            color: step === 2 ? "var(--primary)" : step > 2 ? "#10b981" : "var(--text-muted)"
                        }}>
                            <span style={{
                                width: "18px",
                                height: "18px",
                                borderRadius: "50%",
                                background: step > 2 ? "#10b981" : step === 2 ? "var(--primary)" : "var(--border)",
                                color: "#ffffff",
                                display: "grid",
                                placeItems: "center",
                                fontSize: "0.7rem",
                                fontWeight: "800"
                            }}>
                                {step > 2 ? "✓" : "2"}
                            </span>
                            <span>Verify OTP</span>
                        </div>

                        <span style={{ color: "var(--border)", fontSize: "0.75rem" }}>—</span>

                        <div style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                            fontSize: "0.75rem",
                            fontWeight: step === 3 ? "800" : "600",
                            color: step === 3 ? "var(--primary)" : "var(--text-muted)"
                        }}>
                            <span style={{
                                width: "18px",
                                height: "18px",
                                borderRadius: "50%",
                                background: step === 3 ? "var(--primary)" : "var(--border)",
                                color: "#ffffff",
                                display: "grid",
                                placeItems: "center",
                                fontSize: "0.7rem",
                                fontWeight: "800"
                            }}>
                                3
                            </span>
                            <span>New Password</span>
                        </div>
                    </div>
                )}

                {/* Card Title & Icon */}
                <div style={{ textAlign: "center", marginBottom: "20px" }}>
                    <div style={{
                        width: "44px",
                        height: "44px",
                        borderRadius: "12px",
                        background: step === 4 ? "rgba(16, 185, 129, 0.15)" : "rgba(99, 102, 241, 0.12)",
                        border: `1px solid ${step === 4 ? "rgba(16, 185, 129, 0.3)" : "rgba(99, 102, 241, 0.25)"}`,
                        display: "grid",
                        placeItems: "center",
                        margin: "0 auto 10px",
                        color: step === 4 ? "#10b981" : "var(--primary)"
                    }}>
                        {step === 1 && <Mail size={22} />}
                        {step === 2 && <KeyRound size={22} />}
                        {step === 3 && <Lock size={22} />}
                        {step === 4 && <CheckCircle2 size={24} style={{ color: "#10b981" }} />}
                    </div>

                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                        {step === 1 && "Forgot Your Password?"}
                        {step === 2 && "Enter Verification Code"}
                        {step === 3 && "Set New Password"}
                        {step === 4 && "Password Reset Complete"}
                    </h2>
                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", marginTop: "4px", lineHeight: 1.4 }}>
                        {step === 1 && "Enter your registered Gmail or work email to receive a 6-digit OTP code."}
                        {step === 2 && (
                            <span>
                                We dispatched a 6-digit OTP to <strong style={{ color: "var(--text-main)" }}>{email}</strong>
                            </span>
                        )}
                        {step === 3 && "Create a secure new password for your ASCEND account."}
                        {step === 4 && "Your password has been successfully updated. All sessions are secured."}
                    </p>
                </div>

                {/* Clean Alert Banner: Error */}
                {error && (
                    <div className="alert-banner alert-error" style={{ marginBottom: "16px", display: "flex", alignItems: "flex-start", gap: "10px" }}>
                        <AlertCircle size={17} style={{ flexShrink: 0, marginTop: "2px" }} />
                        <span style={{ flex: 1, fontSize: "0.84rem", lineHeight: 1.4 }}>{error}</span>
                    </div>
                )}

                {/* Clean Alert Banner: Success */}
                {successMsg && step !== 4 && (
                    <div className="alert-banner alert-success" style={{ marginBottom: "16px", display: "flex", alignItems: "flex-start", gap: "10px" }}>
                        <CheckCircle2 size={17} style={{ flexShrink: 0, marginTop: "2px" }} />
                        <span style={{ flex: 1, fontSize: "0.84rem", lineHeight: 1.4 }}>{successMsg}</span>
                    </div>
                )}

                {/* ================= STEP 1: ENTER EMAIL ================= */}
                {step === 1 && (
                    <form onSubmit={handleSendOtp} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div className="form-group">
                            <label className="form-label">Registered Gmail / Work Email *</label>
                            <div style={{ position: "relative" }}>
                                <input
                                    type="email"
                                    required
                                    className="form-input"
                                    style={{ paddingLeft: "38px" }}
                                    placeholder="e.g. yourname@gmail.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoFocus
                                />
                                <Mail size={16} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading}
                            style={{ width: "100%", padding: "12px", display: "flex", justifyContent: "center", alignItems: "center", gap: "8px", fontWeight: "700" }}
                        >
                            {loading ? (
                                <>
                                    <RefreshCw size={16} className="animate-spin" />
                                    <span>Sending OTP Code...</span>
                                </>
                            ) : (
                                <span>Send 6-Digit OTP →</span>
                            )}
                        </button>

                        <div style={{ textAlign: "center", marginTop: "4px" }}>
                            <Link href="/login" style={{ fontSize: "0.825rem", color: "var(--text-muted)", textDecoration: "none", display: "inline-flex", alignItems: "center", gap: "6px" }}>
                                <ArrowLeft size={14} />
                                <span>Back to Login</span>
                            </Link>
                        </div>
                    </form>
                )}

                {/* ================= STEP 2: VERIFY OTP ONLY ================= */}
                {step === 2 && (
                    <form onSubmit={handleVerifyOtp} style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
                        {/* Email recipient card */}
                        <div style={{
                            padding: "10px 14px",
                            background: "var(--bg-subtle)",
                            borderRadius: "10px",
                            border: "1px solid var(--border)",
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center"
                        }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "8px", overflow: "hidden" }}>
                                <Mail size={14} style={{ color: "var(--primary)", flexShrink: 0 }} />
                                <span style={{ fontSize: "0.82rem", color: "var(--text-main)", fontWeight: "600", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                    {email}
                                </span>
                            </div>
                            <button
                                type="button"
                                onClick={() => {
                                    setStep(1);
                                    setError("");
                                    setSuccessMsg("");
                                    setOtpDigits(["", "", "", "", "", ""]);
                                }}
                                style={{
                                    background: "transparent",
                                    border: "none",
                                    color: "var(--primary)",
                                    fontSize: "0.78rem",
                                    fontWeight: "700",
                                    cursor: "pointer",
                                    padding: "2px 6px"
                                }}
                            >
                                Change
                            </button>
                        </div>

                        {/* 6-box OTP input */}
                        <div className="form-group">
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                <label className="form-label" style={{ margin: 0 }}>6-Digit Verification Code *</label>
                                <span style={{ fontSize: "0.75rem", color: otpExpiresIn <= 60 ? "#ef4444" : "var(--text-muted)", fontWeight: "700" }}>
                                    {otpExpiresIn > 0 ? `⏱️ Valid for ${formatMinutes(otpExpiresIn)}` : "⚠️ OTP Expired"}
                                </span>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "repeat(6, 1fr)", gap: "8px" }}>
                                {otpDigits.map((digit, index) => (
                                    <input
                                        key={index}
                                        ref={(el) => { inputRefs.current[index] = el; }}
                                        type="text"
                                        inputMode="numeric"
                                        maxLength={6}
                                        disabled={loading || isOtpLockedOut}
                                        value={digit}
                                        onChange={(e) => handleOtpChange(index, e.target.value)}
                                        onKeyDown={(e) => handleOtpKeyDown(index, e)}
                                        className="form-input"
                                        style={{
                                            textAlign: "center",
                                            fontSize: "1.25rem",
                                            fontWeight: "800",
                                            padding: "10px 0",
                                            borderRadius: "10px",
                                            borderColor: digit ? "var(--primary)" : "var(--border)",
                                            background: digit ? "rgba(99, 102, 241, 0.05)" : "var(--bg-surface)",
                                            opacity: isOtpLockedOut ? 0.6 : 1
                                        }}
                                    />
                                ))}
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading || fullOtp.length !== 6 || otpExpiresIn <= 0 || isOtpLockedOut}
                            style={{
                                width: "100%",
                                padding: "12px",
                                display: "flex",
                                justifyContent: "center",
                                alignItems: "center",
                                gap: "8px",
                                fontWeight: "700",
                                opacity: isOtpLockedOut ? 0.6 : 1
                            }}
                        >
                            {loading ? (
                                <>
                                    <RefreshCw size={16} className="animate-spin" />
                                    <span>Verifying OTP Code...</span>
                                </>
                            ) : (
                                <>
                                    <ShieldCheck size={17} />
                                    <span>Verify OTP Code →</span>
                                </>
                            )}
                        </button>

                        {/* Resend OTP button */}
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: "0.8rem", marginTop: "2px" }}>
                            <button
                                type="button"
                                onClick={handleResendOtp}
                                disabled={resendCooldown > 0 || resending || isOtpLockedOut}
                                style={{
                                    background: "transparent",
                                    border: "none",
                                    color: (resendCooldown > 0 || isOtpLockedOut) ? "var(--text-muted)" : "var(--primary)",
                                    fontWeight: "600",
                                    cursor: (resendCooldown > 0 || isOtpLockedOut) ? "not-allowed" : "pointer",
                                    display: "inline-flex",
                                    alignItems: "center",
                                    gap: "5px",
                                    padding: 0
                                }}
                            >
                                <RefreshCw size={13} className={resending ? "animate-spin" : ""} />
                                <span>{resending ? "Resending..." : resendCooldown > 0 ? `Resend OTP in ${resendCooldown}s` : "Resend OTP"}</span>
                            </button>

                            <Link href="/login" style={{ color: "var(--text-muted)", textDecoration: "none" }}>
                                Cancel
                            </Link>
                        </div>
                    </form>
                )}

                {/* ================= STEP 3: RESET PASSWORD ================= */}
                {step === 3 && (
                    <form onSubmit={handleResetPassword} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div style={{
                            padding: "10px 14px",
                            background: "rgba(16, 185, 129, 0.1)",
                            borderRadius: "10px",
                            border: "1px solid rgba(16, 185, 129, 0.25)",
                            display: "flex",
                            alignItems: "center",
                            gap: "8px"
                        }}>
                            <Check size={15} style={{ color: "#10b981", flexShrink: 0 }} />
                            <span style={{ fontSize: "0.8rem", color: "var(--text-main)", fontWeight: "600" }}>
                                OTP Verified! Create your new password below.
                            </span>
                        </div>

                        <div className="form-group">
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "4px" }}>
                                <label className="form-label" style={{ margin: 0 }}>New Password *</label>
                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)" }}>Min 12 characters</span>
                            </div>
                            <div style={{ position: "relative" }}>
                                <input
                                    type={showNewPassword ? "text" : "password"}
                                    required
                                    minLength={12}
                                    maxLength={128}
                                    className="form-input"
                                    style={{ paddingLeft: "38px", paddingRight: "38px" }}
                                    placeholder="At least 12 characters"
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    autoFocus
                                />
                                <Lock size={16} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
                                <button
                                    type="button"
                                    onClick={() => setShowNewPassword(!showNewPassword)}
                                    style={{ position: "absolute", right: "12px", top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "var(--text-muted)", cursor: "pointer" }}
                                >
                                    {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                                </button>
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Confirm New Password *</label>
                            <div style={{ position: "relative" }}>
                                <input
                                    type={showConfirmPassword ? "text" : "password"}
                                    required
                                    minLength={12}
                                    maxLength={128}
                                    className="form-input"
                                    style={{ paddingLeft: "38px", paddingRight: "38px" }}
                                    placeholder="Re-type new password"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                />
                                <Lock size={16} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    style={{ position: "absolute", right: "12px", top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "var(--text-muted)", cursor: "pointer" }}
                                >
                                    {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                                </button>
                            </div>
                        </div>

                        {/* Live Password Policy Checklist */}
                        <div style={{
                            background: "var(--bg-subtle)",
                            padding: "12px 14px",
                            borderRadius: "10px",
                            border: "1px solid var(--border)",
                            fontSize: "0.75rem",
                            display: "flex",
                            flexDirection: "column",
                            gap: "6px"
                        }}>
                            <span style={{ fontWeight: "700", color: "var(--text-muted)" }}>Password Security Rules:</span>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "5px" }}>
                                <span style={{ color: hasMinLength ? "#10b981" : "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                                    {hasMinLength ? "✓" : "○"} 12+ characters
                                </span>
                                <span style={{ color: hasUppercase ? "#10b981" : "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                                    {hasUppercase ? "✓" : "○"} Uppercase (A-Z)
                                </span>
                                <span style={{ color: hasLowercase ? "#10b981" : "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                                    {hasLowercase ? "✓" : "○"} Lowercase (a-z)
                                </span>
                                <span style={{ color: hasNumber ? "#10b981" : "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                                    {hasNumber ? "✓" : "○"} Number (0-9)
                                </span>
                                <span style={{ color: hasSpecial ? "#10b981" : "var(--text-muted)", display: "flex", alignItems: "center", gap: "4px" }}>
                                    {hasSpecial ? "✓" : "○"} Special char (!@#$...)
                                </span>
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading || !newPassword || !confirmPassword || !isNewPasswordValid || newPassword !== confirmPassword}
                            style={{ width: "100%", padding: "12px", display: "flex", justifyContent: "center", alignItems: "center", gap: "8px", fontWeight: "700" }}
                        >
                            {loading ? (
                                <>
                                    <RefreshCw size={16} className="animate-spin" />
                                    <span>Saving New Password...</span>
                                </>
                            ) : (
                                <span>Reset & Save Password →</span>
                            )}
                        </button>
                    </form>
                )}

                {/* ================= STEP 4: SUCCESS CONFIRMATION ================= */}
                {step === 4 && (
                    <div style={{ display: "flex", flexDirection: "column", gap: "18px", textAlign: "center" }}>
                        <div style={{
                            padding: "16px",
                            background: "rgba(16, 185, 129, 0.08)",
                            borderRadius: "14px",
                            border: "1px solid rgba(16, 185, 129, 0.25)"
                        }}>
                            <p style={{ margin: 0, fontSize: "0.88rem", color: "var(--text-main)", lineHeight: 1.5 }}>
                                Your password has been updated and all security tokens have been reset. You can now log into your ASCEND account.
                            </p>
                        </div>

                        <Link
                            href="/login"
                            className="btn btn-primary"
                            style={{ width: "100%", padding: "12px", display: "inline-flex", justifyContent: "center", alignItems: "center", gap: "8px", fontWeight: "700", textDecoration: "none" }}
                        >
                            <span>Return to Login</span>
                            <ArrowLeft size={16} style={{ transform: "rotate(180deg)" }} />
                        </Link>
                    </div>
                )}
            </div>
        </main>
    );
}
