"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { AuthUser, getUser, logout } from "@/lib/auth";
import {
    Shield,
    Plus,
    Search,
    Trash2,
    Edit3,
    FileText,
    LogOut,
    CheckCircle,
    Clock,
    Archive,
    X,
    ExternalLink
} from "lucide-react";

interface Policy {
    id: number;
    name: string;
    code: string;
    category: string;
    content: string;
    applicability: string;
    mandatory: boolean;
    status: "DRAFT" | "ACTIVE" | "ARCHIVED";
    createdAt?: string;
    updatedAt?: string;
}

export default function PoliciesPage() {
    const router = useRouter();
    const [user, setUser] = useState<AuthUser | null>(null);
    const [policies, setPolicies] = useState<Policy[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    // Filters
    const [searchKeyword, setSearchKeyword] = useState("");
    const [selectedCategory, setSelectedCategory] = useState("ALL");
    const [selectedStatus, setSelectedStatus] = useState("ALL");

    // Modal state for Create / Edit (Admin only)
    const [modalOpen, setModalOpen] = useState(false);
    const [editingPolicy, setEditingPolicy] = useState<Policy | null>(null);
    const [formName, setFormName] = useState("");
    const [formCode, setFormCode] = useState("");
    const [formCategory, setFormCategory] = useState("Leave");
    const [formApplicability, setFormApplicability] = useState("ALL");
    const [formMandatory, setFormMandatory] = useState(true);
    const [formStatus, setFormStatus] = useState<"DRAFT" | "ACTIVE" | "ARCHIVED">("ACTIVE");
    const [formContent, setFormContent] = useState("");
    const [saving, setSaving] = useState(false);

    // 9-Dots App Switcher State
    const [appSwitcherOpen, setAppSwitcherOpen] = useState(false);

    useEffect(() => {
        const currentUser = getUser();
        if (!currentUser) {
            router.replace("/login");
            return;
        }
        setUser(currentUser);
        loadPolicies();

        // Cross-App Single Logout Listener: if logged out from Ascend or anywhere, log out immediately
        const checkGlobalSession = () => {
            const hasActiveSession = document.cookie.includes("app_suite_active_session=true");
            if (!hasActiveSession) {
                logout();
                router.replace("/login");
            }
        };

        window.addEventListener("focus", checkGlobalSession);
        window.addEventListener("visibilitychange", checkGlobalSession);
        const interval = setInterval(checkGlobalSession, 1500);

        return () => {
            window.removeEventListener("focus", checkGlobalSession);
            window.removeEventListener("visibilitychange", checkGlobalSession);
            clearInterval(interval);
        };
    }, [router]);

    const loadPolicies = async () => {
        setLoading(true);
        try {
            const data = await api<any>("/api/policies/search?size=50");
            if (data?.content) {
                setPolicies(data.content);
            } else if (Array.isArray(data)) {
                setPolicies(data);
            }
        } catch (err: any) {
            setError(err.message || "Failed to load policies");
        } finally {
            setLoading(false);
        }
    };

    const handleOpenCreate = () => {
        setEditingPolicy(null);
        setFormName("");
        setFormCode("POL-" + Math.floor(100 + Math.random() * 900));
        setFormCategory("Leave");
        setFormApplicability("ALL");
        setFormMandatory(true);
        setFormStatus("ACTIVE");
        setFormContent("");
        setError("");
        setSuccessMsg("");
        setModalOpen(true);
    };

    const handleOpenEdit = (p: Policy) => {
        setEditingPolicy(p);
        setFormName(p.name);
        setFormCode(p.code);
        setFormCategory(p.category);
        setFormApplicability(p.applicability);
        setFormMandatory(p.mandatory);
        setFormStatus(p.status);
        setFormContent(p.content || "");
        setError("");
        setSuccessMsg("");
        setModalOpen(true);
    };

    const handleSavePolicy = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setError("");

        const payload = {
            name: formName.trim(),
            code: formCode.trim(),
            category: formCategory,
            applicability: formApplicability,
            mandatory: formMandatory,
            status: formStatus,
            content: formContent.trim()
        };

        try {
            if (editingPolicy) {
                await api(`/api/policies/${editingPolicy.id}`, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
                setSuccessMsg("Policy updated successfully!");
            } else {
                await api("/api/policies", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                setSuccessMsg("Policy created successfully!");
            }
            setModalOpen(false);
            loadPolicies();
        } catch (err: any) {
            setError(err.message || "Failed to save policy");
        } finally {
            setSaving(false);
        }
    };

    const handleDeletePolicy = async (id: number) => {
        if (!confirm("Are you sure you want to delete this policy?")) return;
        try {
            await api(`/api/policies/${id}`, { method: "DELETE" });
            setSuccessMsg("Policy deleted successfully!");
            loadPolicies();
        } catch (err: any) {
            setError(err.message || "Failed to delete policy");
        }
    };

    const handleLogout = () => {
        logout();
        router.push("/login");
    };

    const filteredPolicies = policies.filter((p) => {
        const matchesKeyword =
            !searchKeyword ||
            p.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
            p.code.toLowerCase().includes(searchKeyword.toLowerCase()) ||
            (p.content && p.content.toLowerCase().includes(searchKeyword.toLowerCase()));

        const matchesCat = selectedCategory === "ALL" || p.category.toLowerCase() === selectedCategory.toLowerCase();
        const matchesStatus = selectedStatus === "ALL" || p.status === selectedStatus;

        return matchesKeyword && matchesCat && matchesStatus;
    });

    if (!user) return null;

    const isAdmin = user.role === "ADMIN";

    return (
        <div style={{ minHeight: "100vh", background: "var(--bg-main)" }}>
            {/* TOP HEADER WITH GOOGLE SUITE APP SWITCHER */}
            <header style={{
                height: "64px",
                borderBottom: "1px solid var(--border)",
                background: "var(--bg-surface)",
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "0 28px"
            }}>
                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                    <div style={{
                        width: "36px",
                        height: "36px",
                        borderRadius: "10px",
                        background: "rgba(16, 185, 129, 0.15)",
                        border: "1px solid rgba(16, 185, 129, 0.3)",
                        display: "grid",
                        placeItems: "center",
                        color: "#10b981"
                    }}>
                        <Shield size={20} />
                    </div>
                    <div>
                        <div style={{ fontWeight: "800", fontSize: "1.05rem", color: "#ffffff", letterSpacing: "0.5px" }}>
                            HRMS POLICIES
                        </div>
                    </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                    {/* User Badge */}
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <span style={{ fontSize: "0.85rem", fontWeight: "600", color: "var(--text-main)" }}>
                            {user.name || user.email}
                        </span>
                        <span style={{
                            fontSize: "0.7rem",
                            fontWeight: "700",
                            padding: "3px 8px",
                            borderRadius: "6px",
                            background: isAdmin ? "rgba(239, 68, 68, 0.15)" : "rgba(16, 185, 129, 0.15)",
                            color: isAdmin ? "#f87171" : "#34d399",
                            border: `1px solid ${isAdmin ? "rgba(239, 68, 68, 0.3)" : "rgba(16, 185, 129, 0.3)"}`
                        }}>
                            [{user.role}]
                        </span>
                    </div>

                    {/* 9-DOTS GOOGLE SUITE APP SWITCHER (CROSS-APP JUMP BACK TO ASCEND) */}
                    <div style={{ position: "relative" }}>
                        <button
                            type="button"
                            onClick={() => setAppSwitcherOpen(!appSwitcherOpen)}
                            className="btn btn-secondary btn-sm"
                            style={{
                                width: "36px",
                                height: "36px",
                                padding: 0,
                                borderRadius: "8px",
                                background: appSwitcherOpen ? "rgba(16, 185, 129, 0.2)" : "rgba(255, 255, 255, 0.05)",
                                borderColor: appSwitcherOpen ? "#10b981" : "var(--border)",
                                color: "var(--text-main)",
                                cursor: "pointer"
                            }}
                            title="Enterprise Suite App Switcher"
                        >
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                                <circle cx="5" cy="5" r="2.5"/><circle cx="12" cy="5" r="2.5"/><circle cx="19" cy="5" r="2.5"/>
                                <circle cx="5" cy="12" r="2.5"/><circle cx="12" cy="12" r="2.5"/><circle cx="19" cy="12" r="2.5"/>
                                <circle cx="5" cy="19" r="2.5"/><circle cx="12" cy="19" r="2.5"/><circle cx="19" cy="19" r="2.5"/>
                            </svg>
                        </button>

                        {appSwitcherOpen && (
                            <>
                                <div
                                    style={{ position: "fixed", inset: 0, zIndex: 998 }}
                                    onClick={() => setAppSwitcherOpen(false)}
                                />
                                <div style={{
                                    position: "absolute",
                                    top: "44px",
                                    right: 0,
                                    width: "300px",
                                    background: "var(--bg-surface)",
                                    border: "1px solid var(--border)",
                                    borderRadius: "14px",
                                    boxShadow: "0 12px 30px rgba(0,0,0,0.45)",
                                    padding: "16px",
                                    zIndex: 999,
                                    display: "flex",
                                    flexDirection: "column",
                                    gap: "12px"
                                }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                        <span style={{ fontSize: "0.75rem", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.5px", color: "var(--text-muted)" }}>
                                            🏢 Enterprise Apps Suite
                                        </span>
                                    </div>

                                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                                        {/* Switch back to Ascend Performance */}
                                        <a
                                            href="http://localhost:3000"
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            onClick={() => setAppSwitcherOpen(false)}
                                            style={{
                                                padding: "12px 10px",
                                                borderRadius: "10px",
                                                background: "rgba(99, 102, 241, 0.15)",
                                                border: "1px solid rgba(99, 102, 241, 0.4)",
                                                textAlign: "center",
                                                display: "flex",
                                                flexDirection: "column",
                                                alignItems: "center",
                                                gap: "4px",
                                                textDecoration: "none",
                                                color: "var(--text-main)"
                                            }}
                                            title="Open Ascend Performance in a new window"
                                        >
                                            <div style={{ fontSize: "1.3rem" }}>⚡</div>
                                            <div style={{ fontWeight: "700", fontSize: "0.85rem" }}>Ascend</div>
                                            <span style={{ fontSize: "0.68rem", color: "#818cf8", fontWeight: "700" }}>Open Suite ↗</span>
                                        </a>

                                        {/* HRMS Policies (Current Active) */}
                                        <div style={{
                                            padding: "12px 10px",
                                            borderRadius: "10px",
                                            background: "rgba(16, 185, 129, 0.15)",
                                            border: "1px solid rgba(16, 185, 129, 0.4)",
                                            textAlign: "center",
                                            display: "flex",
                                            flexDirection: "column",
                                            alignItems: "center",
                                            gap: "4px"
                                        }}>
                                            <div style={{ fontSize: "1.3rem" }}>📜</div>
                                            <div style={{ fontWeight: "700", fontSize: "0.85rem", color: "var(--text-main)" }}>HR Policies</div>
                                            <span style={{ fontSize: "0.68rem", color: "#34d399", fontWeight: "600" }}>Active Module</span>
                                        </div>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>

                    <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={handleLogout}
                        title="Sign out of HRMS Policies"
                    >
                        <LogOut size={15} />
                        <span>Sign out</span>
                    </button>
                </div>
            </header>

            {/* MAIN CONTENT AREA */}
            <main style={{ maxWidth: "1200px", margin: "0 auto", padding: "32px 24px" }}>
                {/* Banner Header */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "28px" }}>
                    <div>
                        <h1 style={{ fontSize: "1.7rem", fontWeight: "800", color: "#ffffff" }}>
                            Organization Policy Catalog
                        </h1>
                    </div>

                    {/* RBAC Action: Only ADMIN sees the "Create Policy" button */}
                    {isAdmin && (
                        <button
                            type="button"
                            onClick={handleOpenCreate}
                            className="btn btn-primary"
                        >
                            <Plus size={18} />
                            <span>Create New Policy</span>
                        </button>
                    )}
                </div>

                {successMsg && <div className="alert-success" style={{ marginBottom: "20px" }}>{successMsg}</div>}
                {error && <div className="alert-error" style={{ marginBottom: "20px" }}>{error}</div>}

                {/* SEARCH & FILTERS BAR */}
                <div style={{
                    display: "grid",
                    gridTemplateColumns: "1fr auto auto",
                    gap: "14px",
                    background: "var(--bg-surface)",
                    padding: "16px",
                    borderRadius: "12px",
                    border: "1px solid var(--border)",
                    marginBottom: "24px"
                }}>
                    <div style={{ position: "relative" }}>
                        <input
                            type="text"
                            className="form-input"
                            value={searchKeyword}
                            onChange={(e) => setSearchKeyword(e.target.value)}
                            placeholder="Search by policy name, code (e.g. WFH-001), or keyword..."
                            style={{ paddingLeft: "38px" }}
                        />
                        <Search size={16} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
                    </div>

                    <select
                        className="form-input"
                        value={selectedCategory}
                        onChange={(e) => setSelectedCategory(e.target.value)}
                        style={{ width: "160px" }}
                    >
                        <option value="ALL">All Categories</option>
                        <option value="Leave">Leave</option>
                        <option value="Conduct">Code of Conduct</option>
                        <option value="Security">Security & IT</option>
                        <option value="WFH">Remote / WFH</option>
                    </select>

                    <select
                        className="form-input"
                        value={selectedStatus}
                        onChange={(e) => setSelectedStatus(e.target.value)}
                        style={{ width: "150px" }}
                    >
                        <option value="ALL">All Statuses</option>
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="DRAFT">DRAFT</option>
                        <option value="ARCHIVED">ARCHIVED</option>
                    </select>
                </div>

                {/* POLICY GRID */}
                {loading ? (
                    <div style={{ textAlign: "center", padding: "60px", color: "var(--text-muted)" }}>
                        Loading enterprise policies...
                    </div>
                ) : filteredPolicies.length === 0 ? (
                    <div style={{
                        textAlign: "center",
                        padding: "60px",
                        background: "var(--bg-surface)",
                        borderRadius: "12px",
                        border: "1px solid var(--border)"
                    }}>
                        <FileText size={40} style={{ color: "var(--text-muted)", margin: "0 auto 12px" }} />
                        <h3 style={{ color: "var(--text-main)", marginBottom: "6px" }}>No Policies Found</h3>
                        <p style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>
                            Try adjusting your search keyword or filters.
                        </p>
                    </div>
                ) : (
                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(360px, 1fr))", gap: "20px" }}>
                        {filteredPolicies.map((p) => (
                            <div
                                key={p.id}
                                style={{
                                    background: "var(--bg-surface)",
                                    border: "1px solid var(--border)",
                                    borderRadius: "14px",
                                    padding: "20px",
                                    display: "flex",
                                    flexDirection: "column",
                                    justifyContent: "space-between"
                                }}
                            >
                                <div>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "10px" }}>
                                        <span style={{
                                            fontFamily: "monospace",
                                            fontSize: "0.8rem",
                                            fontWeight: "700",
                                            color: "#10b981",
                                            background: "rgba(16, 185, 129, 0.1)",
                                            padding: "2px 8px",
                                            borderRadius: "6px"
                                        }}>
                                            {p.code}
                                        </span>

                                        <div style={{ display: "flex", gap: "6px" }}>
                                            {p.mandatory && (
                                                <span style={{ fontSize: "0.7rem", fontWeight: "700", color: "#f59e0b", background: "rgba(245, 158, 11, 0.15)", padding: "2px 6px", borderRadius: "4px" }}>
                                                    MANDATORY
                                                </span>
                                            )}
                                            <span style={{
                                                fontSize: "0.7rem",
                                                fontWeight: "700",
                                                padding: "2px 6px",
                                                borderRadius: "4px",
                                                background: p.status === "ACTIVE" ? "rgba(16, 185, 129, 0.15)" : p.status === "DRAFT" ? "rgba(59, 130, 246, 0.15)" : "rgba(100, 116, 139, 0.2)",
                                                color: p.status === "ACTIVE" ? "#34d399" : p.status === "DRAFT" ? "#60a5fa" : "#94a3b8"
                                            }}>
                                                {p.status}
                                            </span>
                                        </div>
                                    </div>

                                    <h3 style={{ fontSize: "1.1rem", fontWeight: "700", color: "#f8fafc", marginBottom: "8px" }}>
                                        {p.name}
                                    </h3>

                                    <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", lineHeight: "1.5", marginBottom: "16px" }}>
                                        {p.content || "No detailed content provided for this policy."}
                                    </p>
                                </div>

                                <div style={{ borderTop: "1px solid var(--border)", paddingTop: "14px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>
                                        Category: <strong style={{ color: "#ffffff" }}>{p.category}</strong> • Scope: <strong style={{ color: "#ffffff" }}>{p.applicability}</strong>
                                    </div>

                                    {/* RBAC Actions: Edit and Delete only for ADMIN */}
                                    {isAdmin && (
                                        <div style={{ display: "flex", gap: "8px" }}>
                                            <button
                                                type="button"
                                                onClick={() => handleOpenEdit(p)}
                                                className="btn btn-secondary btn-sm"
                                                style={{ padding: "5px 10px", fontSize: "0.75rem" }}
                                                title="Edit Policy"
                                            >
                                                <Edit3 size={14} />
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => handleDeletePolicy(p.id)}
                                                className="btn btn-secondary btn-sm"
                                                style={{ padding: "5px 10px", fontSize: "0.75rem", color: "#f87171" }}
                                                title="Delete Policy"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </main>

            {/* CREATE / EDIT POLICY MODAL (ADMIN ONLY) */}
            {modalOpen && (
                <div style={{
                    position: "fixed",
                    inset: 0,
                    background: "rgba(0,0,0,0.7)",
                    backdropFilter: "blur(4px)",
                    display: "grid",
                    placeItems: "center",
                    padding: "20px",
                    zIndex: 1000
                }}>
                    <div style={{
                        width: "100%",
                        maxWidth: "520px",
                        background: "var(--bg-surface)",
                        border: "1px solid var(--border)",
                        borderRadius: "14px",
                        padding: "24px"
                    }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "18px" }}>
                            <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "#ffffff" }}>
                                {editingPolicy ? "Edit Policy" : "Create New Policy"}
                            </h2>
                            <button
                                type="button"
                                onClick={() => setModalOpen(false)}
                                style={{ background: "none", border: "none", color: "var(--text-muted)", cursor: "pointer" }}
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSavePolicy}>
                            <div className="form-group">
                                <label className="form-label">Policy Name *</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={formName}
                                    onChange={(e) => setFormName(e.target.value)}
                                    placeholder="e.g. Annual Leave Policy"
                                    required
                                />
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-group">
                                    <label className="form-label">Policy Code *</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={formCode}
                                        onChange={(e) => setFormCode(e.target.value)}
                                        placeholder="e.g. LEAVE-001"
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Category *</label>
                                    <select
                                        className="form-input"
                                        value={formCategory}
                                        onChange={(e) => setFormCategory(e.target.value)}
                                    >
                                        <option value="Leave">Leave</option>
                                        <option value="Conduct">Conduct</option>
                                        <option value="Security">Security</option>
                                        <option value="WFH">Remote / WFH</option>
                                    </select>
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-group">
                                    <label className="form-label">Applicability *</label>
                                    <select
                                        className="form-input"
                                        value={formApplicability}
                                        onChange={(e) => setFormApplicability(e.target.value)}
                                    >
                                        <option value="ALL">ALL Employees</option>
                                        <option value="MANAGERS">Managers Only</option>
                                        <option value="INTERNS">Interns Only</option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Status *</label>
                                    <select
                                        className="form-input"
                                        value={formStatus}
                                        onChange={(e) => setFormStatus(e.target.value as any)}
                                    >
                                        <option value="ACTIVE">ACTIVE</option>
                                        <option value="DRAFT">DRAFT</option>
                                        <option value="ARCHIVED">ARCHIVED</option>
                                    </select>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label" style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
                                    <input
                                        type="checkbox"
                                        checked={formMandatory}
                                        onChange={(e) => setFormMandatory(e.target.checked)}
                                    />
                                    <span>Mandatory Policy (All staff must acknowledge)</span>
                                </label>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Policy Content & Description</label>
                                <textarea
                                    className="form-input"
                                    rows={4}
                                    value={formContent}
                                    onChange={(e) => setFormContent(e.target.value)}
                                    placeholder="Describe the rules, guidelines, eligibility, and procedures..."
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "16px" }}>
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setModalOpen(false)}
                                    disabled={saving}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={saving}
                                >
                                    {saving ? "Saving..." : editingPolicy ? "Update Policy" : "Create Policy"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
