"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signup, getPublicDepartments, PublicDepartment } from "@/services/authService";
import { ThemeToggle } from "@/context/ThemeContext";
import { Eye, EyeOff } from "lucide-react";

const EMAIL_REGEX = /^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/;

export default function Signup() {
    const router = useRouter();

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [departmentId, setDepartmentId] = useState<string>("");
    const [departments, setDepartments] = useState<PublicDepartment[]>([]);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        getPublicDepartments()
            .then((data) => {
                setDepartments(data);
                if (data.length > 0) {
                    setDepartmentId(String(data[0].id));
                }
            })
            .catch(() => {});
    }, []);

    const isEmailValid = Boolean(email) && EMAIL_REGEX.test(email.trim());
    const hasMinLength = password.length >= 12;
    const hasUppercase = /[A-Z]/.test(password);
    const hasLowercase = /[a-z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecial = /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?/~`]/.test(password);
    const isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;

    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError("");
        setSuccess("");

        const cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail || !EMAIL_REGEX.test(cleanEmail)) {
            setError("Please enter a valid email address (e.g. name@company.com)");
            return;
        }

        if (!hasMinLength) {
            setError("Password must be at least 12 characters long");
            return;
        }
        if (!hasUppercase) {
            setError("Password must contain at least one uppercase letter (A-Z)");
            return;
        }
        if (!hasLowercase) {
            setError("Password must contain at least one lowercase letter (a-z)");
            return;
        }
        if (!hasNumber) {
            setError("Password must contain at least one number (0-9)");
            return;
        }
        if (!hasSpecial) {
            setError("Password must contain at least one special character (!@#$%^&*...)");
            return;
        }
        if (password !== confirmPassword) {
            setError("Passwords do not match");
            return;
        }

        if (!departmentId) {
            setError("Please select a department");
            return;
        }

        setLoading(true);

        try {
            const response = await signup({
                name,
                email,
                password,
                confirmPassword,
                departmentId: Number(departmentId)
            });

            setSuccess(
                `Account created successfully! Your employee code is ${response.employeeCode}. Redirecting to login...`
            );

            setName("");
            setEmail("");
            setPassword("");
            setConfirmPassword("");

            setTimeout(() => {
                router.replace("/login");
            }, 2500);
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : "Signup failed"
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="auth-wrapper">
            <div style={{ position: "absolute", top: "24px", right: "24px", zIndex: 50 }}>
                <ThemeToggle />
            </div>

            <div className="auth-card" style={{ maxWidth: "480px" }}>
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
                            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                            <circle cx="8.5" cy="7" r="4" />
                            <line x1="20" y1="8" x2="20" y2="14" />
                            <line x1="23" y1="11" x2="17" y2="11" />
                        </svg>
                    </div>
                    <h1 className="auth-brand">Create Account</h1>
                    <p className="auth-sub">Join ASCEND Performance Management System</p>
                </div>

                {error && (
                    <div className="alert alert-error">
                        <span className="alert-icon">!</span>
                        <span>{error}</span>
                    </div>
                )}

                {success && (
                    <div className="alert alert-success">
                        <span className="alert-icon">✓</span>
                        <span>{success}</span>
                    </div>
                )}

                <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    <div className="form-group">
                        <label className="form-label">Full Name</label>
                        <div style={{ position: "relative" }}>
                            <input
                                type="text"
                                className="form-input"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Sarah Jenkins"
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
                        <label className="form-label">Work Email</label>
                        <div style={{ position: "relative" }}>
                            <input
                                type="email"
                                className="form-input"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="sarah.jenkins@company.com"
                                style={{
                                    paddingLeft: "42px",
                                    borderColor: email.length > 0 && !isEmailValid ? "#ef4444" : undefined
                                }}
                                required
                            />
                            <div style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                                    <polyline points="22,6 12,13 2,6" />
                                </svg>
                            </div>
                        </div>
                        {email.length > 0 && !isEmailValid && (
                            <span style={{ color: "#ef4444", fontSize: "0.74rem", marginTop: "4px", display: "block" }}>
                                Please enter a valid email address (e.g. name@company.com)
                            </span>
                        )}
                    </div>

                    <div className="form-group">
                        <label className="form-label">Department</label>
                        <div style={{ position: "relative" }}>
                            {departments.length > 0 ? (
                                <select
                                    className="form-select"
                                    value={departmentId}
                                    onChange={(e) => setDepartmentId(e.target.value)}
                                    style={{ paddingLeft: "42px" }}
                                    required
                                >
                                    <option value="">Select your department...</option>
                                    {departments.map((dept) => (
                                        <option key={dept.id} value={dept.id}>
                                            {dept.name}
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <input
                                    type="number"
                                    className="form-input"
                                    value={departmentId}
                                    onChange={(e) => setDepartmentId(e.target.value)}
                                    placeholder="Department ID (e.g. 10)"
                                    min="1"
                                    style={{ paddingLeft: "42px" }}
                                    required
                                />
                            )}
                            <div style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M3 21h18" />
                                    <path d="M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16" />
                                    <path d="M9 9h1" />
                                    <path d="M9 13h1" />
                                    <path d="M9 17h1" />
                                    <path d="M14 9h1" />
                                    <path d="M14 13h1" />
                                    <path d="M14 17h1" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                        <div className="form-group">
                            <label className="form-label">Password</label>
                            <div style={{ position: "relative" }}>
                                <input
                                    type={showPassword ? "text" : "password"}
                                    className="form-input"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="••••••••"
                                    style={{ paddingRight: "38px" }}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    style={{ position: "absolute", right: "12px", top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "var(--text-muted)", cursor: "pointer" }}
                                >
                                    {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                                </button>
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Confirm</label>
                            <div style={{ position: "relative" }}>
                                <input
                                    type={showConfirmPassword ? "text" : "password"}
                                    className="form-input"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    placeholder="••••••••"
                                    style={{ paddingRight: "38px" }}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    style={{ position: "absolute", right: "12px", top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "var(--text-muted)", cursor: "pointer" }}
                                >
                                    {showConfirmPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Live Password Policy Checklist */}
                    <div style={{
                        background: "var(--bg-subtle, rgba(255,255,255,0.03))",
                        padding: "10px 14px",
                        borderRadius: "10px",
                        border: "1px solid var(--border, #374151)",
                        fontSize: "0.74rem",
                        display: "flex",
                        flexDirection: "column",
                        gap: "4px",
                        marginBottom: "6px"
                    }}>
                        <span style={{ fontWeight: "700", color: "var(--text-muted, #9ca3af)" }}>Password Requirements:</span>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4px" }}>
                            <span style={{ color: hasMinLength ? "#10b981" : "var(--text-muted, #9ca3af)", display: "flex", alignItems: "center", gap: "4px" }}>
                                {hasMinLength ? "✓" : "○"} 12+ characters
                            </span>
                            <span style={{ color: hasUppercase ? "#10b981" : "var(--text-muted, #9ca3af)", display: "flex", alignItems: "center", gap: "4px" }}>
                                {hasUppercase ? "✓" : "○"} Uppercase (A-Z)
                            </span>
                            <span style={{ color: hasLowercase ? "#10b981" : "var(--text-muted, #9ca3af)", display: "flex", alignItems: "center", gap: "4px" }}>
                                {hasLowercase ? "✓" : "○"} Lowercase (a-z)
                            </span>
                            <span style={{ color: hasNumber ? "#10b981" : "var(--text-muted, #9ca3af)", display: "flex", alignItems: "center", gap: "4px" }}>
                                {hasNumber ? "✓" : "○"} Number (0-9)
                            </span>
                            <span style={{ color: hasSpecial ? "#10b981" : "var(--text-muted, #9ca3af)", display: "flex", alignItems: "center", gap: "4px" }}>
                                {hasSpecial ? "✓" : "○"} Special char (!@#$...)
                            </span>
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: "100%", padding: "13px", marginTop: "6px", fontSize: "0.95rem", fontWeight: "700" }}
                        disabled={loading || !name.trim() || !isEmailValid || !password || !confirmPassword || !isPasswordValid || password !== confirmPassword}
                    >
                        {loading ? "Creating Account..." : "Register Employee Account"}
                    </button>

                    <div style={{ display: "flex", alignItems: "center", margin: "10px 0", gap: "10px" }}>
                        <div style={{ flex: 1, height: "1px", background: "var(--border, #374151)" }} />
                        <span style={{ fontSize: "0.75rem", color: "var(--text-muted, #9ca3af)", textTransform: "uppercase", letterSpacing: "0.05em" }}>OR</span>
                        <div style={{ flex: 1, height: "1px", background: "var(--border, #374151)" }} />
                    </div>

                    <a
                        href="http://localhost:8080/oauth2/authorization/google"
                        style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            gap: "10px",
                            padding: "11px 16px",
                            borderRadius: "10px",
                            border: "1px solid var(--border, #374151)",
                            background: "var(--bg-secondary, #1f2937)",
                            color: "var(--text-primary, #ffffff)",
                            fontSize: "0.9rem",
                            fontWeight: "600",
                            textDecoration: "none",
                            transition: "all 0.2s ease"
                        }}
                    >
                        <svg width="18" height="18" viewBox="0 0 24 24">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                        </svg>
                        Sign up with Google
                    </a>

                    <div style={{ textAlign: "center", marginTop: "8px", fontSize: "0.875rem", color: "var(--text-muted)" }}>
                        Already have an account?{" "}
                        <Link href="/login" style={{ color: "var(--primary)", fontWeight: "600", textDecoration: "none" }}>
                            Sign in here →
                        </Link>
                    </div>
                </form>
            </div>
        </main>
    );
}