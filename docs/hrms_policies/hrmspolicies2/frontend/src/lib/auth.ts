export interface AuthUser {
    token: string;
    email: string;
    name?: string;
    role: "ADMIN" | "USER";
}

export function getUser(): AuthUser | null {
    if (typeof window === "undefined") return null;
    const raw = localStorage.getItem("hrms_user");
    if (!raw) return null;
    try {
        return JSON.parse(raw) as AuthUser;
    } catch {
        localStorage.removeItem("hrms_user");
        localStorage.removeItem("hrms_token");
        return null;
    }
}

export function getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("hrms_token");
}

export function saveUser(user: AuthUser): void {
    if (typeof window === "undefined") return;
    localStorage.setItem("hrms_token", user.token);
    localStorage.setItem("hrms_user", JSON.stringify(user));
    document.cookie = "app_suite_active_session=true; path=/; max-age=86400; SameSite=Lax";
}

export function logout(): void {
    if (typeof window === "undefined") return;
    localStorage.removeItem("hrms_token");
    localStorage.removeItem("hrms_user");
    document.cookie = "app_suite_active_session=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
}
