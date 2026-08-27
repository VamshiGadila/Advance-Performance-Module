"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import {
    createGoal,
    updateGoal,
    deleteGoal,
    getEmployeeGoals,
    getTeam,
    getCycles,
    Goal,
    TeamMember
} from "@/services/managerService";
import { Cycle } from "@/services/hrService";
import Pagination from "@/components/Pagination";

export default function ManagerGoalStudio() {
    const params = useParams<{ employeeId: string }>();
    const employeeId = Number(params?.employeeId);

    const [employee, setEmployee] = useState<TeamMember | null>(null);
    const [cycles, setCycles] = useState<Cycle[]>([]);
    const [selectedCycleId, setSelectedCycleId] = useState<number>(0);
    const [allGoals, setAllGoals] = useState<Goal[]>([]);

    // Search, Filter, Pagination
    const [searchTerm, setSearchTerm] = useState("");
    const [typeFilter, setTypeFilter] = useState("");
    const [scopeFilter, setScopeFilter] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(5);

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [isUnassigned, setIsUnassigned] = useState(false);

    // Create Goal Form
    const [createForm, setCreateForm] = useState({
        title: "",
        description: "",
        target: "",
        weight: "",
        goalType: "OKR" as "OKR" | "KPI",
        goalScope: "INDIVIDUAL" as "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY",
        dueDate: ""
    });

    // Edit Goal Modal State
    const [editingGoal, setEditingGoal] = useState<Goal | null>(null);
    const [editForm, setEditForm] = useState({
        title: "",
        description: "",
        target: "",
        weight: "",
        goalType: "OKR" as "OKR" | "KPI",
        goalScope: "INDIVIDUAL" as "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY",
        status: "PENDING_ACCEPTANCE" as Goal["status"],
        dueDate: ""
    });
    const [updating, setUpdating] = useState(false);

    // Delete Goal State
    const [deletingId, setDeletingId] = useState<number | null>(null);

    const loadData = async () => {
        if (!employeeId || isNaN(employeeId)) return;

        setLoading(true);
        setError("");
        setIsUnassigned(false);

        try {
            // 1. Load manager's team members
            const team = await getTeam();
            const currentEmp = team.find((m) => m.id === employeeId) || null;
            setEmployee(currentEmp);

            if (!currentEmp) {
                setIsUnassigned(true);
                setLoading(false);
                return;
            }

            // 2. Load cycles
            const allCycles = await getCycles();
            setCycles(allCycles);

            const active = allCycles.find((c) => c.status === "ACTIVE") || null;
            const currentCycleId = active ? active.id : allCycles[0]?.id || 0;
            setSelectedCycleId(currentCycleId);

            // 3. Load employee goals for current cycle
            if (currentCycleId > 0) {
                const goalList = await getEmployeeGoals(employeeId, currentCycleId);
                setAllGoals(goalList);
            }
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to load employee goal studio");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [employeeId]);

    const handleCycleChange = async (newCycleId: number) => {
        setSelectedCycleId(newCycleId);
        setError("");
        setPage(0);
        try {
            const goalList = await getEmployeeGoals(employeeId, newCycleId);
            setAllGoals(goalList);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to load goals for selected cycle");
        }
    };

    const handleCreateGoal = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        const selectedCycle = cycles.find((c) => c.id === selectedCycleId);

        if (!selectedCycleId || !selectedCycle) {
            setError("Please select a valid performance cycle.");
            return;
        }

        if (selectedCycle.status !== "ACTIVE") {
            setError(`Goals cannot be added to a ${selectedCycle.status} cycle. Please switch to an ACTIVE cycle.`);
            return;
        }

        const weightNum = parseFloat(createForm.weight);
        if (isNaN(weightNum) || weightNum <= 0 || weightNum > 100) {
            setError("Weight must be a valid percentage between 0.01% and 100%.");
            return;
        }

        setSubmitting(true);

        try {
            await createGoal({
                cycleId: selectedCycleId,
                employeeId,
                goalType: createForm.goalType,
                goalScope: createForm.goalScope,
                title: createForm.title.trim(),
                description: createForm.description || undefined,
                target: createForm.target || undefined,
                weight: weightNum,
                dueDate: createForm.dueDate || null
            });

            setSuccess(`Goal "${createForm.title}" allocated successfully!`);
            setCreateForm({
                title: "",
                description: "",
                target: "",
                weight: "",
                goalType: "OKR",
                goalScope: "INDIVIDUAL",
                dueDate: ""
            });

            const refreshedGoals = await getEmployeeGoals(employeeId, selectedCycleId);
            setAllGoals(refreshedGoals);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to create goal");
        } finally {
            setSubmitting(false);
        }
    };

    const openEditModal = (goal: Goal) => {
        setEditingGoal(goal);
        setEditForm({
            title: goal.title,
            description: goal.description || "",
            target: goal.target || "",
            weight: String(goal.weight),
            goalType: goal.goalType,
            goalScope: goal.goalScope,
            status: goal.status,
            dueDate: goal.dueDate || ""
        });
        setError("");
    };

    const handleUpdateGoal = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingGoal) return;

        setError("");
        setUpdating(true);

        try {
            await updateGoal(editingGoal.id, {
                title: editForm.title.trim(),
                description: editForm.description || undefined,
                target: editForm.target || undefined,
                weight: parseFloat(editForm.weight),
                goalType: editForm.goalType,
                goalScope: editForm.goalScope,
                status: editForm.status,
                dueDate: editForm.dueDate || null
            });

            setSuccess(`Goal "${editForm.title}" updated successfully!`);
            setEditingGoal(null);

            const refreshedGoals = await getEmployeeGoals(employeeId, selectedCycleId);
            setAllGoals(refreshedGoals);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to update goal");
        } finally {
            setUpdating(false);
        }
    };

    const handleDeleteGoal = async (goalId: number, goalTitle: string) => {
        if (!confirm(`Are you sure you want to delete goal: "${goalTitle}"?`)) {
            return;
        }

        setError("");
        setDeletingId(goalId);

        try {
            await deleteGoal(goalId);
            setSuccess(`Goal "${goalTitle}" deleted successfully.`);
            const refreshedGoals = await getEmployeeGoals(employeeId, selectedCycleId);
            setAllGoals(refreshedGoals);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to delete goal");
        } finally {
            setDeletingId(null);
        }
    };

    // Calculate total allocated weight
    const totalWeight = allGoals.reduce((acc, g) => acc + (Number(g.weight) || 0), 0);
    const roundedTotal = Math.round(totalWeight * 100) / 100;
    const remainingWeight = Math.max(0, Math.round((100 - roundedTotal) * 100) / 100);

    const getProgressColorClass = () => {
        if (roundedTotal === 100) return "complete";
        if (roundedTotal > 100) return "danger";
        return "normal";
    };

    // Filtering goals
    const filteredGoals = allGoals.filter((g) => {
        const matchesSearch =
            !searchTerm ||
            g.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (g.description && g.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (g.target && g.target.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesType = !typeFilter || g.goalType === typeFilter;
        const matchesScope = !scopeFilter || g.goalScope === scopeFilter;
        const matchesStatus = !statusFilter || g.status === statusFilter;

        return matchesSearch && matchesType && matchesScope && matchesStatus;
    });

    const totalElements = filteredGoals.length;
    const totalPages = Math.ceil(totalElements / size);
    const paginatedGoals = filteredGoals.slice(page * size, (page + 1) * size);

    const selectedCycle = cycles.find((c) => c.id === selectedCycleId);
    const isCycleActive = selectedCycle?.status === "ACTIVE";

    if (isUnassigned) {
        return (
            <section>
                <div style={{ marginBottom: "18px" }}>
                    <Link href="/manager" className="btn btn-secondary btn-sm" style={{ marginBottom: "16px" }}>
                        ← Back to My Team
                    </Link>
                </div>
                <div className="card" style={{ padding: "40px", textAlign: "center" }}>
                    <div style={{ fontSize: "3rem", marginBottom: "12px" }}>🔒</div>
                    <h2 style={{ fontSize: "1.35rem", fontWeight: "800", marginBottom: "8px" }}>Employee Not in Your Team</h2>
                    <p style={{ color: "var(--text-muted)", maxWidth: "480px", margin: "0 auto 20px" }}>
                        Employee #{employeeId} is not assigned to your direct team. Only employees assigned to you by HR can be managed here.
                    </p>
                    <Link href="/manager" className="btn btn-primary">
                        Return to Team Dashboard
                    </Link>
                </div>
            </section>
        );
    }

    return (
        <section>
            {/* BACK LINK & TITLE */}
            <div style={{ marginBottom: "18px" }}>
                <Link
                    href="/manager"
                    style={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: "6px",
                        fontSize: "0.875rem",
                        color: "var(--primary)",
                        fontWeight: "600",
                        marginBottom: "12px"
                    }}
                >
                    ← Back to My Team
                </Link>
                <div className="page-header">
                    <div>
                        <h1 className="page-title">Goal Studio</h1>
                        <p className="page-subtitle">
                            Set, balance, and track performance goals (OKRs & KPIs) for your team member.
                        </p>
                    </div>
                </div>
            </div>

            {/* ALERTS */}
            {error && (
                <div className="alert alert-error">
                    <span>⚠️</span>
                    <span>{error}</span>
                </div>
            )}

            {success && (
                <div className="alert alert-success">
                    <span>✅</span>
                    <span>{success}</span>
                </div>
            )}

            {/* EMPLOYEE & CYCLE CONTEXT HEADER */}
            <div
                className="card"
                style={{
                    marginBottom: "24px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: "16px",
                    background: "var(--bg-surface)",
                    backdropFilter: "blur(12px)",
                    border: "1px solid var(--border)"
                }}
            >
                <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                    <div className="user-avatar" style={{ width: "52px", height: "52px", fontSize: "1.3rem" }}>
                        {employee ? employee.name[0] : "E"}
                    </div>
                    <div>
                        <div style={{ fontSize: "1.25rem", fontWeight: "800", color: "var(--text-main)" }}>
                            {employee ? employee.name : `Employee #${employeeId}`}
                        </div>
                        <div style={{ fontSize: "0.85rem", color: "var(--text-muted)", fontFamily: "monospace" }}>
                            {employee ? `${employee.employeeCode} • ${employee.email}` : `ID: ${employeeId}`}
                        </div>
                    </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                    <label className="form-label" style={{ margin: 0 }}>Review Cycle:</label>
                    <select
                        className="form-select"
                        style={{ minWidth: "260px" }}
                        value={selectedCycleId}
                        onChange={(e) => handleCycleChange(Number(e.target.value))}
                    >
                        {cycles.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.name} ({c.status})
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* INACTIVE CYCLE WARNING */}
            {!isCycleActive && (
                <div className="alert alert-info">
                    <span>ℹ️</span>
                    <span>
                        Selected cycle (<strong>{selectedCycle?.name || "None"}</strong>) is <strong>{selectedCycle?.status || "CLOSED"}</strong>.
                        Goals can only be added or modified in an <strong>ACTIVE</strong> cycle.
                    </span>
                </div>
            )}

            {/* GOAL WEIGHT PROGRESS METER */}
            <div className="weight-meter">
                <div className="weight-meter-header">
                    <div>
                        <span>Goal Weight Distribution: </span>
                        <strong style={{ color: roundedTotal > 100 ? "var(--danger)" : "var(--primary)" }}>
                            {roundedTotal}%
                        </strong>
                        <span style={{ color: "var(--text-muted)" }}> / 100.00% Allocated</span>
                    </div>
                    <div>
                        <span style={{ color: "var(--text-muted)" }}>Remaining: </span>
                        <strong style={{ color: remainingWeight === 0 ? "var(--success)" : "var(--text-main)" }}>
                            {remainingWeight}%
                        </strong>
                    </div>
                </div>

                <div className="progress-bar-container">
                    <div
                        className={`progress-bar-fill ${getProgressColorClass()}`}
                        style={{ width: `${Math.min(100, roundedTotal)}%` }}
                    />
                </div>

                {roundedTotal === 100 && (
                    <div style={{ fontSize: "0.8rem", color: "var(--success-text)", fontWeight: "600", marginTop: "8px" }}>
                        ✨ Perfect! 100% of goal weight has been allocated for this review cycle.
                    </div>
                )}
                {roundedTotal > 100 && (
                    <div style={{ fontSize: "0.8rem", color: "var(--danger-text)", fontWeight: "600", marginTop: "8px" }}>
                        ⚠️ Total weight exceeds 100%. Please adjust or lower goal weights.
                    </div>
                )}
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1.6fr 1fr", gap: "24px", alignItems: "start" }}>
                {/* EXISTING GOALS LIST */}
                <div className="card">
                    <div className="card-header" style={{ marginBottom: "14px" }}>
                        <h2 className="card-title">
                            🎯 Allocated Goals ({allGoals.length})
                        </h2>
                    </div>

                    {/* SEARCH & FILTER BAR */}
                    <div style={{ display: "flex", gap: "10px", marginBottom: "16px", flexWrap: "wrap", alignItems: "center" }}>
                        <div style={{ flex: "1 1 160px" }}>
                            <input
                                type="text"
                                className="form-input"
                                placeholder="🔍 Search goals..."
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
                            style={{ minWidth: "110px" }}
                        >
                            <option value="">All Types</option>
                            <option value="OKR">OKR</option>
                            <option value="KPI">KPI</option>
                        </select>

                        <select
                            className="form-select"
                            value={scopeFilter}
                            onChange={(e) => {
                                setScopeFilter(e.target.value);
                                setPage(0);
                            }}
                            style={{ minWidth: "130px" }}
                        >
                            <option value="">All Scopes</option>
                            <option value="INDIVIDUAL">INDIVIDUAL</option>
                            <option value="TEAM">TEAM</option>
                            <option value="DEPARTMENT">DEPARTMENT</option>
                            <option value="COMPANY">COMPANY</option>
                        </select>

                        {(searchTerm || typeFilter || scopeFilter) && (
                            <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => {
                                    setSearchTerm("");
                                    setTypeFilter("");
                                    setScopeFilter("");
                                    setPage(0);
                                }}
                            >
                                ✕
                            </button>
                        )}
                    </div>

                    {loading ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            Loading goals...
                        </div>
                    ) : paginatedGoals.length === 0 ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            {allGoals.length === 0
                                ? "No goals have been created for this employee in the selected cycle."
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
                                            padding: "18px",
                                            background: "var(--bg-surface)",
                                            backdropFilter: "blur(8px)"
                                        }}
                                    >
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px", marginBottom: "8px" }}>
                                            <div>
                                                <div style={{ display: "flex", gap: "8px", alignItems: "center", marginBottom: "6px", flexWrap: "wrap" }}>
                                                    <span className={`pill ${g.goalType === "OKR" ? "pill-okr" : "pill-kpi"}`}>
                                                        {g.goalType}
                                                    </span>
                                                    <span className="pill pill-draft">{g.goalScope}</span>
                                                    <span className={`pill ${g.status === "ACTIVE" ? "pill-active" : g.status === "COMPLETED" ? "pill-completed" : "pill-draft"}`}>
                                                        {g.status}
                                                    </span>
                                                </div>
                                                <div style={{ fontWeight: "800", fontSize: "1.1rem", color: "var(--text-main)" }}>
                                                    {g.title}
                                                </div>
                                            </div>

                                            <div style={{ textAlign: "right", minWidth: "90px" }}>
                                                <div style={{ fontSize: "1.25rem", fontWeight: "800", color: "var(--primary)" }}>
                                                    {g.weight}%
                                                </div>
                                                <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Weight</div>
                                            </div>
                                        </div>

                                        {g.description && (
                                            <p style={{ fontSize: "0.9rem", color: "var(--text-secondary)", marginBottom: "10px", lineHeight: "1.4" }}>
                                                {g.description}
                                            </p>
                                        )}

                                        {g.target && (
                                            <div style={{ fontSize: "0.85rem", background: "var(--bg-subtle)", padding: "8px 12px", borderRadius: "6px", marginBottom: "12px" }}>
                                                <strong>Target KPI / Key Result:</strong> {g.target}
                                            </div>
                                        )}

                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: "10px", borderTop: "1px solid var(--border)" }}>
                                            <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                                                📅 Due: {g.dueDate || "No deadline"}
                                            </div>
                                            <div style={{ display: "flex", gap: "8px" }}>
                                                <button
                                                    type="button"
                                                    className="btn btn-secondary btn-sm"
                                                    onClick={() => openEditModal(g)}
                                                >
                                                    ✏️ Edit
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn btn-danger btn-sm"
                                                    onClick={() => handleDeleteGoal(g.id, g.title)}
                                                    disabled={deletingId === g.id}
                                                >
                                                    {deletingId === g.id ? "Deleting..." : "🗑️ Delete"}
                                                </button>
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

                {/* CREATE GOAL FORM */}
                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">➕ Add New Goal</h2>
                    </div>

                    <form onSubmit={handleCreateGoal} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                            <div className="form-group">
                                <label className="form-label">Framework</label>
                                <select
                                    className="form-select"
                                    value={createForm.goalType}
                                    onChange={(e) => setCreateForm({ ...createForm, goalType: e.target.value as "OKR" | "KPI" })}
                                >
                                    <option value="OKR">OKR (Objective)</option>
                                    <option value="KPI">KPI (Metric)</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Goal Scope</label>
                                <select
                                    className="form-select"
                                    value={createForm.goalScope}
                                    onChange={(e) => setCreateForm({ ...createForm, goalScope: e.target.value as "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY" })}
                                >
                                    <option value="INDIVIDUAL">INDIVIDUAL</option>
                                    <option value="TEAM">TEAM</option>
                                    <option value="DEPARTMENT">DEPARTMENT</option>
                                    <option value="COMPANY">COMPANY</option>
                                </select>
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Weight % (Max: {remainingWeight}%)</label>
                            <input
                                type="number"
                                className="form-input"
                                min="0.01"
                                max="100"
                                step="0.01"
                                value={createForm.weight}
                                onChange={(e) => setCreateForm({ ...createForm, weight: e.target.value })}
                                placeholder="e.g. 25.00"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Goal Title</label>
                            <input
                                type="text"
                                className="form-input"
                                value={createForm.title}
                                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                                placeholder="e.g. Ship v2.0 Microservice Architecture"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Target / Measurable Criteria</label>
                            <input
                                type="text"
                                className="form-input"
                                value={createForm.target}
                                onChange={(e) => setCreateForm({ ...createForm, target: e.target.value })}
                                placeholder="e.g. Achieve 99.9% uptime, <150ms p95 latency"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Detailed Description</label>
                            <textarea
                                className="form-textarea"
                                value={createForm.description}
                                onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                                placeholder="Describe key deliverables and milestones..."
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Target Due Date</label>
                            <input
                                type="date"
                                className="form-input"
                                value={createForm.dueDate}
                                onChange={(e) => setCreateForm({ ...createForm, dueDate: e.target.value })}
                            />
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            style={{ width: "100%", padding: "12px", marginTop: "6px" }}
                            disabled={submitting || !isCycleActive || !createForm.title.trim() || !createForm.weight}
                        >
                            {submitting ? "Allocating Goal..." : isCycleActive ? "Allocate & Save Goal" : "Cycle Inactive"}
                        </button>
                    </form>
                </div>
            </div>

            {/* EDIT GOAL MODAL */}
            {editingGoal && (
                <div className="modal-backdrop">
                    <div className="modal-card">
                        <div className="modal-header">
                            <h3 className="modal-title">Edit Performance Goal</h3>
                            <button
                                type="button"
                                className="btn-close"
                                onClick={() => setEditingGoal(null)}
                            >
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleUpdateGoal} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-group">
                                    <label className="form-label">Goal Type</label>
                                    <select
                                        className="form-select"
                                        value={editForm.goalType}
                                        onChange={(e) => setEditForm({ ...editForm, goalType: e.target.value as "OKR" | "KPI" })}
                                    >
                                        <option value="OKR">OKR</option>
                                        <option value="KPI">KPI</option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Goal Scope</label>
                                    <select
                                        className="form-select"
                                        value={editForm.goalScope}
                                        onChange={(e) => setEditForm({ ...editForm, goalScope: e.target.value as "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY" })}
                                    >
                                        <option value="INDIVIDUAL">INDIVIDUAL</option>
                                        <option value="TEAM">TEAM</option>
                                        <option value="DEPARTMENT">DEPARTMENT</option>
                                        <option value="COMPANY">COMPANY</option>
                                    </select>
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-group">
                                    <label className="form-label">Weight %</label>
                                    <input
                                        type="number"
                                        className="form-input"
                                        min="0.01"
                                        max="100"
                                        step="0.01"
                                        value={editForm.weight}
                                        onChange={(e) => setEditForm({ ...editForm, weight: e.target.value })}
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Status</label>
                                    <select
                                        className="form-select"
                                        value={editForm.status}
                                        onChange={(e) => setEditForm({ ...editForm, status: e.target.value as Goal["status"] })}
                                    >
                                        <option value="PENDING_ACCEPTANCE">PENDING_ACCEPTANCE</option>
                                        <option value="ACCEPTED">ACCEPTED</option>
                                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                                        <option value="MODIFICATION_REQUESTED">MODIFICATION_REQUESTED</option>
                                        <option value="COMPLETED">COMPLETED</option>
                                    </select>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Goal Title</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={editForm.title}
                                    onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Target / Metrics</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={editForm.target}
                                    onChange={(e) => setEditForm({ ...editForm, target: e.target.value })}
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Description</label>
                                <textarea
                                    className="form-textarea"
                                    value={editForm.description}
                                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Due Date</label>
                                <input
                                    type="date"
                                    className="form-input"
                                    value={editForm.dueDate}
                                    onChange={(e) => setEditForm({ ...editForm, dueDate: e.target.value })}
                                />
                            </div>

                            <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
                                <button type="submit" className="btn btn-primary" disabled={updating}>
                                    {updating ? "Saving Changes..." : "Save Goal Changes"}
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setEditingGoal(null)}
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </section>
    );
}