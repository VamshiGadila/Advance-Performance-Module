"use client";

import React from "react";

interface PaginationProps {
    page: number; // 0-indexed
    totalPages: number;
    totalElements: number;
    size: number;
    onPageChange: (newPage: number) => void;
    onSizeChange?: (newSize: number) => void;
    sizeOptions?: number[];
}

export default function Pagination({
    page,
    totalPages,
    totalElements,
    size,
    onPageChange,
    onSizeChange,
    sizeOptions = [5, 10, 20, 50]
}: PaginationProps) {
    if (totalElements === 0) {
        return null;
    }

    const currentPage = page + 1; // 1-indexed for display
    const startItem = page * size + 1;
    const endItem = Math.min((page + 1) * size, totalElements);

    // Calculate page range to show with ellipsis
    const getPageNumbers = () => {
        const pages: (number | string)[] = [];
        const maxPagesToShow = 5;

        if (totalPages <= maxPagesToShow + 2) {
            for (let i = 1; i <= totalPages; i++) {
                pages.push(i);
            }
        } else {
            pages.push(1);

            let start = Math.max(2, currentPage - 1);
            let end = Math.min(totalPages - 1, currentPage + 1);

            if (currentPage <= 3) {
                end = 4;
            } else if (currentPage >= totalPages - 2) {
                start = totalPages - 3;
            }

            if (start > 2) {
                pages.push("...");
            }

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }

            if (end < totalPages - 1) {
                pages.push("...");
            }

            pages.push(totalPages);
        }

        return pages;
    };

    return (
        <div
            style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: "14px",
                padding: "18px 0 6px",
                marginTop: "16px",
                borderTop: "1px solid var(--border)",
                fontSize: "0.875rem",
                color: "var(--text-secondary)"
            }}
        >
            {/* RANGE DISPLAY */}
            <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                <span>Showing</span>
                <strong style={{ color: "var(--text-main)" }}>{startItem}</strong>
                <span>to</span>
                <strong style={{ color: "var(--text-main)" }}>{endItem}</strong>
                <span>of</span>
                <strong style={{ color: "var(--primary)", fontWeight: "700" }}>{totalElements}</strong>
                <span>results</span>
            </div>

            {/* CONTROLS */}
            <div style={{ display: "flex", alignItems: "center", gap: "14px", flexWrap: "wrap" }}>
                {/* PAGE SIZE SELECTOR */}
                {onSizeChange && (
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <span style={{ color: "var(--text-muted)", fontSize: "0.825rem", fontWeight: "500" }}>Rows per page:</span>
                        <select
                            className="form-select"
                            value={size}
                            onChange={(e) => onSizeChange(Number(e.target.value))}
                            style={{
                                padding: "6px 28px 6px 10px",
                                fontSize: "0.825rem",
                                borderRadius: "8px",
                                minWidth: "70px",
                                fontWeight: "600"
                            }}
                        >
                            {sizeOptions.map((opt) => (
                                <option key={opt} value={opt}>
                                    {opt}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                {/* PAGINATION BUTTONS */}
                <div style={{ display: "flex", alignItems: "center", gap: "5px" }}>
                    <button
                        type="button"
                        onClick={() => onPageChange(page - 1)}
                        disabled={page <= 0}
                        style={{
                            padding: "6px 14px",
                            border: "1px solid var(--border)",
                            borderRadius: "8px",
                            background: page <= 0 ? "var(--bg-subtle)" : "var(--card-bg)",
                            color: page <= 0 ? "var(--text-muted)" : "var(--text-main)",
                            cursor: page <= 0 ? "not-allowed" : "pointer",
                            fontWeight: "600",
                            fontSize: "0.825rem",
                            boxShadow: page > 0 ? "0 1px 3px rgba(0,0,0,0.04)" : "none",
                            transition: "all 0.15s ease"
                        }}
                    >
                        ‹ Prev
                    </button>

                    {getPageNumbers().map((p, idx) => {
                        if (typeof p === "string") {
                            return (
                                <span
                                    key={`ellipsis-${idx}`}
                                    style={{ padding: "0 6px", color: "var(--text-muted)", fontWeight: "600" }}
                                >
                                    …
                                </span>
                            );
                        }

                        const isActive = p === currentPage;
                        return (
                            <button
                                key={`page-${p}`}
                                type="button"
                                onClick={() => onPageChange(p - 1)}
                                style={{
                                    minWidth: "34px",
                                    height: "34px",
                                    padding: "0 6px",
                                    display: "grid",
                                    placeItems: "center",
                                    border: isActive ? "1px solid transparent" : "1px solid var(--border)",
                                    borderRadius: "8px",
                                    background: isActive
                                        ? "linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)"
                                        : "var(--card-bg)",
                                    color: isActive ? "#ffffff" : "var(--text-main)",
                                    cursor: "pointer",
                                    fontWeight: isActive ? "700" : "600",
                                    fontSize: "0.825rem",
                                    boxShadow: isActive
                                        ? "0 2px 8px rgba(79, 70, 229, 0.35)"
                                        : "0 1px 3px rgba(0,0,0,0.04)",
                                    transition: "all 0.15s ease"
                                }}
                            >
                                {p}
                            </button>
                        );
                    })}

                    <button
                        type="button"
                        onClick={() => onPageChange(page + 1)}
                        disabled={page >= totalPages - 1}
                        style={{
                            padding: "6px 14px",
                            border: "1px solid var(--border)",
                            borderRadius: "8px",
                            background: page >= totalPages - 1 ? "var(--bg-subtle)" : "var(--card-bg)",
                            color: page >= totalPages - 1 ? "var(--text-muted)" : "var(--text-main)",
                            cursor: page >= totalPages - 1 ? "not-allowed" : "pointer",
                            fontWeight: "600",
                            fontSize: "0.825rem",
                            boxShadow: page >= totalPages - 1 ? "none" : "0 1px 3px rgba(0,0,0,0.04)",
                            transition: "all 0.15s ease"
                        }}
                    >
                        Next ›
                    </button>
                </div>
            </div>
        </div>
    );
}
