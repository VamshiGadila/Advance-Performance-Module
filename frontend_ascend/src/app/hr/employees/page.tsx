"use client";

import { useEffect, useState } from "react";
import { searchEmployees, createManager, promoteEmployee, Employee } from "@/services/hrService";
import { getPublicDepartments, PublicDepartment } from "@/services/authService";
import Pagination from "@/components/Pagination";

export default function EmployeesPage() {
    const [tab, setTab] = useState<"directory" | "create-manager">("directory");
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [departments, setDepartments] = useState<PublicDepartment[]>([]);

    // Search and filter parameters
    const [searchName, setSearchName] = useState("");
    const [roleFilter, setRoleFilter] = useState<string>("");
    const [deptFilter, setDeptFilter] = useState<string>("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Create Manager Form State
    const [mgrName, setMgrName] = useState("");
    const [mgrEmail, setMgrEmail] = useState("");
    const [mgrPassword, setMgrPassword] = useState("");
    const [mgrDeptId, setMgrDeptId] = useState("");
    const [creatingMgr, setCreatingMgr] = useState(false);

    // Promotion Workflow State
    const [promotingEmp, setPromotingEmp] = useState<Employee | null>(null);
    const [submittingPromotion, setSubmittingPromotion] = useState(false);

    const handleConfirmPromotion = async () => {
        if (!promotingEmp) return;
        setSubmittingPromotion(true);
        setError("");
        setSuccess("");
        try {
            const updated = await promoteEmployee(promotingEmp.id);
            setSuccess(`Employee ${updated.name} (${updated.employeeCode}) was successfully promoted to MANAGER!`);
            setPromotingEmp(null);
            fetchDirectory();
        } catch (err: any) {
            setError(err.message || "Failed to promote employee to Manager");
        } finally {
            setSubmittingPromotion(false);
        }
    };

    useEffect(() => {
        getPublicDepartments()
            .then((depts) => {
                setDepartments(depts);
                if (depts.length > 0 && !mgrDeptId) {
                    setMgrDeptId(String(depts[0].id));
                }
            })
            .catch(() => {});
    }, []);

    const fetchDirectory = () => {
        setLoading(true);
        setError("");

        searchEmployees({
            search: searchName.trim() || undefined,
            role: roleFilter || undefined,
            departmentId: deptFilter ? Number(deptFilter) : undefined,
            page,
            size,
            sortBy: "name",
            direction: "asc"
        })
            .then((res) => {
                setEmployees(res.content || []);
                setTotalPages(res.totalPages || 0);
                setTotalElements(res.totalElements || 0);
            })
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load directory"))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        if (tab === "directory") {
            fetchDirectory();
        }
    }, [tab, searchName, roleFilter, deptFilter, page, size]);

    const handleCreateManager = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccess("");
        setCreatingMgr(true);

        try {
            const created = await createManager({
                name: mgrName.trim(),
                email: mgrEmail.trim(),
                temporaryPassword: mgrPassword,
                departmentId: Number(mgrDeptId)
            });

            setSuccess(`🎉 Manager ${created.name} (${created.employeeCode}) created successfully!`);
            setMgrName("");
            setMgrEmail("");
            setMgrPassword("");
            setTab("directory");
            setRoleFilter("MANAGER");
            setPage(0);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to create manager");
        } finally {
            setCreatingMgr(false);
        }
    };

    const handleClearFilters = () => {
        setSearchName("");
        setRoleFilter("");
        setDeptFilter("");
        setPage(0);
    };

    return (
        <section>
            <div className="page-header">
                <div>
                    <h1 className="page-title">People & Roles Directory</h1>
                    <p className="page-subtitle">Manage organization employees, provision managers, and search with live filters.</p>
                </div>
                <button
                    className="btn btn-primary"
                    onClick={() => {
                        setTab("create-manager");
                        setError("");
                        setSuccess("");
                    }}
                >
                    <span>➕</span>
                    <span>Add New Manager</span>
                </button>
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

            {/* TAB NAVIGATION */}
            <div className="tabs-container" style={{ marginBottom: "24px" }}>
                <button
                    type="button"
                    className={`tab-btn ${tab === "directory" ? "active" : ""}`}
                    onClick={() => {
                        setTab("directory");
                        setError("");
                    }}
                >
                    👥 Directory & Roles ({totalElements})
                </button>
                <button
                    type="button"
                    className={`tab-btn ${tab === "create-manager" ? "active" : ""}`}
                    onClick={() => {
                        setTab("create-manager");
                        setError("");
                    }}
                >
                    ➕ Create Manager
                </button>
            </div>

            {tab === "create-manager" ? (
                <div className="card" style={{ maxWidth: "600px" }}>
                    <div className="card-header">
                        <h2 className="card-title">Register Team Manager</h2>
                    </div>
                    <form onSubmit={handleCreateManager} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div className="form-group">
                            <label className="form-label">Full Name</label>
                            <input
                                type="text"
                                className="form-input"
                                value={mgrName}
                                onChange={(e) => setMgrName(e.target.value)}
                                placeholder="Sarah Connor"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Official Email</label>
                            <input
                                type="email"
                                className="form-input"
                                value={mgrEmail}
                                onChange={(e) => setMgrEmail(e.target.value)}
                                placeholder="sarah.connor@ascend.local"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Department</label>
                            {departments.length > 0 ? (
                                <select
                                    className="form-select"
                                    value={mgrDeptId}
                                    onChange={(e) => setMgrDeptId(e.target.value)}
                                    required
                                >
                                    {departments.map((d) => (
                                        <option key={d.id} value={d.id}>
                                            {d.name}
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <input
                                    type="number"
                                    className="form-input"
                                    value={mgrDeptId}
                                    onChange={(e) => setMgrDeptId(e.target.value)}
                                    placeholder="Department ID (e.g. 10)"
                                    required
                                />
                            )}
                        </div>

                        <div className="form-group">
                            <label className="form-label">Temporary Password</label>
                            <input
                                type="password"
                                className="form-input"
                                value={mgrPassword}
                                onChange={(e) => setMgrPassword(e.target.value)}
                                placeholder="Min 8 characters"
                                required
                            />
                        </div>

                        <div style={{ display: "flex", gap: "12px", marginTop: "8px" }}>
                            <button type="submit" className="btn btn-primary" disabled={creatingMgr}>
                                {creatingMgr ? "Creating Manager..." : "Create & Activate Manager"}
                            </button>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setTab("directory")}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            ) : (
                <div className="card">
                    {/* SEARCH & FILTER CONTROLS */}
                    <div style={{ display: "flex", gap: "12px", marginBottom: "20px", flexWrap: "wrap", alignItems: "center" }}>
                        <div style={{ flex: "1 1 240px" }}>
                            <input
                                type="text"
                                className="form-input"
                                placeholder="🔍 Search by name or code..."
                                value={searchName}
                                onChange={(e) => {
                                    setSearchName(e.target.value);
                                    setPage(0);
                                }}
                            />
                        </div>

                        <div className="tabs-container">
                            {[
                                { label: "All Roles", value: "" },
                                { label: "Employees", value: "EMPLOYEE" },
                                { label: "Managers", value: "MANAGER" },
                                { label: "HR Admins", value: "HR" }
                            ].map((r) => (
                                <button
                                    key={r.value}
                                    type="button"
                                    className={`tab-btn ${roleFilter === r.value ? "active" : ""}`}
                                    onClick={() => {
                                        setRoleFilter(r.value);
                                        setPage(0);
                                    }}
                                >
                                    {r.label}
                                </button>
                            ))}
                        </div>

                        <select
                            className="form-select"
                            value={deptFilter}
                            onChange={(e) => {
                                setDeptFilter(e.target.value);
                                setPage(0);
                            }}
                            style={{ minWidth: "180px" }}
                        >
                            <option value="">All Departments</option>
                            {departments.map((d) => (
                                <option key={d.id} value={d.id}>
                                    {d.name}
                                </option>
                            ))}
                        </select>

                        {(searchName || roleFilter || deptFilter) && (
                            <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={handleClearFilters}
                            >
                                ✕ Clear Filters
                            </button>
                        )}
                    </div>

                    {loading ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            Searching directory...
                        </div>
                    ) : employees.length === 0 ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            No users found matching your search and filter criteria.
                        </div>
                    ) : (
                        <>
                            <div className="table-container">
                                <table className="modern-table">
                                    <thead>
                                        <tr>
                                            <th>EMP ID</th>
                                            <th>Role Code</th>
                                            <th>Full Name</th>
                                            <th>Email Address</th>
                                            <th>Role</th>
                                            <th>Department</th>
                                            <th>Skills & Domain</th>
                                            <th>Status</th>
                                            <th>Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {employees.map((emp) => (
                                            <tr key={emp.id}>
                                                <td>
                                                    <span className="id-badge" title={`Permanent Serial Employee ID #${emp.id}`}>
                                                        #{String(emp.id).padStart(3, '0')}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span style={{ fontWeight: "700", color: emp.role === "MANAGER" ? "#8b5cf6" : "var(--primary)", fontFamily: "monospace" }}>
                                                        {emp.employeeCode}
                                                    </span>
                                                </td>
                                                <td style={{ fontWeight: "600" }}>{emp.name}</td>
                                                <td style={{ color: "var(--text-muted)" }}>{emp.email}</td>
                                                <td>
                                                    <span className={`pill ${emp.role === "MANAGER" ? "pill-okr" : emp.role === "HR" ? "pill-completed" : "pill-active"}`}>
                                                        {emp.role}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span className="dept-tag">
                                                        {emp.departmentName || `Dept #${emp.departmentId ?? "N/A"}`}
                                                    </span>
                                                </td>
                                                <td style={{ fontSize: "0.825rem", color: "var(--text-secondary)" }}>
                                                    {emp.skill || emp.domain ? `${emp.skill || ""}${emp.domain ? ` • ${emp.domain}` : ""}` : "-"}
                                                </td>
                                                <td>
                                                    <span className="pill pill-active">Active</span>
                                                </td>
                                                <td>
                                                    {emp.role === "EMPLOYEE" ? (
                                                        <button
                                                            type="button"
                                                            onClick={() => setPromotingEmp(emp)}
                                                            className="btn btn-secondary btn-sm"
                                                            style={{
                                                                fontSize: "0.75rem",
                                                                padding: "4px 10px",
                                                                borderColor: "rgba(96, 165, 250, 0.4)",
                                                                color: "#60a5fa",
                                                                fontWeight: "600"
                                                            }}
                                                            title="Promote this employee to Manager"
                                                        >
                                                            ⭐ Promote
                                                        </button>
                                                    ) : (
                                                        <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>—</span>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
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
            )}

            {/* PROMOTION CONFIRMATION MODAL */}
            {promotingEmp && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "500px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                                <div style={{
                                    width: "44px",
                                    height: "44px",
                                    borderRadius: "14px",
                                    background: "rgba(245, 158, 11, 0.15)",
                                    border: "1px solid rgba(245, 158, 11, 0.25)",
                                    display: "grid",
                                    placeItems: "center",
                                    fontSize: "1.35rem"
                                }}>
                                    ⭐
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                        Promote to Manager
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                        Generates MGR designation while keeping Serial ID
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setPromotingEmp(null)}
                                className="btn-close"
                                title="Close modal"
                            >
                                ✕
                            </button>
                        </div>

                        <div className="card" style={{ padding: "16px", marginBottom: "20px", background: "var(--bg-subtle)" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "12px" }}>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Employee</div>
                                    <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "0.95rem" }}>{promotingEmp.name}</div>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Serial EMP ID</div>
                                    <span className="id-badge">#{String(promotingEmp.id).padStart(3, '0')}</span>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Current Code</div>
                                    <span style={{ fontFamily: "monospace", color: "var(--primary)", fontWeight: "700" }}>{promotingEmp.employeeCode}</span>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>New Role Code</div>
                                    <span style={{ fontFamily: "monospace", color: "#8b5cf6", fontWeight: "700" }}>MGR{promotingEmp.employeeCode.replace(/^EMP/, '')}</span>
                                </div>
                            </div>
                            <div style={{ fontSize: "0.825rem", color: "var(--text-secondary)", lineHeight: "1.5", borderTop: "1px solid var(--border)", paddingTop: "10px" }}>
                                Elevates permissions to <strong>MANAGER</strong>. Permanent serial ID (<strong style={{ color: "var(--text-main)" }}>#{String(promotingEmp.id).padStart(3, '0')}</strong>) remains immutable.
                            </div>
                        </div>

                        <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setPromotingEmp(null)}
                                disabled={submittingPromotion}
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={handleConfirmPromotion}
                                disabled={submittingPromotion}
                            >
                                {submittingPromotion ? "Promoting..." : "Confirm Promotion ⭐"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}