import { api } from "@/lib/api";
import { AuthUser, saveUser } from "@/lib/auth";

export type LoginRequest = {
    email: string;
    password: string;
};

export type SignupRequest = {
    name: string;
    email: string;
    password: string;
    confirmPassword: string;
    departmentId: number;
};

export type SignupResponse = {
    id: number;
    employeeCode: string;
    name: string;
    email: string;
    departmentId: number;
    role: string;
    message: string;
};

export type PublicDepartment = {
    id: number;
    name: string;
};

export async function login(data: LoginRequest): Promise<AuthUser> {
    const response = await api<AuthUser>("/auth/login", {
        method: "POST",
        body: JSON.stringify(data)
    });

    saveUser(response);
    return response;
}

export async function signup(data: SignupRequest): Promise<SignupResponse> {
    return api<SignupResponse>("/auth/signup", {
        method: "POST",
        body: JSON.stringify(data)
    });
}

export async function getPublicDepartments(): Promise<PublicDepartment[]> {
    return api<PublicDepartment[]>("/auth/departments", {
        method: "GET"
    });
}

export async function getCurrentUser(): Promise<AuthUser> {
    return api<AuthUser>("/auth/me", {
        method: "GET"
    });
}

export async function forgotPassword(email: string): Promise<string> {
    return api<string>("/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify({ email })
    });
}

export async function resetPassword(data: {
    email: string;
    username: string;
    newPassword: string;
    confirmPassword: string;
}): Promise<string> {
    return api<string>("/auth/reset-password", {
        method: "POST",
        body: JSON.stringify(data)
    });
}

export async function logoutApi(): Promise<void> {
    try {
        await api<string>("/auth/logout", {
            method: "POST"
        });
    } catch {
        // Ignore if already unauthenticated
    }
}