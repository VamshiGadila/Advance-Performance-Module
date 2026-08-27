"use client";

import { useEffect, useState } from "react";
import { launchCycle, closeCycle, createCycle, searchCycles, Cycle } from "@/services/hrService";
import Pagination from "@/components/Pagination";

export default function CyclesPage() {
    const [cycles, setCycles] = useState<Cycle[]>([]);
    const [searchName, setSearchName] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [name, setName] = useState("");
    const [startDate, setStartDate] = useState("2026-01-01");
    const [endDate, setEndDate] = useState("2026-12-31");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [actionId, setActionId] = useState<number | null>(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const loadCycles = () => {
        setLoading(true);
        setError("");

        searchCycles({
            name: searchName || undefined,
            status: statusFilter || undefined,
            page,
            size,
            sortBy: "startDate",
            direction: "desc"
        })
            .then((res) => {
                setCycles(res.content || []);
                setTotalPages(res.totalPages || 0);
                setTotalElements(res.totalElements || 0);
            })
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load performance cycles"))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        loadCycles();
    }, [searchName, statusFilter, page, size]);

    async function handleCreate(e: React.FormEvent) {
        e.preventDefault();
        setError("");
        setSuccess("");
        setSubmitting(true);

        try {
            const created = await createCycle({
                name: name.trim(),
                startDate,
                endDate
            });
            setSuccess(`🎉 Performance Cycle "${created.name}" created successfully as DRAFT!`);
            setName("");
            loadCycles();
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to create cycle");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleLaunch(id: number, cycleName: string) {
        setError("");
        setSuccess("");
        setActionId(id);

        try {
            await launchCycle(id);
            setSuccess(`🚀 Performance Cycle "${cycleName}" is now ACTIVE! Previous cycles were closed.`);
            loadCycles();
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to launch cycle");
        } finally {
            setActionId(null);
        }
    }

    async function handleClose(id: number, cycleName: string) {
        setError("");
        setSuccess("");
        setActionId(id);

        try {
            await closeCycle(id);
            setSuccess(`🔒 Performance Cycle "${cycleName}" has been CLOSED.`);
            loadCycles();
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to close cycle");
        } finally {
            setActionId(null);
        }
    }

    const activeCycle = cycles.find((c) => c.status === "ACTIVE");

    return (
        <section>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Performance Review Cycles</h1>
                    <p className="page-subtitle">
                        Configure organizational review cycles, review timelines, and activate cycles to enable goal creation.
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

            {activeCycle && (
                <div className="cycle-banner">
                    <div className="cycle-banner-content">
                        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                            <span className="cycle-banner-pill">CURRENT ACTIVE CYCLE</span>
                            <span style={{ fontSize: "0.85rem", opacity: 0.9 }}>ID: #{activeCycle.id}</span>
                        </div>
                        <h2>{activeCycle.name}</h2>
                        <p>
                            🗓️ Active Review Timeline: <strong>{activeCycle.startDate}</strong> through <strong>{activeCycle.endDate}</strong>
                        </p>
                    </div>
                </div>
            )}

            <div style={{ display: "grid", gridTemplateColumns: "1.6fr 1fr", gap: "24px", alignItems: "start" }}>
                <div className="card">
                    {/* SEARCH & FILTER CONTROLS */}
                    <div style={{ display: "flex", gap: "12px", marginBottom: "18px", flexWrap: "wrap", alignItems: "center" }}>
                        <div style={{ flex: "1 1 200px" }}>
                            <input
                                type="text"
                                className="form-input"
                                placeholder="🔍 Search cycle name..."
                                value={searchName}
                                onChange={(e) => {
                                    setSearchName(e.target.value);
                                    setPage(0);
                                }}
                            />
                        </div>

                        <div className="tabs-container">
                            {[
                                { label: "All Cycles", value: "" },
                                { label: "Active", value: "ACTIVE" },
                                { label: "Draft", value: "DRAFT" },
                                { label: "Closed", value: "CLOSED" }
                            ].map((tab) => (
                                <button
                                    key={tab.value}
                                    type="button"
                                    className={`tab-btn ${statusFilter === tab.value ? "active" : ""}`}
                                    onClick={() => {
                                        setStatusFilter(tab.value);
                                        setPage(0);
                                    }}
                                >
                                    {tab.label}
                                </button>
                            ))}
                        </div>

                        {(searchName || statusFilter) && (
                            <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => {
                                    setSearchName("");
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
                            Loading cycles...
                        </div>
                    ) : cycles.length === 0 ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            No performance cycles found matching your criteria.
                        </div>
                    ) : (
                        <>
                            <div className="table-container">
                                <table className="modern-table">
                                    <thead>
                                        <tr>
                                            <th>Cycle Name</th>
                                            <th>Period</th>
                                            <th>Status</th>
                                            <th style={{ textAlign: "right" }}>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {cycles.map((c) => {
                                            const isProcessing = actionId === c.id;
                                            return (
                                                <tr key={c.id}>
                                                    <td>
                                                        <div style={{ fontWeight: "700", color: "var(--text-main)" }}>
                                                            {c.name}
                                                        </div>
                                                        <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontFamily: "monospace" }}>
                                                            ID: #{c.id}
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <div style={{ fontSize: "0.85rem", color: "var(--text-main)" }}>
                                                            {c.startDate} → {c.endDate}
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <span
                                                            className={`pill ${
                                                                c.status === "ACTIVE"
                                                                    ? "pill-active"
                                                                    : c.status === "CLOSED"
                                                                        ? "pill-closed"
                                                                        : "pill-draft"
                                                                }`}
                                                        >
                                                            {c.status === "ACTIVE" && "🟢 "}
                                                            {c.status === "CLOSED" && "🔴 "}
                                                            {c.status === "DRAFT" && "⚪ "}
                                                            {c.status}
                                                        </span>
                                                    </td>
                                                    <td style={{ textAlign: "right" }}>
                                                        <div style={{ display: "inline-flex", gap: "6px" }}>
                                                            {c.status === "DRAFT" && (
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-primary btn-sm"
                                                                    onClick={() => handleLaunch(c.id, c.name)}
                                                                    disabled={isProcessing}
                                                                >
                                                                    {isProcessing ? "..." : "🚀 Launch"}
                                                                </button>
                                                            )}
                                                            {c.status === "ACTIVE" && (
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-danger btn-sm"
                                                                    onClick={() => handleClose(c.id, c.name)}
                                                                    disabled={isProcessing}
                                                                >
                                                                    {isProcessing ? "..." : "🔒 Close"}
                                                                </button>
                                                            )}
                                                            {c.status === "CLOSED" && (
                                                                <span style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>
                                                                    Archived
                                                                </span>
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

                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">➕ Create New Cycle</h2>
                    </div>

                    <form onSubmit={handleCreate} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div className="form-group">
                            <label className="form-label">Cycle Title</label>
                            <input
                                type="text"
                                className="form-input"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="e.g. FY26 Annual Appraisal"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Start Date</label>
                            <input
                                type="date"
                                className="form-input"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">End Date</label>
                            <input
                                type="date"
                                className="form-input"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            style={{ width: "100%", padding: "12px", marginTop: "8px" }}
                            disabled={submitting || !name.trim()}
                        >
                            {submitting ? "Creating Cycle..." : "Save as Draft Cycle"}
                        </button>
                    </form>
                </div>
            </div>
        </section>
    );
}