"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getHrAnalytics, HrAnalytics } from "@/services/analyticsService";
import { GoalStatusDonut, DepartmentBarChart } from "@/components/AnalyticsCharts";

export default function HRDashboard() {
    const [data, setData] = useState<HrAnalytics | null>(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);
        setError("");

        getHrAnalytics()
            .then(setData)
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load executive analytics"))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div style={{ padding: "60px 0", textAlign: "center", color: "var(--text-muted)" }}>
                <div style={{ fontSize: "2rem", marginBottom: "12px" }}>⏳</div>
                <div style={{ fontWeight: "600" }}>Loading HR Executive Control Center...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="alert alert-error">
                <span>⚠️</span>
                <span>{error}</span>
            </div>
        );
    }

    return (
        <section>
            <div className="page-header" style={{ marginBottom: "20px" }}>
                <div>
                    <h1 className="page-title">Executive Operations & Analytics</h1>
                    <p className="page-subtitle">Organization-wide workforce oversight, performance cycles, and goal achievement.</p>
                </div>
            </div>

            {/* ACTIVE CYCLE BANNER */}
            {data?.activeCycleId ? (
                <div className="cycle-banner" style={{ marginBottom: "24px" }}>
                    <div className="cycle-banner-content">
                        <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                            <span className="cycle-banner-pill">ACTIVE CYCLE</span>
                            <span style={{ fontSize: "0.85rem", opacity: 0.9 }}>Cycle ID: #{data.activeCycleId}</span>
                        </div>
                        <h2 style={{ fontSize: "1.35rem", fontWeight: "700", margin: "4px 0" }}>{data.activeCycleName}</h2>
                        <p style={{ margin: 0, fontSize: "0.85rem", opacity: 0.9 }}>
                            🗓️ Active Timeline: <strong>{data.activeCycleStartDate}</strong> to <strong>{data.activeCycleEndDate}</strong>
                        </p>
                    </div>
                    <Link href="/hr/cycles" className="btn btn-secondary" style={{ fontWeight: "700" }}>
                        Manage Cycles →
                    </Link>
                </div>
            ) : (
                <div className="alert alert-info" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px", marginBottom: "24px" }}>
                    <div>
                        <strong>No Active Performance Cycle Published</strong>
                        <div style={{ fontSize: "0.85rem", marginTop: "2px" }}>
                            Managers cannot allocate employee goals until an active review cycle is started.
                        </div>
                    </div>
                    <Link href="/hr/cycles" className="btn btn-primary btn-sm">
                        Create / Launch Cycle
                    </Link>
                </div>
            )}

            {/* KEY METRICS GRID */}
            <div className="stats-grid" style={{ marginBottom: "24px" }}>
                <div className="stat-card">
                    <div className="stat-label">Total Workforce</div>
                    <div className="stat-value">{data?.totalEmployees ?? 0}</div>
                    <div className="stat-desc">Permanent registered employee identities</div>
                </div>

                <div className="stat-card emerald">
                    <div className="stat-label">People Managers</div>
                    <div className="stat-value">{data?.totalManagers ?? 0}</div>
                    <div className="stat-desc">Managers with direct reports</div>
                </div>

                <div className="stat-card amber">
                    <div className="stat-label">Active Linkages</div>
                    <div className="stat-value">{data?.activeAssignments ?? 0}</div>
                    <div className="stat-desc">Employees assigned to managers</div>
                </div>

                <div className="stat-card purple">
                    <div className="stat-label">Cycle Completion Rate</div>
                    <div className="stat-value">{data?.completionRate ?? 0}%</div>
                    <div className="stat-desc">
                        {data?.completedGoals ?? 0} of {data?.totalGoals ?? 0} goals achieved
                    </div>
                </div>
            </div>

            {/* VISUAL ANALYTICS CHARTS SECTION */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "20px", marginBottom: "28px" }}>
                <GoalStatusDonut
                    statusMap={data?.goalsByStatus || {}}
                    title="Organization Goal Status Breakdown"
                />
                <DepartmentBarChart
                    departments={data?.departmentMetrics || []}
                />
            </div>

            {/* ADMINISTRATIVE WORKFLOWS */}
            <div className="card">
                <div className="card-header" style={{ marginBottom: "16px" }}>
                    <h2 className="card-title" style={{ fontSize: "1.1rem" }}>⚡ Administrative Workflows</h2>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "16px" }}>
                    <Link href="/hr/employees" className="card" style={{ padding: "20px", textDecoration: "none", border: "1px solid var(--border)", transition: "all 0.15s ease" }}>
                        <div style={{ fontSize: "1.75rem", marginBottom: "8px" }}>👥</div>
                        <div style={{ fontWeight: "700", color: "var(--text-main)", marginBottom: "4px" }}>People & Promotion</div>
                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>Search directory, promote employees to manager</div>
                    </Link>

                    <Link href="/hr/assignments" className="card" style={{ padding: "20px", textDecoration: "none", border: "1px solid var(--border)", transition: "all 0.15s ease" }}>
                        <div style={{ fontSize: "1.75rem", marginBottom: "8px" }}>🔗</div>
                        <div style={{ fontWeight: "700", color: "var(--text-main)", marginBottom: "4px" }}>Manager Linkages</div>
                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>Assign team members to reporting managers</div>
                    </Link>

                    <Link href="/hr/cycles" className="card" style={{ padding: "20px", textDecoration: "none", border: "1px solid var(--border)", transition: "all 0.15s ease" }}>
                        <div style={{ fontSize: "1.75rem", marginBottom: "8px" }}>🔄</div>
                        <div style={{ fontWeight: "700", color: "var(--text-main)", marginBottom: "4px" }}>Performance Cycles</div>
                        <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>Configure timeline, launch or close cycles</div>
                    </Link>
                </div>
            </div>
        </section>
    );
}