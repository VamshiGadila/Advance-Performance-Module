"use client";

import React from "react";
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid
} from "recharts";
import { DepartmentMetric } from "@/services/analyticsService";
import { useTheme } from "@/context/ThemeContext";

const STATUS_COLORS: Record<string, string> = {
    COMPLETED: "#10b981",
    IN_PROGRESS: "#3b82f6",
    ACCEPTED: "#6366f1",
    PENDING_ACCEPTANCE: "#f59e0b",
    CANCELLED: "#ef4444"
};

const STATUS_LABELS: Record<string, string> = {
    COMPLETED: "Completed",
    IN_PROGRESS: "In Progress",
    ACCEPTED: "Accepted",
    PENDING_ACCEPTANCE: "Pending Review",
    CANCELLED: "Cancelled"
};

interface GoalStatusDonutProps {
    statusMap: Record<string, number>;
    title?: string;
}

export function GoalStatusDonut({ statusMap, title = "Goal Status Distribution" }: GoalStatusDonutProps) {
    const { theme } = useTheme();
    const isDark = theme === "dark";

    const data = Object.entries(statusMap || {})
        .filter(([_, count]) => count > 0)
        .map(([status, count]) => ({
            name: STATUS_LABELS[status] || status,
            value: count,
            rawStatus: status,
            color: STATUS_COLORS[status] || "#94a3b8"
        }));

    const total = data.reduce((acc, curr) => acc + curr.value, 0);

    if (total === 0) {
        return (
            <div className="card" style={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", padding: "32px", minHeight: "260px" }}>
                <div style={{ fontSize: "1.75rem", marginBottom: "8px" }}>🎯</div>
                <div style={{ fontWeight: "700", color: "var(--text-main)", marginBottom: "4px" }}>{title}</div>
                <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>No goals recorded for this cycle yet</div>
            </div>
        );
    }

    return (
        <div className="card" style={{ height: "100%", padding: "22px", display: "flex", flexDirection: "column" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                <div>
                    <h3 style={{ fontSize: "0.95rem", fontWeight: "700", color: "var(--text-main)" }}>{title}</h3>
                    <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>Total Goals: <strong>{total}</strong></div>
                </div>
            </div>

            <div style={{ height: "200px", width: "100%" }}>
                <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                        <Pie
                            data={data}
                            innerRadius={55}
                            outerRadius={80}
                            paddingAngle={4}
                            dataKey="value"
                        >
                            {data.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={entry.color} stroke="transparent" />
                            ))}
                        </Pie>
                        <Tooltip
                            contentStyle={{
                                background: isDark ? "#131b2e" : "#ffffff",
                                border: isDark ? "1px solid rgba(255, 255, 255, 0.12)" : "1px solid #e2e8f0",
                                borderRadius: "10px",
                                boxShadow: isDark ? "0 10px 25px rgba(0,0,0,0.5)" : "0 4px 16px rgba(0,0,0,0.08)",
                                fontSize: "0.8rem",
                                padding: "8px 12px",
                                color: isDark ? "#ffffff" : "#0f172a"
                            }}
                            itemStyle={{ color: isDark ? "#ffffff" : "#0f172a" }}
                        />
                    </PieChart>
                </ResponsiveContainer>
            </div>

            {/* STATUS LEGEND */}
            <div style={{ display: "flex", flexWrap: "wrap", gap: "10px", marginTop: "12px", justifyContent: "center" }}>
                {data.map((item) => (
                    <div key={item.name} style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.775rem" }}>
                        <div style={{ width: "8px", height: "8px", borderRadius: "50%", background: item.color }} />
                        <span style={{ color: "var(--text-secondary)" }}>{item.name}:</span>
                        <strong style={{ color: "var(--text-main)" }}>{item.value}</strong>
                    </div>
                ))}
            </div>
        </div>
    );
}

interface DepartmentBarChartProps {
    departments: DepartmentMetric[];
}

export function DepartmentBarChart({ departments }: DepartmentBarChartProps) {
    const { theme } = useTheme();
    const isDark = theme === "dark";

    const data = (departments || []).map((d) => ({
        name: d.departmentName,
        rate: d.goalCompletionRate,
        employees: d.employeeCount
    }));

    if (data.length === 0) {
        return (
            <div className="card" style={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", padding: "32px", minHeight: "260px" }}>
                <div style={{ fontSize: "1.75rem", marginBottom: "8px" }}>🏢</div>
                <div style={{ fontWeight: "700", color: "var(--text-main)", marginBottom: "4px" }}>Department Performance</div>
                <div style={{ fontSize: "0.825rem", color: "var(--text-muted)" }}>No department metrics available</div>
            </div>
        );
    }

    return (
        <div className="card" style={{ height: "100%", padding: "22px", display: "flex", flexDirection: "column" }}>
            <div style={{ marginBottom: "16px" }}>
                <h3 style={{ fontSize: "0.95rem", fontWeight: "700", color: "var(--text-main)" }}>Department Goal Completion</h3>
                <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>Average goal completion rate across organizational units</div>
            </div>

            <div style={{ height: "220px", width: "100%" }}>
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)"} horizontal={false} />
                        <XAxis type="number" domain={[0, 100]} unit="%" tick={{ fill: isDark ? "#94a3b8" : "#64748b", fontSize: 11 }} />
                        <YAxis dataKey="name" type="category" tick={{ fill: isDark ? "#cbd5e1" : "#334155", fontSize: 11 }} width={100} />
                        <Tooltip
                            formatter={(val) => [`${val}%`, "Completion Rate"]}
                            contentStyle={{
                                background: isDark ? "#131b2e" : "#ffffff",
                                border: isDark ? "1px solid rgba(255, 255, 255, 0.12)" : "1px solid #e2e8f0",
                                borderRadius: "10px",
                                boxShadow: isDark ? "0 10px 25px rgba(0,0,0,0.5)" : "0 4px 16px rgba(0,0,0,0.08)",
                                fontSize: "0.8rem",
                                padding: "8px 12px",
                                color: isDark ? "#ffffff" : "#0f172a"
                            }}
                            itemStyle={{ color: isDark ? "#ffffff" : "#0f172a" }}
                        />
                        <Bar dataKey="rate" fill="#4f46e5" radius={[0, 6, 6, 0]} />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
