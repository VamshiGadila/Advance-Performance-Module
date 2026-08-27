export type Role =
    | "HR"
    | "MANAGER"
    | "EMPLOYEE";

export interface AuthUser {
    token: string;
    userId: number;
    employeeCode: string;
    name: string;
    email: string;
    role: Role;
}

// =====================================================
// GET LOGGED-IN USER
// =====================================================

export function getUser(): AuthUser | null {
    if (typeof window === "undefined") {
        return null;
    }

    const raw = localStorage.getItem("ascend_user");

    if (!raw) {
        return null;
    }

    try {
        return JSON.parse(raw) as AuthUser;
    } catch {
        localStorage.removeItem("ascend_user");
        localStorage.removeItem("ascend_token");
        return null;
    }
}

// =====================================================
// GET JWT TOKEN
// =====================================================

export function getToken(): string | null {
    if (typeof window === "undefined") {
        return null;
    }

    return localStorage.getItem("ascend_token");
}

// =====================================================
// SAVE USER
// =====================================================

export function saveUser(user: AuthUser): void {
    if (typeof window === "undefined") {
        return;
    }

    localStorage.setItem(
        "ascend_token",
        user.token
    );

    localStorage.setItem(
        "ascend_user",
        JSON.stringify(user)
    );
}

// =====================================================
// LOGOUT
// =====================================================

export function logout(): void {
    if (typeof window === "undefined") {
        return;
    }

    localStorage.removeItem("ascend_token");
    localStorage.removeItem("ascend_user");
}