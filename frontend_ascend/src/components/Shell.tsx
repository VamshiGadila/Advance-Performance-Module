"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AuthUser, getUser, logout } from "@/lib/auth";
import { ThemeToggle } from "@/context/ThemeContext";
import { getMyProfile, updateMyProfile, UserProfile, UpdateProfileRequest } from "@/services/authService";

import {
    LayoutDashboard,
    Users,
    Network,
    CalendarRange,
    Target,
    UserCircle,
    UserCheck,
    Shield,
    Sparkles,
    Briefcase,
    X,
    CheckCircle2,
    AlertCircle,
    Lock,
    Key,
    ChevronDown,
    ChevronUp
} from "lucide-react";

interface ShellProps {
    children: React.ReactNode;
}

export default function Shell({ children }: ShellProps) {
    const pathname = usePathname();
    const router = useRouter();

    const [user, setUser] = useState<AuthUser | null>(null);
    const [mounted, setMounted] = useState(false);

    // Profile Modal State
    const [profileModalOpen, setProfileModalOpen] = useState(false);
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loadingProfile, setLoadingProfile] = useState(false);
    const [nameInput, setNameInput] = useState("");
    const [skillInput, setSkillInput] = useState("");
    const [domainInput, setDomainInput] = useState("");
    const [locationInput, setLocationInput] = useState("");
    const [expYearsInput, setExpYearsInput] = useState<number | string>(0);
    
    // Credential Update State
    const [showPasswordChange, setShowPasswordChange] = useState(false);
    const [currentPasswordInput, setCurrentPasswordInput] = useState("");
    const [newPasswordInput, setNewPasswordInput] = useState("");
    const [confirmPasswordInput, setConfirmPasswordInput] = useState("");

    const [savingProfile, setSavingProfile] = useState(false);
    const [profileError, setProfileError] = useState("");
    const [profileSuccess, setProfileSuccess] = useState("");

    const handleOpenProfile = () => {
        setProfileModalOpen(true);
        setLoadingProfile(true);
        setProfileError("");
        setProfileSuccess("");
        setShowPasswordChange(false);
        setCurrentPasswordInput("");
        setNewPasswordInput("");
        setConfirmPasswordInput("");

        getMyProfile()
            .then((data) => {
                setProfile(data);
                setNameInput(data.name || "");
                setSkillInput(data.skill || "");
                setDomainInput(data.domain || "");
                setLocationInput(data.location || "");
                setExpYearsInput(data.experienceYears ?? 0);
            })
            .catch((err) => setProfileError(err.message || "Failed to load profile"))
            .finally(() => setLoadingProfile(false));
    };

    const handleSaveProfile = async (e: React.FormEvent) => {
        e.preventDefault();
        setSavingProfile(true);
        setProfileError("");
        setProfileSuccess("");

        if (!nameInput.trim()) {
            setProfileError("Full Name cannot be empty.");
            setSavingProfile(false);
            return;
        }

        if (showPasswordChange && newPasswordInput) {
            if (newPasswordInput.length < 6) {
                setProfileError("New password must be at least 6 characters long.");
                setSavingProfile(false);
                return;
            }
            if (newPasswordInput !== confirmPasswordInput) {
                setProfileError("New password and confirm password do not match.");
                setSavingProfile(false);
                return;
            }
        }

        try {
            const payload: UpdateProfileRequest = {
                name: nameInput.trim(),
                skill: skillInput.trim(),
                domain: domainInput.trim(),
                location: locationInput.trim(),
                experienceYears: Number(expYearsInput),
                ...(showPasswordChange && newPasswordInput ? {
                    currentPassword: currentPasswordInput,
                    newPassword: newPasswordInput,
                    confirmPassword: confirmPasswordInput
                } : {})
            };

            const updated = await updateMyProfile(payload);
            setProfile(updated);
            
            // Sync live state & localStorage so headers & avatar update immediately
            if (user && updated.name) {
                const updatedUser: AuthUser = {
                    ...user,
                    name: updated.name
                };
                setUser(updatedUser);
                try {
                    localStorage.setItem("ascend_user", JSON.stringify(updatedUser));
                } catch {}
            }

            setProfileSuccess("Profile details and credentials updated successfully!");
            setShowPasswordChange(false);
            setCurrentPasswordInput("");
            setNewPasswordInput("");
            setConfirmPasswordInput("");

            setTimeout(() => {
                setProfileModalOpen(false);
            }, 1200);
        } catch (err: any) {
            setProfileError(err.message || "Failed to update profile");
        } finally {
            setSavingProfile(false);
        }
    };

    useEffect(() => {
        setMounted(true);
        const currentUser = getUser();
        setUser(currentUser);
    }, []);

    const isPublicPage =
        pathname === "/login" ||
        pathname === "/signup" ||
        pathname === "/forgot-password";

    if (!mounted || isPublicPage) {
        return <>{children}</>;
    }

    if (!user) {
        return <>{children}</>;
    }

    interface NavItem {
        href: string;
        label: string;
        icon: React.ReactNode;
    }

    let links: NavItem[] = [];

    switch (user.role) {
        case "HR":
            links = [
                { href: "/hr", label: "Dashboard", icon: <LayoutDashboard size={18} /> },
                { href: "/hr/employees", label: "Employees & Managers", icon: <Users size={18} /> },
                { href: "/hr/assignments", label: "Manager Assignments", icon: <Network size={18} /> },
                { href: "/hr/cycles", label: "Performance Cycles", icon: <CalendarRange size={18} /> },
            ];
            break;

        case "MANAGER":
            links = [
                { href: "/manager", label: "My Team", icon: <Users size={18} /> },
            ];
            break;

        case "EMPLOYEE":
            links = [
                { href: "/employee", label: "My Goals", icon: <Target size={18} /> },
            ];
            break;

        default:
            links = [];
    }

    const handleLogout = () => {
        logout();
        router.push("/login");
    };

    const getInitials = (name: string) => {
        if (!name) return "US";
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

                        <button
                            type="button"
                            onClick={handleOpenProfile}
                            className="btn btn-secondary btn-sm"
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "6px",
                                fontSize: "0.78rem",
                                padding: "5px 12px",
                                borderRadius: "8px",
                                borderColor: user.role === "MANAGER" ? "rgba(139, 92, 246, 0.4)" : user.role === "HR" ? "rgba(16, 185, 129, 0.4)" : "rgba(99, 102, 241, 0.4)",
                                color: "var(--text-main)",
                                fontWeight: "600"
                            }}
                            title="View and edit your personal profile, name, credentials, and skills"
                        >
                            <UserCircle size={15} />
                            <span>My Profile</span>
                        </button>

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

                {/* GLOBAL MY PROFILE MODAL */}
                {profileModalOpen && (
                    <div className="modal-backdrop">
                        <div className="modal-card" style={{ maxWidth: "600px", maxHeight: "90vh", overflowY: "auto" }}>
                            {/* Header */}
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" }}>
                                <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                                    <div style={{
                                        width: "42px",
                                        height: "42px",
                                        borderRadius: "12px",
                                        background: user.role === "MANAGER" ? "rgba(139, 92, 246, 0.15)" : user.role === "HR" ? "rgba(16, 185, 129, 0.15)" : "rgba(99, 102, 241, 0.15)",
                                        border: `1px solid ${user.role === "MANAGER" ? "rgba(139, 92, 246, 0.3)" : user.role === "HR" ? "rgba(16, 185, 129, 0.3)" : "rgba(99, 102, 241, 0.3)"}`,
                                        display: "grid",
                                        placeItems: "center",
                                        color: user.role === "MANAGER" ? "#a78bfa" : user.role === "HR" ? "#34d399" : "#818cf8"
                                    }}>
                                        <UserCheck size={22} />
                                    </div>
                                    <div>
                                        <h2 style={{ fontSize: "1.2rem", fontWeight: "800", color: "var(--text-main)", margin: 0 }}>
                                            My Profile & Credentials
                                        </h2>
                                        <p style={{ fontSize: "0.825rem", color: "var(--text-muted)", margin: "3px 0 0" }}>
                                            Update your personal name, security credentials, and professional competencies
                                        </p>
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => setProfileModalOpen(false)}
                                    className="btn-close"
                                    title="Close modal"
                                >
                                    <X size={18} />
                                </button>
                            </div>

                            {profileError && (
                                <div className="alert-banner alert-error" style={{ marginBottom: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
                                    <AlertCircle size={16} />
                                    <span>{profileError}</span>
                                </div>
                            )}

                            {profileSuccess && (
                                <div className="alert-banner alert-success" style={{ marginBottom: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
                                    <CheckCircle2 size={16} />
                                    <span>{profileSuccess}</span>
                                </div>
                            )}

                            {loadingProfile ? (
                                <div style={{ padding: "30px", textAlign: "center", color: "var(--text-muted)" }}>
                                    Loading profile details...
                                </div>
                            ) : profile ? (
                                <form onSubmit={handleSaveProfile} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                                    
                                    {/* BASIC DETAILS & EDITABLE NAME */}
                                    <div className="card" style={{ padding: "16px", background: "var(--bg-subtle)", borderRadius: "10px" }}>
                                        <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: "12px" }}>
                                            <Shield size={13} style={{ color: "#10b981" }} />
                                            <span>Corporate Identity & Basic Details</span>
                                        </div>

                                        <div className="form-group" style={{ marginBottom: "14px" }}>
                                            <label className="form-label">Full Name *</label>
                                            <input
                                                type="text"
                                                className="form-input"
                                                required
                                                value={nameInput}
                                                onChange={(e) => setNameInput(e.target.value)}
                                                placeholder="e.g. Vamshi Gadila"
                                            />
                                            <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", marginTop: "3px", display: "block" }}>
                                                You can update your name if misspelled or changed
                                            </span>
                                        </div>

                                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>Work Email</span>
                                                <span style={{ fontWeight: "600", color: "var(--text-main)", fontSize: "0.85rem" }}>{profile.email}</span>
                                            </div>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>Permanent Serial ID</span>
                                                <span className="id-badge">#{String(profile.id).padStart(3, '0')}</span>
                                            </div>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>Employee Code</span>
                                                <span style={{
                                                    fontFamily: "monospace",
                                                    color: profile.role === "MANAGER" ? "#8b5cf6" : profile.role === "HR" ? "#10b981" : "var(--primary)",
                                                    fontWeight: "700",
                                                    fontSize: "0.85rem"
                                                }}>
                                                    {profile.employeeCode}
                                                </span>
                                            </div>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>Department</span>
                                                <span style={{ fontWeight: "600", color: "var(--text-main)", fontSize: "0.85rem" }}>
                                                    {profile.departmentName || "General"}
                                                </span>
                                            </div>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>Designation / Title</span>
                                                <span style={{ fontSize: "0.82rem", color: "#93c5fd", fontWeight: "600", display: "flex", alignItems: "center", gap: "5px" }}>
                                                    <Briefcase size={12} />
                                                    <span>{profile.designation || (profile.role === "MANAGER" ? "Engineering Manager" : profile.role === "HR" ? "HR Administrator" : "Software Engineer")}</span>
                                                </span>
                                            </div>
                                            <div>
                                                <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", display: "block" }}>
                                                    {profile.role === "EMPLOYEE" ? "Reporting Manager" : "Role Responsibility"}
                                                </span>
                                                <span style={{ fontSize: "0.82rem", color: "#c4b5fd", fontWeight: "600", display: "flex", alignItems: "center", gap: "5px" }}>
                                                    <UserCheck size={12} />
                                                    <span>
                                                        {profile.role === "EMPLOYEE"
                                                            ? (profile.managerName ? `${profile.managerName} (${profile.managerCode})` : "Unassigned")
                                                            : profile.role === "MANAGER"
                                                            ? "Department People Manager"
                                                            : "System Administrator"}
                                                    </span>
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    {/* EDITABLE PROFESSIONAL ATTRIBUTES */}
                                    <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                                        <Sparkles size={13} style={{ color: "#a855f7" }} />
                                        <span>Professional Attributes & Competencies</span>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Key Technical / Management Skills *</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            required
                                            value={skillInput}
                                            onChange={(e) => setSkillInput(e.target.value)}
                                            placeholder="e.g. Java, Spring Boot, Architecture, Team Leadership"
                                        />
                                        <span style={{ fontSize: "0.72rem", color: "var(--text-muted)", marginTop: "3px", display: "block" }}>
                                            Comma-separated list of your primary languages, tools, frameworks, and competencies
                                        </span>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Domain Specialization *</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            required
                                            value={domainInput}
                                            onChange={(e) => setDomainInput(e.target.value)}
                                            placeholder="e.g. Backend Engineering, Cloud Infrastructure, HR Operations"
                                        />
                                    </div>

                                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                        <div className="form-group">
                                            <label className="form-label">Base Location *</label>
                                            <input
                                                type="text"
                                                className="form-input"
                                                required
                                                value={locationInput}
                                                onChange={(e) => setLocationInput(e.target.value)}
                                                placeholder="e.g. Hyderabad, India"
                                            />
                                        </div>

                                        <div className="form-group">
                                            <label className="form-label">Total Experience (Years) *</label>
                                            <input
                                                type="number"
                                                className="form-input"
                                                required
                                                min={0}
                                                max={50}
                                                value={expYearsInput}
                                                onChange={(e) => setExpYearsInput(e.target.value)}
                                                placeholder="e.g. 8"
                                            />
                                        </div>
                                    </div>

                                    {/* SECURITY & CREDENTIALS SECTION */}
                                    <div className="card" style={{ padding: "14px 16px", background: "var(--bg-subtle)", borderRadius: "10px", border: "1px solid var(--border)" }}>
                                        <div
                                            onClick={() => setShowPasswordChange(!showPasswordChange)}
                                            style={{
                                                display: "flex",
                                                justifyContent: "space-between",
                                                alignItems: "center",
                                                cursor: "pointer",
                                                userSelect: "none"
                                            }}
                                        >
                                            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                                                <Lock size={15} style={{ color: "var(--primary)" }} />
                                                <span style={{ fontSize: "0.85rem", fontWeight: "700", color: "var(--text-main)" }}>
                                                    Change Account Password / Credentials
                                                </span>
                                            </div>
                                            <div style={{ color: "var(--text-muted)", display: "flex", alignItems: "center" }}>
                                                {showPasswordChange ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                            </div>
                                        </div>

                                        {showPasswordChange && (
                                            <div style={{ marginTop: "14px", paddingTop: "14px", borderTop: "1px solid var(--border)", display: "flex", flexDirection: "column", gap: "12px" }}>
                                                <div className="form-group">
                                                    <label className="form-label">Current Password (if set)</label>
                                                    <input
                                                        type="password"
                                                        className="form-input"
                                                        value={currentPasswordInput}
                                                        onChange={(e) => setCurrentPasswordInput(e.target.value)}
                                                        placeholder="Enter current password"
                                                    />
                                                </div>

                                                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                                    <div className="form-group">
                                                        <label className="form-label">New Password</label>
                                                        <input
                                                            type="password"
                                                            minLength={6}
                                                            className="form-input"
                                                            value={newPasswordInput}
                                                            onChange={(e) => setNewPasswordInput(e.target.value)}
                                                            placeholder="Min 6 characters"
                                                        />
                                                    </div>
                                                    <div className="form-group">
                                                        <label className="form-label">Confirm New Password</label>
                                                        <input
                                                            type="password"
                                                            minLength={6}
                                                            className="form-input"
                                                            value={confirmPasswordInput}
                                                            onChange={(e) => setConfirmPasswordInput(e.target.value)}
                                                            placeholder="Re-type new password"
                                                        />
                                                    </div>
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    {/* Action Buttons */}
                                    <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                                        <button
                                            type="button"
                                            className="btn btn-secondary"
                                            onClick={() => setProfileModalOpen(false)}
                                            disabled={savingProfile}
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            type="submit"
                                            className="btn btn-primary"
                                            disabled={savingProfile}
                                            style={{ display: "flex", alignItems: "center", gap: "6px" }}
                                        >
                                            <CheckCircle2 size={15} />
                                            <span>{savingProfile ? "Saving Profile..." : "Save Profile Changes"}</span>
                                        </button>
                                    </div>
                                </form>
                            ) : null}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
