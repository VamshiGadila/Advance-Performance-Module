"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
    getTeam,
    TeamMember,
    getActiveCycle,
    getModificationRequests,
    ModificationRequest,
    approveModificationRequest,
    rejectModificationRequest
} from "@/services/managerService";
import { Cycle } from "@/services/hrService";
import Pagination from "@/components/Pagination";

export default function ManagerDashboard() {
    const [team, setTeam] = useState<TeamMember[]>([]);
    const [activeCycle, setActiveCycle] = useState<Cycle | null>(null);
    const [modRequests, setModRequests] = useState<ModificationRequest[]>([]);

    // Search and Pagination
    const [searchTerm, setSearchTerm] = useState("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(6);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    // Review Modal State
    const [reviewingMod, setReviewingMod] = useState<ModificationRequest | null>(null);
    const [reviewAction, setReviewAction] = useState<"APPROVE" | "REJECT">("APPROVE");
    const [reviewComment, setReviewComment] = useState("");
    const [submittingReview, setSubmittingReview] = useState(false);

    const loadData = () => {
        setLoading(true);
        setError("");

        Promise.all([
            getTeam().catch(() => [] as TeamMember[]),
            getActiveCycle().catch(() => null),
            getModificationRequests("PENDING").catch(() => [] as ModificationRequest[])
        ])
            .then(([teamData, cycleData, modData]) => {
                setTeam(teamData);
                setActiveCycle(cycleData);
                setModRequests(modData);
            })
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load team data"))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleReviewSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!reviewingMod) return;

        try {
            setSubmittingReview(true);
            setError("");

            if (reviewAction === "APPROVE") {
                await approveModificationRequest(reviewingMod.id, reviewComment);
                setSuccessMessage("Modification request approved successfully!");
            } else {
                await rejectModificationRequest(reviewingMod.id, reviewComment);
                setSuccessMessage("Modification request rejected.");
            }

            setTimeout(() => setSuccessMessage(""), 4000);
            setReviewingMod(null);
            setReviewComment("");
            loadData();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to process review");
        } finally {
            setSubmittingReview(false);
        }
    };

    const getInitials = (name: string) => {
        return name
            .split(" ")
            .map((n) => n[0])
            .join("")
            .substring(0, 2)
            .toUpperCase();
    };

    const filteredTeam = team.filter((m) => {
        const term = searchTerm.trim().toLowerCase();
        return (
            !term ||
            (m.name && m.name.toLowerCase().includes(term)) ||
            (m.employeeCode && m.employeeCode.toLowerCase().includes(term)) ||
            (m.email && m.email.toLowerCase().includes(term))
        );
    });

    const totalElements = filteredTeam.length;
    const totalPages = Math.ceil(totalElements / size);
    const paginatedTeam = filteredTeam.slice(page * size, (page + 1) * size);

    return (
        <section>
            <div className="page-header">
                <div>
                    <h1 className="page-title">My Team & Performance Management</h1>
                    <p className="page-subtitle">
                        Manage OKRs, KPIs, and review goal modification requests from your direct reports.
                    </p>
                </div>
            </div>

            {error && (
                <div className="alert alert-error">
                    <span className="alert-icon">!</span>
                    <span>{error}</span>
                </div>
            )}

            {successMessage && (
                <div className="alert alert-success">
                    <span className="alert-icon">✓</span>
                    <span>{successMessage}</span>
                </div>
            )}

            {activeCycle ? (
                <div className="cycle-banner">
                    <div className="cycle-banner-content">
                        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                            <span className="cycle-banner-pill">ACTIVE REVIEW CYCLE</span>
                            <span style={{ fontSize: "0.85rem", opacity: 0.9 }}>ID: #{activeCycle.id}</span>
                        </div>
                        <h2>{activeCycle.name}</h2>
                        <p>
                            🗓️ Review Period: <strong>{activeCycle.startDate}</strong> to <strong>{activeCycle.endDate}</strong>
                        </p>
                    </div>
                </div>
            ) : (
                <div className="alert alert-info" style={{ marginBottom: "24px" }}>
                    <span className="alert-icon">i</span>
                    <span>No active performance cycle is currently open. Goals can only be assigned during an active review cycle.</span>
                </div>
            )}

            {/* TEAM PERFORMANCE METRICS */}
            <div className="stats-grid" style={{ marginBottom: "24px" }}>
                <div className="stat-card">
                    <div className="stat-label">Direct Reports</div>
                    <div className="stat-value">{team.length}</div>
                    <div className="stat-desc">Assigned reporting team members</div>
                </div>

                <div className="stat-card emerald">
                    <div className="stat-label">Review Period</div>
                    <div className="stat-value">{activeCycle ? "ACTIVE" : "STANDBY"}</div>
                    <div className="stat-desc">Goal assignment and evaluation cycle</div>
                </div>

                <div className="stat-card amber">
                    <div className="stat-label">Pending Reviews</div>
                    <div className="stat-value">{modRequests.length}</div>
                    <div className="stat-desc">Direct report modification requests</div>
                </div>
            </div>

            {/* PENDING MODIFICATION REQUESTS REVIEW SECTION */}
            {modRequests.length > 0 && (
                <div className="card" style={{ marginBottom: "28px", border: "1px solid rgba(251, 191, 36, 0.3)", background: "rgba(245, 158, 11, 0.08)" }}>
                    <div className="card-header" style={{ borderColor: "rgba(251, 191, 36, 0.2)" }}>
                        <h2 className="card-title" style={{ color: "#fbbf24" }}>
                            ⚠️ Pending Goal Modification Requests ({modRequests.length})
                        </h2>
                    </div>

                    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                        {modRequests.map((mod) => (
                            <div
                                key={mod.id}
                                style={{
                                    background: "var(--bg-surface)",
                                    border: "1px solid var(--border)",
                                    borderRadius: "var(--radius-md)",
                                    padding: "16px 20px",
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    flexWrap: "wrap",
                                    gap: "14px"
                                }}
                            >
                                <div>
                                    <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "1rem" }}>
                                        {mod.employeeName} — Goal: <em>"{mod.goalTitle}"</em>
                                    </div>
                                    <div style={{ fontSize: "0.875rem", color: "var(--text-secondary)", marginTop: "4px" }}>
                                        💬 Reason: {mod.comment}
                                    </div>
                                    {mod.requestedChanges && (
                                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)", marginTop: "2px" }}>
                                            📝 Proposed: {mod.requestedChanges}
                                        </div>
                                    )}
                                </div>

                                <div style={{ display: "flex", gap: "8px" }}>
                                    <button
                                        type="button"
                                        className="btn btn-primary btn-sm"
                                        onClick={() => {
                                            setReviewingMod(mod);
                                            setReviewAction("APPROVE");
                                            setReviewComment("");
                                        }}
                                    >
                                        ✓ Approve
                                    </button>
                                    <button
                                        type="button"
                                        className="btn btn-danger btn-sm"
                                        onClick={() => {
                                            setReviewingMod(mod);
                                            setReviewAction("REJECT");
                                            setReviewComment("");
                                        }}
                                    >
                                        ✕ Reject
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* DIRECT REPORTS GRID */}
            <div className="card">
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px", flexWrap: "wrap", gap: "12px" }}>
                    <div>
                        <span style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "1.1rem" }}>
                            Direct Reports ({totalElements})
                        </span>
                        <div style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
                            Select a direct report to manage and allocate performance goals
                        </div>
                    </div>

                    <input
                        type="text"
                        className="form-input"
                        placeholder="🔍 Filter direct reports..."
                        value={searchTerm}
                        onChange={(e) => {
                            setSearchTerm(e.target.value);
                            setPage(0);
                        }}
                        style={{ maxWidth: "280px" }}
                    />
                </div>

                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        Loading team members...
                    </div>
                ) : paginatedTeam.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        {team.length === 0
                            ? "No employees are currently assigned to your team by HR."
                            : "No direct reports match your search filter."}
                    </div>
                ) : (
                    <>
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "20px" }}>
                            {paginatedTeam.map((member) => (
                                <div
                                    key={member.id}
                                    className="card"
                                    style={{
                                        border: "1px solid var(--border)",
                                        display: "flex",
                                        flexDirection: "column",
                                        justifyContent: "space-between",
                                        padding: "22px",
                                        boxShadow: "var(--shadow-xs)"
                                    }}
                                >
                                    <div>
                                        <div style={{ display: "flex", alignItems: "center", gap: "14px", marginBottom: "14px" }}>
                                            <div className="user-avatar" style={{ width: "46px", height: "46px", fontSize: "1.1rem" }}>
                                                {getInitials(member.name)}
                                            </div>
                                            <div>
                                                <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "1.05rem" }}>
                                                    {member.name}
                                                </div>
                                                <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "2px" }}>
                                                    <span style={{ fontSize: "0.8rem", color: "var(--primary)", fontFamily: "monospace", fontWeight: "700" }}>
                                                        {member.employeeCode}
                                                    </span>
                                                    <span className="pill pill-active" style={{ fontSize: "0.7rem", padding: "1px 6px" }}>
                                                        [EMPLOYEE]
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <div style={{ display: "flex", gap: "8px", marginBottom: "16px", flexWrap: "wrap" }}>
                                            <span className="pill pill-active">Direct Report</span>
                                            <span className="dept-tag">
                                                Dept #{member.departmentId ?? "N/A"}
                                            </span>
                                        </div>

                                        <div style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginBottom: "18px" }}>
                                            ✉️ {member.email}
                                        </div>
                                    </div>

                                    <Link
                                        href={`/manager/goals/${member.id}`}
                                        className="btn btn-primary"
                                        style={{ width: "100%", textDecoration: "none" }}
                                    >
                                        <span>🎯</span>
                                        <span>Manage & Assign Goals</span>
                                    </Link>
                                </div>
                            ))}
                        </div>

                        {/* PAGINATION */}
                        <Pagination
                            page={page}
                            size={size}
                            totalPages={totalPages}
                            totalElements={totalElements}
                            sizeOptions={[3, 6, 12]}
                            onPageChange={setPage}
                            onSizeChange={(newSize) => {
                                setSize(newSize);
                                setPage(0);
                            }}
                        />
                    </>
                )}
            </div>

            {/* REVIEW MODAL */}
            {reviewingMod && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "500px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                <div style={{
                                    width: "42px",
                                    height: "42px",
                                    borderRadius: "12px",
                                    background: reviewAction === "APPROVE" ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)",
                                    color: reviewAction === "APPROVE" ? "#10b981" : "#ef4444",
                                    display: "grid",
                                    placeItems: "center",
                                    fontSize: "1.2rem",
                                    fontWeight: "800"
                                }}>
                                    {reviewAction === "APPROVE" ? "✓" : "✕"}
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                        {reviewAction === "APPROVE" ? "Approve Goal Modification" : "Reject Modification Request"}
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                        Employee alignment review
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setReviewingMod(null)}
                                className="btn-close"
                                title="Close modal"
                            >
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleReviewSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div className="card" style={{ padding: "14px", background: "var(--bg-subtle)" }}>
                                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "6px" }}>
                                    <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: "600" }}>Employee:</span>
                                    <strong style={{ color: "var(--text-main)", fontSize: "0.875rem" }}>{reviewingMod.employeeName}</strong>
                                </div>
                                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                                    <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: "600" }}>Goal:</span>
                                    <strong style={{ color: "var(--primary)", fontSize: "0.875rem" }}>{reviewingMod.goalTitle}</strong>
                                </div>
                                <div style={{ fontSize: "0.825rem", color: "var(--text-secondary)", borderTop: "1px solid var(--border)", paddingTop: "8px", marginTop: "4px" }}>
                                    <strong>Request Note:</strong> {reviewingMod.comment}
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Manager Feedback / Comments</label>
                                <textarea
                                    className="form-textarea"
                                    value={reviewComment}
                                    onChange={(e) => setReviewComment(e.target.value)}
                                    placeholder={reviewAction === "APPROVE" ? "e.g. Approved 2-week extension" : "e.g. Cannot extend due to project release deadline"}
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setReviewingMod(null)}>Cancel</button>
                                <button
                                    type="submit"
                                    className={`btn ${reviewAction === "APPROVE" ? "btn-primary" : "btn-danger"}`}
                                    disabled={submittingReview}
                                >
                                    {submittingReview ? "Processing..." : reviewAction === "APPROVE" ? "Confirm Approval ✓" : "Confirm Rejection ✕"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </section>
    );
}