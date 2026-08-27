"use client";

import { useEffect, useState } from "react";
import { assignManager, deleteAssignment, getEmployees, getManagers, getAssignments, Employee, Assignment } from "@/services/hrService";
import Pagination from "@/components/Pagination";

export default function AssignmentsPage() {
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [managers, setManagers] = useState<Employee[]>([]);
    const [assignments, setAssignments] = useState<Assignment[]>([]);
    const [selected, setSelected] = useState<Record<number, number>>({});

    // Filter and Pagination state
    const [searchTerm, setSearchTerm] = useState("");
    const [statusFilter, setStatusFilter] = useState<"ALL" | "ASSIGNED" | "UNASSIGNED">("ALL");
    const [managerFilter, setManagerFilter] = useState<string>("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);

    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<Record<number, boolean>>({});
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    async function load() {
        setLoading(true);
        setError("");
        try {
            const [emps, mgrs, asgns] = await Promise.all([
                getEmployees(),
                getManagers(),
                getAssignments()
            ]);
            setEmployees(emps);
            setManagers(mgrs);
            setAssignments(asgns);

            const initialMap: Record<number, number> = {};
            asgns.forEach((a) => {
                if (a.active) {
                    initialMap[a.employeeId] = a.managerId;
                }
            });
            setSelected(initialMap);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to load assignment data");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    async function handleAssign(employeeId: number) {
        const managerId = selected[employeeId];
        if (!managerId) {
            setError("Please select a manager from the dropdown before assigning.");
            return;
        }

        setError("");
        setSuccess("");
        setActionLoading((prev) => ({ ...prev, [employeeId]: true }));

        try {
            const result = await assignManager(employeeId, managerId);
            setSuccess(
                `Successfully assigned ${result.managerName || "Manager"} to lead ${result.employeeName || "Employee"}!`
            );
            await load();
        } catch (e) {
            setError(e instanceof Error ? e.message : "Assignment failed");
        } finally {
            setActionLoading((prev) => ({ ...prev, [employeeId]: false }));
        }
    }

    async function handleDeactivate(assignmentId: number, employeeId: number, empName: string) {
        if (!confirm(`Are you sure you want to unassign / deactivate the manager for ${empName}?`)) {
            return;
        }

        setError("");
        setSuccess("");
        setActionLoading((prev) => ({ ...prev, [employeeId]: true }));

        try {
            await deleteAssignment(assignmentId);
            setSuccess(`Successfully unassigned reporting manager for ${empName}.`);
            await load();
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to deactivate assignment");
        } finally {
            setActionLoading((prev) => ({ ...prev, [employeeId]: false }));
        }
    }

    const activeAssignmentByEmpId: Record<number, Assignment> = {};
    assignments.forEach((a) => {
        if (a.active) {
            activeAssignmentByEmpId[a.employeeId] = a;
        }
    });

    // Multi-criteria filtering
    const filteredEmployees = employees.filter((e) => {
        const term = searchTerm.trim().toLowerCase();
        const matchesSearch =
            !term ||
            (e.name && e.name.toLowerCase().includes(term)) ||
            (e.employeeCode && e.employeeCode.toLowerCase().includes(term)) ||
            (e.email && e.email.toLowerCase().includes(term));

        const isAssigned = !!activeAssignmentByEmpId[e.id];
        const matchesStatus =
            statusFilter === "ALL" ||
            (statusFilter === "ASSIGNED" && isAssigned) ||
            (statusFilter === "UNASSIGNED" && !isAssigned);

        const currentMgrId = activeAssignmentByEmpId[e.id]?.managerId;
        const matchesManager =
            !managerFilter || (currentMgrId && String(currentMgrId) === managerFilter);

        return matchesSearch && matchesStatus && matchesManager;
    });

    const totalElements = filteredEmployees.length;
    const totalPages = Math.ceil(totalElements / size);
    const paginatedEmployees = filteredEmployees.slice(page * size, (page + 1) * size);

    return (
        <section>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Manager Linkages & Team Assignments</h1>
                    <p className="page-subtitle">
                        Map employees to their reporting managers to unlock goal setting and performance tracking workflows.
                    </p>
                </div>
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

            <div className="card">
                {/* SEARCH & FILTER BAR */}
                <div style={{ display: "flex", gap: "12px", marginBottom: "20px", flexWrap: "wrap", alignItems: "center" }}>
                    <div style={{ flex: "1 1 220px" }}>
                        <input
                            type="text"
                            className="form-input"
                            placeholder="🔍 Filter employee name/code..."
                            value={searchTerm}
                            onChange={(e) => {
                                setSearchTerm(e.target.value);
                                setPage(0);
                            }}
                        />
                    </div>

                    <div className="tabs-container">
                        {[
                            { label: "All Staff", value: "ALL" },
                            { label: "Assigned", value: "ASSIGNED" },
                            { label: "⚠️ Unassigned", value: "UNASSIGNED" }
                        ].map((tab) => (
                            <button
                                key={tab.value}
                                type="button"
                                className={`tab-btn ${statusFilter === tab.value ? "active" : ""}`}
                                onClick={() => {
                                    setStatusFilter(tab.value as any);
                                    setPage(0);
                                }}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    <select
                        className="form-select"
                        value={managerFilter}
                        onChange={(e) => {
                            setManagerFilter(e.target.value);
                            setPage(0);
                        }}
                        style={{ minWidth: "180px" }}
                    >
                        <option value="">All Managers</option>
                        {managers.map((m) => (
                            <option key={m.id} value={m.id}>
                                {m.name} ({m.employeeCode})
                            </option>
                        ))}
                    </select>

                    {(searchTerm || statusFilter !== "ALL" || managerFilter) && (
                        <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => {
                                setSearchTerm("");
                                setStatusFilter("ALL");
                                setManagerFilter("");
                                setPage(0);
                            }}
                        >
                            ✕ Clear
                        </button>
                    )}
                </div>

                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        Loading assignment records...
                    </div>
                ) : paginatedEmployees.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        No employees found matching your search & filter criteria.
                    </div>
                ) : (
                    <>
                        <div className="table-container">
                            <table className="modern-table">
                                <thead>
                                    <tr>
                                        <th>Employee</th>
                                        <th>Department</th>
                                        <th>Current Manager</th>
                                        <th>Assign / Reassign Manager</th>
                                        <th style={{ textAlign: "right" }}>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {paginatedEmployees.map((emp) => {
                                        const currentAssignment = activeAssignmentByEmpId[emp.id];
                                        const isSaving = !!actionLoading[emp.id];

                                        return (
                                            <tr key={emp.id}>
                                                <td>
                                                    <div style={{ fontWeight: "700", color: "var(--text-main)" }}>{emp.name}</div>
                                                    <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontFamily: "monospace" }}>
                                                        {emp.employeeCode} • {emp.email}
                                                    </div>
                                                </td>
                                                <td>
                                                    <span className="dept-tag">
                                                        {emp.departmentName || `Dept #${emp.departmentId ?? "N/A"}`}
                                                    </span>
                                                </td>
                                                <td>
                                                    {currentAssignment ? (
                                                        <span className="pill pill-active">
                                                            👤 {currentAssignment.managerName || `Manager #${currentAssignment.managerId}`}
                                                        </span>
                                                    ) : (
                                                        <span className="pill" style={{ background: "var(--warning-light)", color: "var(--warning-text)", border: "1px solid var(--warning-border)" }}>
                                                            ⚠️ Unassigned
                                                        </span>
                                                    )}
                                                </td>
                                                <td style={{ minWidth: "220px" }}>
                                                    <select
                                                        className="form-select"
                                                        value={selected[emp.id] ?? ""}
                                                        onChange={(x) =>
                                                            setSelected({
                                                                ...selected,
                                                                [emp.id]: Number(x.target.value)
                                                             })
                                                        }
                                                    >
                                                        <option value="">Choose Manager...</option>
                                                        {managers.map((m) => (
                                                            <option key={m.id} value={m.id}>
                                                                {m.name} ({m.employeeCode})
                                                            </option>
                                                        ))}
                                                    </select>
                                                </td>
                                                <td style={{ textAlign: "right", whiteSpace: "nowrap" }}>
                                                    <div style={{ display: "inline-flex", gap: "8px", justifyContent: "flex-end" }}>
                                                        <button
                                                            type="button"
                                                            className="btn btn-primary btn-sm"
                                                            onClick={() => handleAssign(emp.id)}
                                                            disabled={isSaving || !selected[emp.id]}
                                                        >
                                                            {isSaving ? "Saving..." : currentAssignment ? "Reassign" : "Assign"}
                                                        </button>
                                                        {currentAssignment && (
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm"
                                                                style={{
                                                                    backgroundColor: "rgba(239, 68, 68, 0.14)",
                                                                    color: "#f87171",
                                                                    border: "1px solid rgba(248, 113, 113, 0.3)"
                                                                }}
                                                                onClick={() => handleDeactivate(currentAssignment.id, emp.id, emp.name)}
                                                                disabled={isSaving}
                                                                title="Deactivate / Unassign Manager"
                                                            >
                                                                Deactivate
                                                            </button>
                                                        )}
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>

                        {/* PAGINATION */}
                        <Pagination
                            page={page}
                            size={size}
                            totalPages={totalPages}
                            totalElements={totalElements}
                            onPageChange={setPage}
                            onSizeChange={(newSize) => {
                                setSize(newSize);
                                setPage(0);
                            }}
                        />
                    </>
                )}
            </div>
        </section>
    );
}