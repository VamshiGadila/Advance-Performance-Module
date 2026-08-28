"use client";

import { useEffect, useState } from "react";
import {
    searchEmployees,
    getManagers,
    createManager,
    promoteEmployee,
    changeEmployeeManager,
    transferEmployeeDepartment,
    getManagerHierarchy,
    Employee,
    ManagerHierarchyNode
} from "@/services/hrService";
import { getPublicDepartments, PublicDepartment } from "@/services/authService";
import Pagination from "@/components/Pagination";
import {
    Users,
    GitMerge,
    UserPlus,
    Search,
    FilterX,
    Briefcase,
    AlertCircle,
    Award,
    UserCheck,
    Building2,
    X,
    ArrowRight,
    CheckCircle2
} from "lucide-react";

export default function EmployeesPage() {
    const [tab, setTab] = useState<"directory" | "hierarchy" | "create-manager">("directory");
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [managersList, setManagersList] = useState<Employee[]>([]);
    const [departments, setDepartments] = useState<PublicDepartment[]>([]);
    const [hierarchy, setHierarchy] = useState<ManagerHierarchyNode[]>([]);
    const [loadingHierarchy, setLoadingHierarchy] = useState(false);

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
    const [mgrDesignation, setMgrDesignation] = useState("");
    const [creatingMgr, setCreatingMgr] = useState(false);

    // Promotion Workflow State
    const [promotingEmp, setPromotingEmp] = useState<Employee | null>(null);
    const [submittingPromotion, setSubmittingPromotion] = useState(false);

    // Change Manager Workflow State
    const [managingEmp, setManagingEmp] = useState<Employee | null>(null);
    const [selectedManagerId, setSelectedManagerId] = useState("");
    const [submittingManagerChange, setSubmittingManagerChange] = useState(false);

    // Transfer Department Workflow State
    const [transferringEmp, setTransferringEmp] = useState<Employee | null>(null);
    const [selectedDeptId, setSelectedDeptId] = useState("");
    const [submittingDeptTransfer, setSubmittingDeptTransfer] = useState(false);

    useEffect(() => {
        getPublicDepartments()
            .then((depts) => {
                setDepartments(depts);
                if (depts.length > 0 && !mgrDeptId) {
                    setMgrDeptId(String(depts[0].id));
                }
            })
            .catch(() => {});

        getManagers()
            .then(setManagersList)
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

    const fetchHierarchy = () => {
        setLoadingHierarchy(true);
        setError("");
        getManagerHierarchy()
            .then((data) => setHierarchy(data || []))
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load hierarchy"))
            .finally(() => setLoadingHierarchy(false));
    };

    useEffect(() => {
        if (tab === "directory") {
            fetchDirectory();
        } else if (tab === "hierarchy") {
            fetchHierarchy();
        }
    }, [tab, searchName, roleFilter, deptFilter, page, size]);

    // Clear Search Filters
    const handleClearFilters = () => {
        setSearchName("");
        setRoleFilter("");
        setDeptFilter("");
        setPage(0);
    };

    // Promote to Manager
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
            getManagers().then(setManagersList).catch(() => {});
        } catch (err: any) {
            setError(err.message || "Failed to promote employee to Manager");
        } finally {
            setSubmittingPromotion(false);
        }
    };

    // Change Reporting Manager
    const handleConfirmManagerChange = async () => {
        if (!managingEmp || !selectedManagerId) return;
        setSubmittingManagerChange(true);
        setError("");
        setSuccess("");
        try {
            const updated = await changeEmployeeManager(managingEmp.id, Number(selectedManagerId));
            setSuccess(`Reporting manager for ${updated.name} updated successfully!`);
            setManagingEmp(null);
            fetchDirectory();
            if (tab === "hierarchy") fetchHierarchy();
        } catch (err: any) {
            setError(err.message || "Failed to reassign manager");
        } finally {
            setSubmittingManagerChange(false);
        }
    };

    // Transfer Department
    const handleConfirmDeptTransfer = async () => {
        if (!transferringEmp || !selectedDeptId) return;
        setSubmittingDeptTransfer(true);
        setError("");
        setSuccess("");
        try {
            const updated = await transferEmployeeDepartment(transferringEmp.id, Number(selectedDeptId));
            setSuccess(`Employee ${updated.name} transferred to new department successfully!`);
            setTransferringEmp(null);
            fetchDirectory();
            if (tab === "hierarchy") fetchHierarchy();
        } catch (err: any) {
            setError(err.message || "Failed to transfer department");
        } finally {
            setSubmittingDeptTransfer(false);
        }
    };

    // Create Manager
    const handleCreateManager = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setSuccess("");
        setCreatingMgr(true);

        try {
            const created = await createManager({
                name: mgrName.trim(),
                email: mgrEmail.trim(),
                password: mgrPassword || "Password1",
                departmentId: Number(mgrDeptId)
            });

            setSuccess(`Manager account successfully created for ${created.name} (${created.employeeCode})!`);
            setMgrName("");
            setMgrEmail("");
            setMgrPassword("");
            setMgrDesignation("");
            setTab("directory");
            fetchDirectory();
            getManagers().then(setManagersList).catch(() => {});
        } catch (err: any) {
            setError(err.message || "Failed to create manager account");
        } finally {
            setCreatingMgr(false);
        }
    };

    return (
        <section className="section-container">
            {/* HEADER */}
            <div className="section-header">
                <div>
                    <h1 className="page-title">Employees & Managers</h1>
                    <p className="page-subtitle">
                        Manage organizational structure, reporting hierarchies, promotions, and departmental assignments
                    </p>
                </div>
            </div>

            {/* NOTIFICATIONS */}
            {error && (
                <div className="alert-banner alert-error" style={{ marginBottom: "20px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <AlertCircle size={16} />
                        <span>{error}</span>
                    </div>
                    <button type="button" onClick={() => setError("")} className="btn-close"><X size={16} /></button>
                </div>
            )}

            {success && (
                <div className="alert-banner alert-success" style={{ marginBottom: "20px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <CheckCircle2 size={16} />
                        <span>{success}</span>
                    </div>
                    <button type="button" onClick={() => setSuccess("")} className="btn-close"><X size={16} /></button>
                </div>
            )}

            {/* TAB SELECTOR */}
            <div style={{ display: "flex", gap: "10px", marginBottom: "24px", borderBottom: "1px solid var(--border)", paddingBottom: "12px" }}>
                <button
                    type="button"
                    onClick={() => setTab("directory")}
                    className={`btn ${tab === "directory" ? "btn-primary" : "btn-secondary"}`}
                    style={{ display: "flex", alignItems: "center", gap: "8px" }}
                >
                    <Users size={16} />
                    <span>Employee Directory</span>
                </button>
                <button
                    type="button"
                    onClick={() => setTab("hierarchy")}
                    className={`btn ${tab === "hierarchy" ? "btn-primary" : "btn-secondary"}`}
                    style={{ display: "flex", alignItems: "center", gap: "8px" }}
                >
                    <GitMerge size={16} />
                    <span>Manager Hierarchy Tree</span>
                </button>
                <button
                    type="button"
                    onClick={() => setTab("create-manager")}
                    className={`btn ${tab === "create-manager" ? "btn-primary" : "btn-secondary"}`}
                    style={{ display: "flex", alignItems: "center", gap: "8px" }}
                >
                    <UserPlus size={16} />
                    <span>Register Manager</span>
                </button>
            </div>

            {/* TAB 1: DIRECTORY */}
            {tab === "directory" && (
                <div className="card" style={{ padding: "24px" }}>
                    {/* SEARCH & FILTERS BAR */}
                    <div style={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: "12px",
                        alignItems: "center",
                        marginBottom: "20px",
                        padding: "16px",
                        background: "var(--bg-subtle)",
                        borderRadius: "12px",
                        border: "1px solid var(--border)"
                    }}>
                        <div style={{ flex: "1 1 280px", position: "relative" }}>
                            <Search size={16} style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)", pointerEvents: "none" }} />
                            <input
                                type="text"
                                placeholder="Search by EMP ID (#024), code, name, email..."
                                value={searchName}
                                onChange={(e) => {
                                    setSearchName(e.target.value);
                                    setPage(0);
                                }}
                                className="form-input"
                                style={{ width: "100%", paddingLeft: "38px" }}
                            />
                        </div>

                        <select
                            value={roleFilter}
                            onChange={(e) => {
                                setRoleFilter(e.target.value);
                                setPage(0);
                            }}
                            className="form-input"
                            style={{ flex: "0 0 160px" }}
                        >
                            <option value="">All Roles</option>
                            <option value="EMPLOYEE">Employees</option>
                            <option value="MANAGER">Managers</option>
                            <option value="HR">HR Admins</option>
                        </select>

                        <select
                            value={deptFilter}
                            onChange={(e) => {
                                setDeptFilter(e.target.value);
                                setPage(0);
                            }}
                            className="form-input"
                            style={{ flex: "0 0 200px" }}
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
                                style={{ display: "flex", alignItems: "center", gap: "6px" }}
                            >
                                <FilterX size={14} />
                                <span>Clear Filters</span>
                            </button>
                        )}
                    </div>

                    {loading ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            Searching organizational directory...
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
                                            <th>Designation / Job Title</th>
                                            <th>Reporting Manager</th>
                                            <th>Status</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {employees.map((emp) => (
                                            <tr key={emp.id}>
                                                {/* PERMANENT SERIAL ID */}
                                                <td>
                                                    <span className="id-badge" title={`Permanent Serial Employee ID #${emp.id}`}>
                                                        #{String(emp.id).padStart(3, '0')}
                                                    </span>
                                                </td>

                                                {/* ROLE CODE */}
                                                <td>
                                                    <span style={{
                                                        fontWeight: "700",
                                                        color: emp.role === "MANAGER" ? "#8b5cf6" : emp.role === "HR" ? "#10b981" : "var(--primary)",
                                                        fontFamily: "monospace"
                                                    }}>
                                                        {emp.employeeCode}
                                                    </span>
                                                </td>

                                                {/* FULL NAME */}
                                                <td style={{ fontWeight: "600" }}>{emp.name}</td>

                                                {/* EMAIL */}
                                                <td style={{ color: "var(--text-muted)", fontSize: "0.85rem" }}>{emp.email}</td>

                                                {/* ROLE */}
                                                <td>
                                                    <span className={`pill ${emp.role === "MANAGER" ? "pill-okr" : emp.role === "HR" ? "pill-completed" : "pill-active"}`}>
                                                        {emp.role}
                                                    </span>
                                                </td>

                                                {/* DEPARTMENT */}
                                                <td>
                                                    <span className="dept-tag">
                                                        {emp.departmentName || `Dept #${emp.departmentId ?? "N/A"}`}
                                                    </span>
                                                </td>

                                                {/* DESIGNATION / JOB TITLE */}
                                                <td>
                                                    <span style={{
                                                        display: "inline-flex",
                                                        alignItems: "center",
                                                        gap: "5px",
                                                        background: "rgba(59, 130, 246, 0.08)",
                                                        border: "1px solid rgba(59, 130, 246, 0.2)",
                                                        color: "#93c5fd",
                                                        padding: "3px 8px",
                                                        borderRadius: "6px",
                                                        fontSize: "0.8rem",
                                                        fontWeight: "500"
                                                    }}>
                                                        <Briefcase size={12} />
                                                        <span>{emp.designation || (emp.role === "MANAGER" ? "Engineering Manager" : emp.role === "HR" ? "HR Administrator" : "Software Engineer")}</span>
                                                    </span>
                                                </td>

                                                {/* REPORTING MANAGER */}
                                                <td>
                                                    {emp.role === "EMPLOYEE" ? (
                                                        emp.managerName ? (
                                                            <div style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
                                                                <span style={{
                                                                    color: "#a78bfa",
                                                                    fontFamily: "monospace",
                                                                    fontWeight: "700",
                                                                    fontSize: "0.8rem"
                                                                }}>
                                                                    {emp.managerCode || "MGR"}
                                                                </span>
                                                                <span style={{ fontSize: "0.825rem", color: "var(--text-main)", fontWeight: "500" }}>
                                                                    {emp.managerName}
                                                                </span>
                                                            </div>
                                                        ) : (
                                                            <span style={{
                                                                display: "inline-flex",
                                                                alignItems: "center",
                                                                gap: "4px",
                                                                fontSize: "0.75rem",
                                                                color: "#f59e0b",
                                                                background: "rgba(245, 158, 11, 0.1)",
                                                                border: "1px solid rgba(245, 158, 11, 0.25)",
                                                                padding: "2px 8px",
                                                                borderRadius: "6px"
                                                            }}>
                                                                <AlertCircle size={11} />
                                                                <span>Unassigned</span>
                                                            </span>
                                                        )
                                                    ) : emp.role === "MANAGER" ? (
                                                        <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontStyle: "italic" }}>
                                                            Team Lead
                                                        </span>
                                                    ) : (
                                                        <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>—</span>
                                                    )}
                                                </td>

                                                {/* STATUS */}
                                                <td>
                                                    <span className="pill pill-active">Active</span>
                                                </td>

                                                {/* ACTIONS */}
                                                <td>
                                                    <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
                                                        {emp.role === "EMPLOYEE" && (
                                                            <>
                                                                <button
                                                                    type="button"
                                                                    onClick={() => setPromotingEmp(emp)}
                                                                    className="btn btn-secondary btn-sm"
                                                                    style={{
                                                                        display: "flex",
                                                                        alignItems: "center",
                                                                        gap: "4px",
                                                                        fontSize: "0.72rem",
                                                                        padding: "3px 8px",
                                                                        color: "#60a5fa",
                                                                        borderColor: "rgba(96, 165, 250, 0.4)"
                                                                    }}
                                                                    title="Promote this employee to Manager"
                                                                >
                                                                    <Award size={12} />
                                                                    <span>Promote</span>
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    onClick={() => {
                                                                        setManagingEmp(emp);
                                                                        setSelectedManagerId(emp.managerId ? String(emp.managerId) : "");
                                                                    }}
                                                                    className="btn btn-secondary btn-sm"
                                                                    style={{
                                                                        display: "flex",
                                                                        alignItems: "center",
                                                                        gap: "4px",
                                                                        fontSize: "0.72rem",
                                                                        padding: "3px 8px",
                                                                        color: "#a78bfa",
                                                                        borderColor: "rgba(167, 139, 250, 0.4)"
                                                                    }}
                                                                    title="Assign or Change Reporting Manager"
                                                                >
                                                                    <UserCheck size={12} />
                                                                    <span>Manager</span>
                                                                </button>
                                                            </>
                                                        )}
                                                        <button
                                                            type="button"
                                                            onClick={() => {
                                                                setTransferringEmp(emp);
                                                                setSelectedDeptId(emp.departmentId ? String(emp.departmentId) : "");
                                                            }}
                                                            className="btn btn-secondary btn-sm"
                                                            style={{
                                                                display: "flex",
                                                                alignItems: "center",
                                                                gap: "4px",
                                                                fontSize: "0.72rem",
                                                                padding: "3px 8px",
                                                                color: "#34d399",
                                                                borderColor: "rgba(52, 211, 153, 0.4)"
                                                            }}
                                                            title="Transfer Employee Department"
                                                        >
                                                            <Building2 size={12} />
                                                            <span>Transfer</span>
                                                        </button>
                                                    </div>
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

            {/* TAB 2: MANAGER HIERARCHY TREE */}
            {tab === "hierarchy" && (
                <div className="card" style={{ padding: "24px" }}>
                    <div style={{ marginBottom: "20px" }}>
                        <h2 style={{ fontSize: "1.2rem", fontWeight: "700", margin: "0 0 6px" }}>Organizational Hierarchy Tree</h2>
                        <p style={{ color: "var(--text-muted)", fontSize: "0.875rem", margin: 0 }}>
                            Real-time view of department managers and their assigned reporting employees
                        </p>
                    </div>

                    {loadingHierarchy ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            Loading organizational tree...
                        </div>
                    ) : hierarchy.length === 0 ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            No manager reporting structures configured yet.
                        </div>
                    ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                            {hierarchy.map((mgr) => (
                                <div
                                    key={mgr.managerId}
                                    style={{
                                        border: "1px solid var(--border)",
                                        borderRadius: "12px",
                                        background: "var(--bg-subtle)",
                                        overflow: "hidden"
                                    }}
                                >
                                    {/* MANAGER HEADER */}
                                    <div style={{
                                        padding: "16px 20px",
                                        background: "rgba(139, 92, 246, 0.08)",
                                        borderBottom: "1px solid var(--border)",
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center",
                                        flexWrap: "wrap",
                                        gap: "12px"
                                    }}>
                                        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                            <div style={{
                                                width: "40px",
                                                height: "40px",
                                                borderRadius: "10px",
                                                background: "rgba(139, 92, 246, 0.2)",
                                                border: "1px solid rgba(139, 92, 246, 0.3)",
                                                display: "grid",
                                                placeItems: "center",
                                                color: "#c4b5fd",
                                                fontWeight: "800"
                                            }}>
                                                MGR
                                            </div>
                                            <div>
                                                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                                                    <span style={{ fontWeight: "700", fontSize: "1rem", color: "var(--text-main)" }}>
                                                        {mgr.managerName}
                                                    </span>
                                                    <span style={{ fontFamily: "monospace", color: "#a78bfa", fontWeight: "700", fontSize: "0.85rem" }}>
                                                        {mgr.managerCode}
                                                    </span>
                                                    <span className="id-badge">#{String(mgr.managerId).padStart(3, '0')}</span>
                                                </div>
                                                <div style={{ fontSize: "0.825rem", color: "var(--text-muted)", marginTop: "2px" }}>
                                                    {mgr.managerDesignation || "Engineering Manager"} • {mgr.departmentName || "General"}
                                                </div>
                                            </div>
                                        </div>

                                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                                            <span style={{
                                                display: "inline-flex",
                                                alignItems: "center",
                                                gap: "6px",
                                                background: "rgba(139, 92, 246, 0.15)",
                                                border: "1px solid rgba(139, 92, 246, 0.3)",
                                                color: "#c4b5fd",
                                                padding: "4px 12px",
                                                borderRadius: "20px",
                                                fontSize: "0.8rem",
                                                fontWeight: "600"
                                            }}>
                                                <Users size={13} />
                                                <span>{mgr.totalReports} Direct Reports</span>
                                            </span>
                                        </div>
                                    </div>

                                    {/* DIRECT REPORTS LIST */}
                                    <div style={{ padding: "16px 20px" }}>
                                        {mgr.directReports.length === 0 ? (
                                            <div style={{ color: "var(--text-muted)", fontSize: "0.85rem", fontStyle: "italic", padding: "10px 0" }}>
                                                No employees currently assigned to this manager. Use the Directory to assign staff.
                                            </div>
                                        ) : (
                                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "12px" }}>
                                                {mgr.directReports.map((rep) => (
                                                    <div
                                                        key={rep.id}
                                                        style={{
                                                            padding: "12px",
                                                            borderRadius: "8px",
                                                            border: "1px solid var(--border)",
                                                            background: "var(--bg-card)",
                                                            display: "flex",
                                                            flexDirection: "column",
                                                            gap: "6px"
                                                        }}
                                                    >
                                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                                            <span style={{ fontWeight: "600", fontSize: "0.9rem", color: "var(--text-main)" }}>
                                                                {rep.name}
                                                            </span>
                                                            <span className="id-badge">#{String(rep.id).padStart(3, '0')}</span>
                                                        </div>
                                                        <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.78rem" }}>
                                                            <span style={{ fontFamily: "monospace", color: "var(--primary)", fontWeight: "700" }}>
                                                                {rep.employeeCode}
                                                            </span>
                                                            <span style={{ color: "var(--text-muted)" }}>•</span>
                                                            <span style={{ color: "var(--text-secondary)" }}>
                                                                {rep.designation || "Software Engineer"}
                                                            </span>
                                                        </div>
                                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "4px" }}>
                                                            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>{rep.email}</span>
                                                            <button
                                                                type="button"
                                                                onClick={() => {
                                                                    setManagingEmp(rep);
                                                                    setSelectedManagerId(String(mgr.managerId));
                                                                }}
                                                                className="btn btn-secondary btn-sm"
                                                                style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "0.7rem", padding: "2px 8px" }}
                                                                title="Move to another Manager"
                                                            >
                                                                <span>Move</span>
                                                                <ArrowRight size={11} />
                                                            </button>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* TAB 3: REGISTER MANAGER */}
            {tab === "create-manager" && (
                <div className="card" style={{ maxWidth: "560px", margin: "0 auto", padding: "32px" }}>
                    <div style={{ marginBottom: "24px" }}>
                        <h2 style={{ fontSize: "1.25rem", fontWeight: "700", margin: "0 0 6px" }}>Register Team Manager</h2>
                        <p style={{ color: "var(--text-muted)", fontSize: "0.875rem", margin: 0 }}>
                            Provision a new manager account with dedicated MGR code and team management access
                        </p>
                    </div>

                    <form onSubmit={handleCreateManager} style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
                        <div className="form-group">
                            <label className="form-label">Full Name *</label>
                            <input
                                type="text"
                                required
                                value={mgrName}
                                onChange={(e) => setMgrName(e.target.value)}
                                placeholder="e.g. Alice Smith"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Corporate Email Address *</label>
                            <input
                                type="email"
                                required
                                value={mgrEmail}
                                onChange={(e) => setMgrEmail(e.target.value)}
                                placeholder="e.g. alice.smith@ascend.local"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Department *</label>
                            <select
                                required
                                value={mgrDeptId}
                                onChange={(e) => setMgrDeptId(e.target.value)}
                                className="form-input"
                            >
                                {departments.map((d) => (
                                    <option key={d.id} value={d.id}>
                                        {d.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Managerial Designation (Optional)</label>
                            <input
                                type="text"
                                value={mgrDesignation}
                                onChange={(e) => setMgrDesignation(e.target.value)}
                                placeholder="e.g. Engineering Manager (defaults by department)"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Temporary Password</label>
                            <input
                                type="password"
                                value={mgrPassword}
                                onChange={(e) => setMgrPassword(e.target.value)}
                                placeholder="Defaults to 'Password1'"
                                className="form-input"
                            />
                            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px" }}>
                                Minimum 6 characters. Leave blank for default password.
                            </span>
                        </div>

                        <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px", marginTop: "12px" }}>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setTab("directory")}
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                className="btn btn-primary"
                                disabled={creatingMgr}
                                style={{ display: "flex", alignItems: "center", gap: "6px" }}
                            >
                                <UserPlus size={15} />
                                <span>{creatingMgr ? "Creating Account..." : "Create Manager Account"}</span>
                            </button>
                        </div>
                    </form>
                </div>
            )}

            {/* MODAL 1: PROMOTION CONFIRMATION */}
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
                                    placeItems: "center"
                                }}>
                                    <Award size={22} style={{ color: "#f59e0b" }} />
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                        Promote to Manager
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                        Elevates permissions while preserving permanent Serial ID
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setPromotingEmp(null)}
                                className="btn-close"
                                title="Close modal"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div className="card" style={{ padding: "16px", marginBottom: "20px", background: "var(--bg-subtle)" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "12px" }}>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Employee</div>
                                    <div style={{ fontWeight: "700", color: "var(--text-main)", fontSize: "0.95rem" }}>{promotingEmp.name}</div>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Permanent ID</div>
                                    <span className="id-badge">#{String(promotingEmp.id).padStart(3, '0')}</span>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Role Transition</div>
                                    <span style={{ fontFamily: "monospace", color: "var(--primary)", fontWeight: "700" }}>{promotingEmp.employeeCode}</span>
                                    {" ➔ "}
                                    <span style={{ fontFamily: "monospace", color: "#8b5cf6", fontWeight: "700" }}>MGR{promotingEmp.employeeCode.replace(/^EMP/, '')}</span>
                                </div>
                                <div>
                                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase" }}>Designation Upgrade</div>
                                    <span style={{ fontSize: "0.82rem", color: "#93c5fd", fontWeight: "600" }}>
                                        {promotingEmp.departmentName || "Team"} Manager
                                    </span>
                                </div>
                            </div>
                            <div style={{ fontSize: "0.825rem", color: "var(--text-secondary)", lineHeight: "1.5", borderTop: "1px solid var(--border)", paddingTop: "10px" }}>
                                Note: Promotion elevates organizational authority. Per enterprise policy, team members must be explicitly assigned to this Manager afterward.
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
                                style={{ display: "flex", alignItems: "center", gap: "6px" }}
                            >
                                <Award size={15} />
                                <span>{submittingPromotion ? "Promoting..." : "Confirm Promotion"}</span>
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* MODAL 2: ASSIGN / CHANGE MANAGER */}
            {managingEmp && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                <div style={{
                                    width: "40px",
                                    height: "40px",
                                    borderRadius: "12px",
                                    background: "rgba(167, 139, 250, 0.15)",
                                    border: "1px solid rgba(167, 139, 250, 0.3)",
                                    display: "grid",
                                    placeItems: "center"
                                }}>
                                    <UserCheck size={20} style={{ color: "#a78bfa" }} />
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.15rem", fontWeight: "700", color: "var(--text-main)", margin: 0 }}>
                                        Assign Reporting Manager
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "2px 0 0" }}>
                                        Select the supervisor for {managingEmp.name}
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setManagingEmp(null)}
                                className="btn-close"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div style={{ marginBottom: "16px" }}>
                            <label className="form-label">Select Manager *</label>
                            <select
                                value={selectedManagerId}
                                onChange={(e) => setSelectedManagerId(e.target.value)}
                                className="form-input"
                                style={{ width: "100%" }}
                            >
                                <option value="">-- Choose a Manager --</option>
                                {managersList
                                    .filter((m) => m.id !== managingEmp.id)
                                    .map((m) => (
                                        <option key={m.id} value={m.id}>
                                            {m.name} ({m.employeeCode}) • {m.departmentName || "General"}
                                        </option>
                                    ))}
                            </select>
                            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px", display: "block" }}>
                                Current: {managingEmp.managerName ? `${managingEmp.managerName} (${managingEmp.managerCode})` : "Unassigned"}
                            </span>
                        </div>

                        <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setManagingEmp(null)}
                                disabled={submittingManagerChange}
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={handleConfirmManagerChange}
                                disabled={submittingManagerChange || !selectedManagerId}
                                style={{ display: "flex", alignItems: "center", gap: "6px" }}
                            >
                                <UserCheck size={15} />
                                <span>{submittingManagerChange ? "Assigning..." : "Confirm Assignment"}</span>
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* MODAL 3: TRANSFER DEPARTMENT */}
            {transferringEmp && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                <div style={{
                                    width: "40px",
                                    height: "40px",
                                    borderRadius: "12px",
                                    background: "rgba(52, 211, 153, 0.15)",
                                    border: "1px solid rgba(52, 211, 153, 0.3)",
                                    display: "grid",
                                    placeItems: "center"
                                }}>
                                    <Building2 size={20} style={{ color: "#34d399" }} />
                                </div>
                                <div>
                                    <h2 style={{ fontSize: "1.15rem", fontWeight: "700", color: "var(--text-main)", margin: 0 }}>
                                        Transfer Department
                                    </h2>
                                    <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "2px 0 0" }}>
                                        Relocate {transferringEmp.name} to another department
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setTransferringEmp(null)}
                                className="btn-close"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div style={{ marginBottom: "16px" }}>
                            <label className="form-label">Target Department *</label>
                            <select
                                value={selectedDeptId}
                                onChange={(e) => setSelectedDeptId(e.target.value)}
                                className="form-input"
                                style={{ width: "100%" }}
                            >
                                <option value="">-- Choose Target Department --</option>
                                {departments.map((d) => (
                                    <option key={d.id} value={d.id}>
                                        {d.name}
                                    </option>
                                ))}
                            </select>
                            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px", display: "block" }}>
                                Current: {transferringEmp.departmentName || `Dept #${transferringEmp.departmentId ?? "N/A"}`}
                            </span>
                        </div>

                        <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setTransferringEmp(null)}
                                disabled={submittingDeptTransfer}
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={handleConfirmDeptTransfer}
                                disabled={submittingDeptTransfer || !selectedDeptId}
                                style={{ display: "flex", alignItems: "center", gap: "6px" }}
                            >
                                <Building2 size={15} />
                                <span>{submittingDeptTransfer ? "Transferring..." : "Confirm Transfer"}</span>
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}