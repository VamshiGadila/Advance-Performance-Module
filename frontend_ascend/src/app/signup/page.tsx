"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { signup, getPublicDepartments, PublicDepartment } from "@/services/authService";
import { ThemeToggle } from "@/context/ThemeContext";

export default function Signup() {
    const router = useRouter();

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
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

    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError("");
        setSuccess("");

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
                                style={{ paddingLeft: "42px" }}
                                required
                            />
                            <div style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                                    <polyline points="22,6 12,13 2,6" />
                                </svg>
                            </div>
                        </div>
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
                            <input
                                type="password"
                                className="form-input"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Confirm</label>
                            <input
                                type="password"
                                className="form-input"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: "100%", padding: "13px", marginTop: "6px", fontSize: "0.95rem", fontWeight: "700" }}
                        disabled={loading}
                    >
                        {loading ? "Creating Account..." : "Register Employee Account"}
                    </button>

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