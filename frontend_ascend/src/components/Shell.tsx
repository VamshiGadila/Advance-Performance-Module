"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AuthUser, getUser, logout } from "@/lib/auth";
import { ThemeToggle } from "@/context/ThemeContext";

interface ShellProps {
    children: React.ReactNode;
}

export default function Shell({ children }: ShellProps) {
    const pathname = usePathname();
    const router = useRouter();

    const [user, setUser] = useState<AuthUser | null>(null);
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
        const currentUser = getUser();
        setUser(currentUser);
    }, []);

    const isPublicPage =
        pathname === "/login" ||
        pathname === "/signup";

    if (!mounted || isPublicPage) {
        return <>{children}</>;
    }

    if (!user) {
        return <>{children}</>;
    }

    interface NavItem {
        href: string;
        label: string;
        icon: string;
    }

    let links: NavItem[] = [];

    switch (user.role) {
        case "HR":
            links = [
                { href: "/hr", label: "Dashboard", icon: "📊" },
                { href: "/hr/employees", label: "Employees & Managers", icon: "👥" },
                { href: "/hr/assignments", label: "Manager Assignments", icon: "🔗" },
                { href: "/hr/cycles", label: "Performance Cycles", icon: "🔄" },
            ];
            break;

        case "MANAGER":
            links = [
                { href: "/manager", label: "My Team", icon: "👥" },
            ];
            break;

        case "EMPLOYEE":
            links = [
                { href: "/employee", label: "My Goals", icon: "🎯" },
            ];
            break;

        default:
            links = [];
    }

    const handleLogout = () => {
        logout();
        setUser(null);
        router.replace("/login");
    };

    const getInitials = (name: string) => {
        return name
            .split(" ")
            .map((n) => n[0])
            .join("")
            .substring(0, 2)
            .toUpperCase();
    };

    const getRoleClass = (role: string) => {
        switch (role) {
            case "HR":
                return "role-badge hr";
            case "MANAGER":
                return "role-badge manager";
            default:
                return "role-badge employee";
        }
    };

    return (
        <div className="shell">
            <aside className="sidebar">
                <div className="brand-section">
                    <div className="brand-logo">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                        </svg>
                    </div>
                    <div>
                        <div className="brand-title">ASCEND</div>
                        <div className="brand-sub">Performance Suite</div>
                    </div>
                </div>

                <nav className="sidebar-nav">
                    {links.map((item) => {
                        const isActive =
                            pathname === item.href ||
                            (item.href !== "/hr" && pathname.startsWith(`${item.href}/`));

                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={isActive ? "nav-link active" : "nav-link"}
                            >
                                <span style={{ fontSize: "1.15rem" }}>{item.icon}</span>
                                <span>{item.label}</span>
                            </Link>
                        );
                    })}
                </nav>

                <div className="sidebar-footer">
                    <div
                        style={{
                            padding: "12px 14px",
                            background: "rgba(255, 255, 255, 0.03)",
                            border: "1px solid var(--border)",
                            borderRadius: "var(--radius-md)",
                            display: "flex",
                            alignItems: "center",
                            gap: "10px"
                        }}
                    >
                        <div
                            style={{
                                width: "8px",
                                height: "8px",
                                borderRadius: "50%",
                                background: "#10b981",
                                boxShadow: "0 0 8px #10b981"
                            }}
                        />
                        <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                            Logged in: <strong style={{ color: "var(--text-main)" }}>{user.employeeCode}</strong>
                        </div>
                    </div>
                </div>
            </aside>

            <div className="main-wrapper">
                <header className="app-header">
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <span style={{ fontSize: "0.85rem", color: "var(--text-muted)", fontWeight: "600", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                            ASCEND Workspace
                        </span>
                        <span style={{ color: "var(--border)" }}>/</span>
                        <span style={{ fontSize: "0.85rem", color: "var(--text-secondary)", fontWeight: "600" }}>
                            {user.role === "HR" ? "Organization Admin" : user.role === "MANAGER" ? "Team Performance" : "Personal Goals"}
                        </span>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                        <div className="header-user">
                            <div className="user-avatar">{getInitials(user.name)}</div>
                            <div className="user-meta" style={{ textAlign: "left" }}>
                                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                                    <span className="user-name" style={{ fontWeight: "700" }}>{user.name}</span>
                                    <span style={{ fontFamily: "monospace", fontSize: "0.8rem", color: "var(--primary)", fontWeight: "700" }}>
                                        {user.employeeCode}
                                    </span>
                                </div>
                                <span className={getRoleClass(user.role)} style={{ alignSelf: "flex-start", marginTop: "2px" }}>
                                    [{user.role}]
                                </span>
                            </div>
                        </div>

                        <ThemeToggle />

                        <button
                            type="button"
                            className="btn-logout"
                            onClick={handleLogout}
                            title="Sign out of Ascend"
                        >
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                                <polyline points="16 17 21 12 16 7" />
                                <line x1="21" y1="12" x2="9" y2="12" />
                            </svg>
                            <span>Sign out</span>
                        </button>
                    </div>
                </header>

                <main className="page-container">
                    {children}
                </main>
            </div>
        </div>
    );
}