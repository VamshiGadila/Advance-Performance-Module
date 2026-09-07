"use client";

import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import { saveUser, AuthUser } from "@/lib/auth";
import { Shield, Lock, Mail, ArrowRight } from "lucide-react";

export default function LoginPage() {
    const router = useRouter();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Listen for Google OAuth callback parameters
    useEffect(() => {
        if (typeof window === "undefined") return;
        const params = new URLSearchParams(window.location.search);

        const oauthSuccess = params.get("oauth_success");
        if (oauthSuccess === "true") {
            const token = params.get("token") || "";
            const role = (params.get("role") || "USER") as "ADMIN" | "USER";
            const userEmail = params.get("email") || "";
            const name = params.get("name") || "Google User";

            saveUser({
                token,
                role,
                email: userEmail,
                name
            });

            router.replace("/policies");
            return;
        }

        const oauthError = params.get("oauth_error");
        if (oauthError) {
            setError(oauthError);
        }

        const expired = params.get("expired");
        if (expired === "true") {
            setError("Your session has expired. Please sign in again.");
        }
    }, [router]);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const res = await api<{ token: string; role: "ADMIN" | "USER"; email: string; message: string }>("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({ email: email.trim(), password })
            });

            saveUser({
                token: res.token,
                role: res.role,
                email: res.email
            });

            router.replace("/policies");
        } catch (err: any) {
            setError(err.message || "Invalid email or password");
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="auth-wrapper">
            <div className="auth-card">
                <div style={{ textAlign: "center", marginBottom: "24px" }}>
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
                    <h1 style={{ fontSize: "1.4rem", fontWeight: "800", color: "#f8fafc" }}>HRMS Policies</h1>
                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", marginTop: "4px" }}>
                        Corporate Policy & Compliance Workspace
                    </p>
                </div>

                {error && (
                    <div className="alert-error">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Work Email</label>
                        <input
                            type="email"
                            className="form-input"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="admin@gmail.com or user@company.com"
                            required
                        />
                    </div>

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

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: "100%", marginTop: "8px" }}
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Sign In to Policies"}
                    </button>
                </form>

                <div style={{ display: "flex", alignItems: "center", margin: "20px 0", gap: "10px" }}>
                    <div style={{ flex: 1, height: "1px", background: "var(--border)" }} />
                    <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", textTransform: "uppercase" }}>OR</span>
                    <div style={{ flex: 1, height: "1px", background: "var(--border)" }} />
                </div>

                {/* GOOGLE SIGN IN BUTTON */}
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

                <div style={{ textAlign: "center", marginTop: "18px", fontSize: "0.85rem", color: "var(--text-muted)" }}>
                    New staff member?{" "}
                    <Link href="/signup" style={{ color: "#34d399", fontWeight: "600" }}>
                        Create account →
                    </Link>
                </div>
            </div>
        </main>
    );
}
