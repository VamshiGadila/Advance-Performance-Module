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

export async function verifyOtp(email: string, otp: string): Promise<string> {
    return api<string>("/auth/verify-otp", {
        method: "POST",
        body: JSON.stringify({ email, otp })
    });
}

export async function resetPassword(data: {
    email: string;
    otp: string;
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

export interface UserProfile {
    id: number;
    employeeCode: string;
    name: string;
    email: string;
    role: "HR" | "MANAGER" | "EMPLOYEE";
    departmentId: number | null;
    departmentName: string | null;
    designation: string | null;
    managerId: number | null;
    managerName: string | null;
    managerCode: string | null;
    skill: string | null;
    domain: string | null;
    location: string | null;
    experienceYears: number | null;
}

export interface UpdateProfileRequest {
    name?: string;
    skill: string;
    domain: string;
    location: string;
    experienceYears: number;
    currentPassword?: string;
    newPassword?: string;
    confirmPassword?: string;
}

export async function getMyProfile(): Promise<UserProfile> {
    return api<UserProfile>("/employee/profile", {
        method: "GET"
    });
}

export async function updateMyProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    return api<UserProfile>("/employee/profile", {
        method: "PATCH",
        body: JSON.stringify(data)
    });
}