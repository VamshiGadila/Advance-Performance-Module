"use client";

import { useEffect, useState } from "react";
import {
    getMyGoals,
    getMyManager,
    EmployeeGoal,
    MyManagerInfo,
    acceptGoal,
    updateProgress,
    requestModification
} from "@/services/employeeService";
import { getActiveCycle } from "@/services/managerService";
import { Cycle } from "@/services/hrService";
import Pagination from "@/components/Pagination";

export default function EmployeePage() {
    const [allGoals, setAllGoals] = useState<EmployeeGoal[]>([]);
    const [activeCycle, setActiveCycle] = useState<Cycle | null>(null);
    const [manager, setManager] = useState<MyManagerInfo | null>(null);

    // Search, Filter, and Pagination
    const [searchTerm, setSearchTerm] = useState("");
    const [typeFilter, setTypeFilter] = useState<string>("");
    const [statusFilter, setStatusFilter] = useState<string>("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(5);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    // Modal state for progress update
    const [progressModalGoal, setProgressModalGoal] = useState<EmployeeGoal | null>(null);
    const [newProgress, setNewProgress] = useState(0);
    const [progressComment, setProgressComment] = useState("");
    const [submittingProgress, setSubmittingProgress] = useState(false);

    // Modal state for modification request
    const [modModalGoal, setModModalGoal] = useState<EmployeeGoal | null>(null);
    const [modReason, setModReason] = useState("");
    const [modChanges, setModChanges] = useState("");
    const [submittingMod, setSubmittingMod] = useState(false);

    const loadData = () => {
        setLoading(true);
        setError("");

        Promise.all([
            getMyGoals().catch(() => [] as EmployeeGoal[]),
            getActiveCycle().catch(() => null),
            getMyManager().catch(() => null)
        ])
            .then(([goalsData, cycleData, managerData]) => {
                setAllGoals(goalsData);
                setActiveCycle(cycleData);
                setManager(managerData);
            })
            .catch((err) => {
                setError(err instanceof Error ? err.message : "Unable to load employee dashboard");
            })
            .finally(() => {
                setLoading(false);
            });
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleAccept = async (goalId: number) => {
        try {
            setError("");
            await acceptGoal(goalId);
            setSuccessMessage("Goal accepted successfully!");
            setTimeout(() => setSuccessMessage(""), 4000);
            loadData();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to accept goal");
        }
    };

    const handleProgressSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!progressModalGoal) return;

        try {
            setSubmittingProgress(true);
            setError("");
            await updateProgress(progressModalGoal.id, newProgress, progressComment);
            setSuccessMessage(`Goal progress updated to ${newProgress}%!`);
            setTimeout(() => setSuccessMessage(""), 4000);
            setProgressModalGoal(null);
            setProgressComment("");
            loadData();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to update progress");
        } finally {
            setSubmittingProgress(false);
        }
    };

    const handleModSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!modModalGoal) return;

        try {
            setSubmittingMod(true);
            setError("");
            await requestModification(modModalGoal.id, modReason, modChanges);
            setSuccessMessage("Goal modification request submitted to your manager!");
            setTimeout(() => setSuccessMessage(""), 4000);
            setModModalGoal(null);
            setModReason("");
            setModChanges("");
            loadData();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to submit modification request");
        } finally {
            setSubmittingMod(false);
        }
    };

    const totalWeight = allGoals.reduce((acc, g) => acc + (Number(g.weight) || 0), 0);
    const roundedTotal = Math.round(totalWeight * 100) / 100;

    // Filter goals
    const filteredGoals = allGoals.filter((g) => {
        const matchesSearch =
            !searchTerm ||
            g.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (g.description && g.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (g.target && g.target.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesType = !typeFilter || g.goalType === typeFilter;
        const matchesStatus = !statusFilter || g.status === statusFilter;

        return matchesSearch && matchesType && matchesStatus;
    });

    const totalElements = filteredGoals.length;
    const totalPages = Math.ceil(totalElements / size);
    const paginatedGoals = filteredGoals.slice(page * size, (page + 1) * size);

    const getStatusPill = (status: string) => {
        switch (status) {
            case "PENDING_ACCEPTANCE":
                return <span className="pill pill-draft">⏳ Pending Acceptance</span>;
            case "ACCEPTED":
                return <span className="pill pill-kpi">✓ Accepted</span>;
            case "IN_PROGRESS":
                return <span className="pill pill-active">⚡ In Progress</span>;
            case "MODIFICATION_REQUESTED":
                return <span className="pill" style={{ background: "rgba(245, 158, 11, 0.12)", color: "#fbbf24", border: "1px solid rgba(251, 191, 36, 0.3)" }}>📝 Modification Requested</span>;
            case "COMPLETED":
                return <span className="pill pill-completed">🎉 Completed</span>;
            default:
                return <span className="pill pill-draft">{status}</span>;
        }
    };

    return (
        <section>
            <div className="page-header">
                <div>
                    <h1 className="page-title">My Performance Goals</h1>
                    <p className="page-subtitle">
                        View objectives and key performance indicators assigned to you for the active review cycle.
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

            {/* ACTIVE CYCLE BANNER */}
            {activeCycle ? (
                <div className="cycle-banner">
                    <div className="cycle-banner-content">
                        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                            <span className="cycle-banner-pill">ACTIVE CYCLE</span>
                            <span style={{ fontSize: "0.85rem", opacity: 0.9 }}>ID: #{activeCycle.id}</span>
                        </div>
                        <h2>{activeCycle.name}</h2>
                        <p>
                            🗓️ Timeline: <strong>{activeCycle.startDate}</strong> to <strong>{activeCycle.endDate}</strong>
                        </p>
                    </div>
                </div>
            ) : (
                <div className="alert alert-info">
                    <span className="alert-icon">i</span>
                    <span>No active performance cycle is currently open. Once HR publishes an active cycle, your goals will appear here.</span>
                </div>
            )}

            {manager ? (
                <div
                    className="card"
                    style={{
                        marginBottom: "24px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        flexWrap: "wrap",
                        gap: "14px",
                        background: "var(--bg-surface)",
                        backdropFilter: "blur(12px)",
                        border: "1px solid var(--border)"
                    }}
                >
                    <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                        <div className="user-avatar" style={{ width: "46px", height: "46px", fontSize: "1.15rem" }}>
                            {manager.managerName[0] || "M"}
                        </div>
                        <div>
                            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.05em" }}>
                                Direct Reporting Manager
                            </div>
                            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "2px" }}>
                                <span style={{ fontSize: "1.1rem", fontWeight: "800", color: "var(--text-main)" }}>
                                    {manager.managerName}
                                </span>
                                <span style={{ fontSize: "0.825rem", color: "var(--primary)", fontFamily: "monospace", fontWeight: "700" }}>
                                    {manager.managerCode}
                                </span>
                                <span className="pill pill-okr" style={{ fontSize: "0.7rem", padding: "1px 6px" }}>
                                    [MANAGER]
                                </span>
                            </div>
                        </div>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        <span className="pill pill-active">👤 Assigned Manager</span>
                        {manager.cycleName && (
                            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                                Cycle: {manager.cycleName}
                            </span>
                        )}
                    </div>
                </div>
            ) : (
                <div className="alert alert-info" style={{ marginBottom: "24px" }}>
                    <span className="alert-icon">i</span>
                    <span>
                        You have not been assigned to a reporting manager by HR yet. Once HR assigns your manager, they will balance and assign your performance goals.
                    </span>
                </div>
            )}

            {/* PERSONAL PERFORMANCE STATS */}
            <div className="stats-grid" style={{ marginBottom: "24px" }}>
                <div className="stat-card">
                    <div className="stat-label">Assigned Objectives</div>
                    <div className="stat-value">{allGoals.length}</div>
                    <div className="stat-desc">OKRs and KPIs in active cycle</div>
                </div>

                <div className="stat-card amber">
                    <div className="stat-label">Allocated Weight Budget</div>
                    <div className="stat-value">{roundedTotal}%</div>
                    <div className="stat-desc">Of maximum 100.00% target</div>
                </div>

                <div className="stat-card emerald">
                    <div className="stat-label">Action Items</div>
                    <div className="stat-value">{allGoals.filter((g) => g.status === "PENDING_ACCEPTANCE").length}</div>
                    <div className="stat-desc">Goals awaiting your acceptance</div>
                </div>
            </div>

            {allGoals.length > 0 && (
                <div className="weight-meter">
                    <div className="weight-meter-header">
                        <div>
                            <span>Total Allocated Goal Weight: </span>
                            <strong style={{ color: "var(--primary)" }}>{roundedTotal}%</strong>
                            <span style={{ color: "var(--text-muted)" }}> / 100.00%</span>
                        </div>
                        <div>
                            <span className="pill pill-active">{allGoals.length} Goals Total</span>
                        </div>
                    </div>
                    <div className="progress-bar-container">
                        <div
                            className={`progress-bar-fill ${roundedTotal === 100 ? "complete" : "normal"}`}
                            style={{ width: `${Math.min(100, roundedTotal)}%` }}
                        />
                    </div>
                </div>
            )}

            <div className="card">
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px", flexWrap: "wrap", gap: "12px" }}>
                    <h2 className="card-title" style={{ margin: 0 }}>🎯 Assigned Objectives & KPIs</h2>
                </div>

                {/* SEARCH & FILTER BAR */}
                <div style={{ display: "flex", gap: "12px", marginBottom: "20px", flexWrap: "wrap", alignItems: "center" }}>
                    <div style={{ flex: "1 1 200px" }}>
                        <input
                            type="text"
                            className="form-input"
                            placeholder="🔍 Search my goals..."
                            value={searchTerm}
                            onChange={(e) => {
                                setSearchTerm(e.target.value);
                                setPage(0);
                            }}
                        />
                    </div>

                    <select
                        className="form-select"
                        value={typeFilter}
                        onChange={(e) => {
                            setTypeFilter(e.target.value);
                            setPage(0);
                        }}
                        style={{ minWidth: "140px" }}
                    >
                        <option value="">All Types</option>
                        <option value="OKR">OKR Only</option>
                        <option value="KPI">KPI Only</option>
                    </select>

                    <select
                        className="form-select"
                        value={statusFilter}
                        onChange={(e) => {
                            setStatusFilter(e.target.value);
                            setPage(0);
                        }}
                        style={{ minWidth: "180px" }}
                    >
                        <option value="">All Statuses</option>
                        <option value="PENDING_ACCEPTANCE">Pending Acceptance</option>
                        <option value="ACCEPTED">Accepted</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="MODIFICATION_REQUESTED">Modification Requested</option>
                        <option value="COMPLETED">Completed</option>
                    </select>

                    {(searchTerm || typeFilter || statusFilter) && (
                        <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => {
                                setSearchTerm("");
                                setTypeFilter("");
                                setStatusFilter("");
                                setPage(0);
                            }}
                        >
                            ✕ Clear
                        </button>
                    )}
                </div>

                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        Loading your goals...
                    </div>
                ) : paginatedGoals.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        {allGoals.length === 0
                            ? "No goals have been assigned to you for the active performance cycle yet."
                            : "No goals match your search and filter criteria."}
                    </div>
                ) : (
                    <>
                        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            {paginatedGoals.map((g) => (
                                <div
                                    key={g.id}
                                    style={{
                                        border: "1px solid var(--border)",
                                        borderRadius: "var(--radius-md)",
                                        padding: "20px",
                                        background: "var(--bg-surface)",
                                        backdropFilter: "blur(8px)"
                                    }}
                                >
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px", marginBottom: "10px" }}>
                                        <div>
                                            <div style={{ display: "flex", gap: "8px", alignItems: "center", marginBottom: "6px" }}>
                                                <span className={`pill ${g.goalType === "OKR" ? "pill-okr" : "pill-kpi"}`}>
                                                    {g.goalType}
                                                </span>
                                                {getStatusPill(g.status)}
                                            </div>
                                            <div style={{ fontWeight: "800", fontSize: "1.15rem", color: "var(--text-main)" }}>
                                                {g.title}
                                            </div>
                                        </div>

                                        <div style={{ textAlign: "right", minWidth: "90px" }}>
                                            <div style={{ fontSize: "1.35rem", fontWeight: "800", color: "var(--primary)" }}>
                                                {g.weight}%
                                            </div>
                                            <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Weight</div>
                                        </div>
                                    </div>

                                    {g.description && (
                                        <p style={{ fontSize: "0.925rem", color: "var(--text-secondary)", marginBottom: "12px", lineHeight: "1.5" }}>
                                            {g.description}
                                        </p>
                                    )}

                                    {g.target && (
                                        <div style={{ fontSize: "0.875rem", background: "var(--bg-subtle)", padding: "10px 14px", borderRadius: "6px", marginBottom: "12px" }}>
                                            <strong>Target Criteria / Key Metric:</strong> {g.target}
                                        </div>
                                    )}

                                    {/* PROGRESS BAR */}
                                    <div style={{ marginBottom: "14px" }}>
                                        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.85rem", fontWeight: "600", marginBottom: "4px" }}>
                                            <span>Progress: {g.progress ?? 0}%</span>
                                            <span style={{ color: "var(--text-muted)" }}>{g.progress === 100 ? "🎉 Completed" : "Ongoing"}</span>
                                        </div>
                                        <div className="progress-bar-container">
                                            <div
                                                className={`progress-bar-fill ${g.progress === 100 ? "complete" : "normal"}`}
                                                style={{ width: `${g.progress ?? 0}%` }}
                                            />
                                        </div>
                                    </div>

                                    {g.employeeComment && (
                                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)", fontStyle: "italic", marginBottom: "12px" }}>
                                            💬 Latest comment: "{g.employeeComment}"
                                        </div>
                                    )}

                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: "12px", borderTop: "1px solid var(--border)", flexWrap: "wrap", gap: "10px" }}>
                                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>
                                            📅 Due Date: <strong>{g.dueDate || "Not specified"}</strong>
                                        </div>

                                        <div style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
                                            {g.status === "PENDING_ACCEPTANCE" && (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="btn btn-primary btn-sm"
                                                        onClick={() => handleAccept(g.id)}
                                                    >
                                                        ✓ Accept Goal
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="btn btn-secondary btn-sm"
                                                        onClick={() => setModModalGoal(g)}
                                                    >
                                                        📝 Request Changes
                                                    </button>
                                                </>
                                            )}

                                            {(g.status === "ACCEPTED" || g.status === "IN_PROGRESS") && (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="btn btn-primary btn-sm"
                                                        onClick={() => {
                                                            setProgressModalGoal(g);
                                                            setNewProgress(g.progress ?? 0);
                                                        }}
                                                    >
                                                        📈 Update Progress
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="btn btn-secondary btn-sm"
                                                        onClick={() => setModModalGoal(g)}
                                                    >
                                                        📝 Request Changes
                                                    </button>
                                                </>
                                            )}

                                            {g.status === "MODIFICATION_REQUESTED" && (
                                                <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.825rem", color: "var(--warning-text)", background: "var(--warning-light)", padding: "6px 12px", borderRadius: "var(--radius-sm)", border: "1px solid var(--warning-border)" }}>
                                                    <span>⏳</span>
                                                    <span>Modification request pending manager review</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* PAGINATION */}
                        <Pagination
                            page={page}
                            size={size}
                            totalPages={totalPages}
                            totalElements={totalElements}
                            sizeOptions={[5, 10, 20]}
                            onPageChange={setPage}
                            onSizeChange={(newSize) => {
                                setSize(newSize);
                                setPage(0);
                            }}
                        />
                    </>
                )}
            </div>

            {/* PROGRESS UPDATE MODAL */}
            {progressModalGoal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                <div style={{
                                    width: "42px",
                                    height: "42px",
                                    borderRadius: "12px",
                                    background: "rgba(99, 102, 241, 0.15)",
                                    color: "#6366f1",
                                    display: "grid",
                                    placeItems: "center",
                                    fontSize: "1.25rem"
                                }}>
                                    📈
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                        Update Goal Progress
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                        Track milestones and completion
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setProgressModalGoal(null)}
                                className="btn-close"
                                title="Close modal"
                            >
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleProgressSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div className="card" style={{ padding: "14px", background: "var(--bg-subtle)" }}>
                                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase", marginBottom: "4px" }}>Objective</div>
                                <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "0.95rem" }}>
                                    {progressModalGoal.title}
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Completion Percentage ({newProgress}%)</label>
                                <input
                                    type="range"
                                    min="0"
                                    max="100"
                                    step="5"
                                    value={newProgress}
                                    onChange={(e) => setNewProgress(Number(e.target.value))}
                                    style={{ width: "100%", accentColor: "var(--primary)" }}
                                />
                                <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px" }}>
                                    <span>0% (Not Started)</span>
                                    <span>50% (Halfway)</span>
                                    <span>100% (Completed)</span>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Progress Notes / Commentary</label>
                                <textarea
                                    className="form-textarea"
                                    value={progressComment}
                                    onChange={(e) => setProgressComment(e.target.value)}
                                    placeholder="Describe milestones achieved, benchmarks hit, or current status..."
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setProgressModalGoal(null)}>Cancel</button>
                                <button type="submit" className="btn btn-primary" disabled={submittingProgress}>
                                    {submittingProgress ? "Saving..." : "Save Progress"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* MODIFICATION REQUEST MODAL */}
            {modModalGoal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "520px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                <div style={{
                                    width: "42px",
                                    height: "42px",
                                    borderRadius: "12px",
                                    background: "rgba(245, 158, 11, 0.15)",
                                    color: "#f59e0b",
                                    display: "grid",
                                    placeItems: "center",
                                    fontSize: "1.25rem"
                                }}>
                                    📝
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                        Request Goal Modification
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                        Submit proposed adjustments to your manager
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setModModalGoal(null)}
                                className="btn-close"
                                title="Close modal"
                            >
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleModSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div className="card" style={{ padding: "14px", background: "var(--bg-subtle)" }}>
                                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase", marginBottom: "4px" }}>Objective</div>
                                <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "0.95rem" }}>
                                    {modModalGoal.title}
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Reason for Request</label>
                                <textarea
                                    className="form-textarea"
                                    value={modReason}
                                    onChange={(e) => setModReason(e.target.value)}
                                    placeholder="Explain why you are requesting changes (e.g. scope change, blocked dependencies, timeline adjustment)..."
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Proposed Changes / New Due Date</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={modChanges}
                                    onChange={(e) => setModChanges(e.target.value)}
                                    placeholder="e.g. Requesting 2-week extension to Dec 15"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setModModalGoal(null)}>Cancel</button>
                                <button type="submit" className="btn btn-primary" disabled={submittingMod}>
                                    {submittingMod ? "Submitting..." : "Submit to Manager"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </section>
    );
}