import { api } from "@/lib/api";
import { PagedResponse } from "./hrService";

export type GoalType = "OKR" | "KPI";

export type GoalStatus =
    | "PENDING_ACCEPTANCE"
    | "ACCEPTED"
    | "IN_PROGRESS"
    | "MODIFICATION_REQUESTED"
    | "COMPLETED"
    | "DRAFT"
    | "ACTIVE";

export interface EmployeeGoal {
    id: number;
    cycleId: number;
    managerId?: number;
    managerName?: string;
    title: string;
    description: string | null;
    target: string | null;
    goalType: GoalType;
    goalScope?: string;
    weight: number;
    progress?: number;
    dueDate: string | null;
    status: GoalStatus;
    employeeAccepted?: boolean;
    modificationRequested?: boolean;
    employeeComment?: string | null;
    completedAt?: string | null;
}

export interface MyManagerInfo {
    id: number;
    employeeId: number;
    employeeName: string;
    employeeCode: string;
    managerId: number;
    managerName: string;
    managerCode: string;
    performanceCycleId?: number | null;
    cycleName?: string | null;
    active: boolean;
    assignedDate?: string;
}

export interface ModificationRequest {
    id: number;
    goalId: number;
    goalTitle: string;
    comment: string;
    requestedChanges?: string;
    status: "PENDING" | "APPROVED" | "REJECTED";
    managerComment?: string;
    createdAt?: string;
}

export async function getMyManager(): Promise<MyManagerInfo | null> {
    return api<MyManagerInfo>("/employee/goals/my-manager").catch(() => null);
}

export async function getMyGoals(): Promise<EmployeeGoal[]> {
    return api<EmployeeGoal[]>("/employee/goals", {
        method: "GET"
    });
}

export async function getEmployeeGoalById(id: number): Promise<EmployeeGoal> {
    return api<EmployeeGoal>(`/employee/goals/${id}`, {
        method: "GET"
    });
}

export async function acceptGoal(id: number): Promise<EmployeeGoal> {
    return api<EmployeeGoal>(`/employee/goals/${id}/accept`, {
        method: "PATCH"
    });
}

export async function updateProgress(
    id: number,
    progress: number,
    comment?: string
): Promise<EmployeeGoal> {
    return api<EmployeeGoal>(`/employee/goals/${id}/progress`, {
        method: "PATCH",
        body: JSON.stringify({ progress, comment })
    });
}

export async function requestModification(
    id: number,
    comment: string,
    requestedChanges?: string
): Promise<any> {
    return api<any>(`/employee/goals/${id}/modification-request`, {
        method: "PATCH",
        body: JSON.stringify({ comment, requestedChanges })
    });
}

export async function searchMyGoals(
    params: Record<string, string | number | boolean | undefined | null>
): Promise<PagedResponse<EmployeeGoal>> {
    const cleanParams: Record<string, string> = {};
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            cleanParams[k] = String(v);
        }
    });
    const query = new URLSearchParams(cleanParams).toString();
    return api<PagedResponse<EmployeeGoal>>(`/employee/goals/search?${query}`);
}

export async function getMyModificationRequests(): Promise<ModificationRequest[]> {
    return api<ModificationRequest[]>("/employee/goals/modification-requests", {
        method: "GET"
    });
}