"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { Shield, User, Mail, Lock, Eye, EyeOff, CheckCircle2, AlertCircle, ArrowRight } from "lucide-react";

export default function SignupPage() {
    const router = useRouter();
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        // Client-side validations
        if (!name.trim()) {
            setError("Full name is required.");
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email.trim())) {
            setError("Please enter a valid work email address.");
            return;
        }

        if (password.length < 6) {
            setError("Password must be at least 6 characters long.");
            return;
        }

        if (password !== confirmPassword) {
            setError("Passwords do not match. Please verify.");
            return;
        }

        setLoading(true);

        try {
            await api<{ token?: string; message?: string; role?: string; email?: string }>(
                "/api/auth/signup",
                {
                    method: "POST",
                    body: JSON.stringify({
                        name: name.trim(),
                        email: email.trim().toLowerCase(),
                        password
                    })
                }
            );

            setSuccess("Account created successfully! Redirecting to login...");
            setTimeout(() => {
                router.push("/login");
            }, 1500);
        } catch (err: any) {
            setError(err.message || "Signup failed. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const passwordsMatch = confirmPassword.length > 0 && password === confirmPassword;
    const passwordsMismatch = confirmPassword.length > 0 && password !== confirmPassword;

    return (
        <main className="auth-wrapper">
            <div className="auth-card" style={{ maxWidth: "460px" }}>
                <div style={{ textAlign: "center", marginBottom: "22px" }}>
                    <div style={{
                        width: "48px",
                        height: "48px",
                        margin: "0 auto 12px",
                        background: "rgba(16, 185, 129, 0.15)",
                        border: "1px solid rgba(16, 185, 129, 0.3)",
                        borderRadius: "12px",
                        display: "grid",
                        placeItems: "center",
                        color: "#10b981"
                    }}>
                        <Shield size={26} />
                    </div>
                    <h1 style={{ fontSize: "1.4rem", fontWeight: "800", color: "#f8fafc" }}>Create Staff Account</h1>
                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", marginTop: "4px" }}>
                        HRMS Policies & Compliance Portal
                    </p>
                </div>

                {error && (
                    <div className="alert-error" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <AlertCircle size={18} style={{ flexShrink: 0 }} />
                        <span>{error}</span>
                    </div>
                )}

                {success && (
                    <div className="alert-success" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
                        <span>{success}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    {/* Full Name */}
                    <div className="form-group">
                        <label className="form-label" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <User size={14} /> Full Name
                        </label>
                        <input
                            type="text"
                            className="form-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="e.g. Alex Morgan"
                            required
                            disabled={loading}
                        />
                    </div>

                    {/* Work Email */}
                    <div className="form-group">
                        <label className="form-label" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <Mail size={14} /> Work Email
                        </label>
                        <input
                            type="email"
                            className="form-input"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="alex@company.com"
                            required
                            disabled={loading}
                        />
                    </div>

                    {/* Password */}
                    <div className="form-group">
                        <label className="form-label" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <Lock size={14} /> Password
                        </label>
                        <div style={{ position: "relative" }}>
                            <input
                                type={showPassword ? "text" : "password"}
                                className="form-input"
                                style={{ paddingRight: "40px" }}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Min. 6 characters"
                                minLength={6}
                                required
                                disabled={loading}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                style={{
                                    position: "absolute",
                                    right: "12px",
                                    top: "50%",
                                    transform: "translateY(-50%)",
                                    background: "transparent",
                                    border: "none",
                                    color: "var(--text-muted)",
                                    cursor: "pointer",
                                    display: "flex",
                                    alignItems: "center"
                                }}
                                tabIndex={-1}
                            >
                                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </div>

                    {/* Confirm Password */}
                    <div className="form-group">
                        <label className="form-label" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <Lock size={14} /> Confirm Password
                        </label>
                        <div style={{ position: "relative" }}>
                            <input
                                type={showConfirmPassword ? "text" : "password"}
                                className="form-input"
                                style={{
                                    paddingRight: "40px",
                                    borderColor: passwordsMismatch
                                        ? "rgba(239, 68, 68, 0.6)"
                                        : passwordsMatch
                                        ? "rgba(16, 185, 129, 0.6)"
                                        : undefined
                                }}
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Re-type your password"
                                minLength={6}
                                required
                                disabled={loading}
                            />
                            <button
                                type="button"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                style={{
                                    position: "absolute",
                                    right: "12px",
                                    top: "50%",
                                    transform: "translateY(-50%)",
                                    background: "transparent",
                                    border: "none",
                                    color: "var(--text-muted)",
                                    cursor: "pointer",
                                    display: "flex",
                                    alignItems: "center"
                                }}
                                tabIndex={-1}
                            >
                                {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                        {passwordsMismatch && (
                            <span style={{ fontSize: "0.75rem", color: "#f87171", marginTop: "2px" }}>
                                Passwords do not match
                            </span>
                        )}
                        {passwordsMatch && (
                            <span style={{ fontSize: "0.75rem", color: "#34d399", marginTop: "2px" }}>
                                Passwords match
                            </span>
                        )}
                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: "100%", marginTop: "10px" }}
                        disabled={loading || passwordsMismatch}
                    >
                        {loading ? "Creating Account..." : "Create Account"}
                    </button>
                </form>

                {/* Divider */}
                <div style={{ display: "flex", alignItems: "center", margin: "18px 0", gap: "10px" }}>
                    <div style={{ flex: 1, height: "1px", background: "var(--border)" }} />
                    <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", textTransform: "uppercase" }}>OR</span>
                    <div style={{ flex: 1, height: "1px", background: "var(--border)" }} />
                </div>

                {/* Google Sign-up / Sign-in */}
                <a
                    href="http://localhost:8081/oauth2/authorization/google"
                    className="btn btn-secondary"
                    style={{
                        width: "100%",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: "10px",
                        padding: "11px"
                    }}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                    </svg>
                    Continue with Google
                </a>

                {/* Login Link */}
                <div style={{ textAlign: "center", marginTop: "18px", fontSize: "0.85rem", color: "var(--text-muted)" }}>
                    Already have an account?{" "}
                    <Link href="/login" style={{ color: "#34d399", fontWeight: "600", display: "inline-flex", alignItems: "center", gap: "2px" }}>
                        Sign in here <ArrowRight size={14} />
                    </Link>
                </div>
            </div>
        </main>
    );
}
