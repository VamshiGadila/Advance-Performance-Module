import { api } from "@/lib/api";
import { Cycle, PagedResponse } from "./hrService";

export interface TeamMember {
    id: number;
    employeeCode: string;
    name: string;
    email: string;
    departmentId?: number | null;
    role?: string;
}

export type GoalStatus =
    | "PENDING_ACCEPTANCE"
    | "ACCEPTED"
    | "IN_PROGRESS"
    | "MODIFICATION_REQUESTED"
    | "COMPLETED"
    | "DRAFT"
    | "ACTIVE";

export interface Goal {
    id: number;
    cycleId: number;
    employeeId: number;
    employeeName?: string;
    managerId: number;
    managerName?: string;
    goalType: "OKR" | "KPI";
    goalScope: "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY";
    parentGoalId?: number | null;
    title: string;
    description: string | null;
    target: string | null;
    weight: number;
    progress?: number;
    dueDate: string | null;
    status: GoalStatus;
    modificationRequested?: boolean;
    employeeAccepted?: boolean;
    employeeComment?: string | null;
}

export interface ModificationRequest {
    id: number;
    goalId: number;
    goalTitle: string;
    employeeId: number;
    employeeName: string;
    managerId: number;
    managerName: string;
    comment: string;
    requestedChanges?: string;
    status: "PENDING" | "APPROVED" | "REJECTED";
    managerComment?: string;
    requestedAt?: string;
    reviewedAt?: string;
}

export interface CreateGoalInput {
    cycleId: number;
    employeeId: number;
    goalType: "OKR" | "KPI";
    goalScope: "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY";
    parentGoalId?: number | null;
    title: string;
    description?: string;
    target?: string;
    weight: number;
    dueDate?: string | null;
}

export interface UpdateGoalInput {
    title: string;
    description?: string;
    target?: string;
    weight: number;
    dueDate?: string | null;
    goalType?: "OKR" | "KPI";
    goalScope?: "INDIVIDUAL" | "TEAM" | "DEPARTMENT" | "COMPANY";
    status?: GoalStatus | string;
}

export async function getTeam(): Promise<TeamMember[]> {
    try {
        // 1. Direct call to manager direct reports endpoint (HR-assigned employees)
        const team = await api<any[]>("/manager/goals/team");
        if (team && Array.isArray(team)) {
            return team.map((emp) => ({
                id: emp.id,
                name: emp.name,
                employeeCode: emp.employeeCode,
                email: emp.email,
                departmentId: emp.departmentId,
                role: emp.role || "EMPLOYEE"
            }));
        }
    } catch {
        // Fallback to goals if endpoint fails
    }

    try {
        const goals = await getManagerGoals();
        const memberMap = new Map<number, TeamMember>();

        goals.forEach((g) => {
            if (g.employeeId && !memberMap.has(g.employeeId)) {
                memberMap.set(g.employeeId, {
                    id: g.employeeId,
                    name: g.employeeName || `Employee #${g.employeeId}`,
                    employeeCode: `EMP${String(g.employeeId).padStart(3, "0")}`,
                    email: `emp${g.employeeId}@ascend.local`,
                    role: "EMPLOYEE"
                });
            }
        });

        return Array.from(memberMap.values());
    } catch {
        return [];
    }
}

export const getManagerGoals = () => api<Goal[]>("/manager/goals");

export const getCycles = () => api<Cycle[]>("/hr/performance-cycles");

export const getActiveCycle = () => api<Cycle>("/hr/performance-cycles/active");

export const createGoal = (data: CreateGoalInput) =>
    api<Goal>("/manager/goals", {
        method: "POST",
        body: JSON.stringify(data)
    });

export const updateGoal = (id: number, data: UpdateGoalInput) =>
    api<Goal>(`/manager/goals/${id}`, {
        method: "PUT",
        body: JSON.stringify(data)
    });

export const deleteGoal = (id: number) =>
    api<void>(`/manager/goals/${id}`, {
        method: "DELETE"
    });

export const getGoalById = (id: number) =>
    api<Goal>(`/manager/goals/${id}`);

export const getEmployeeGoals = (employeeId: number, cycleId?: number) =>
    api<Goal[]>(`/manager/goals/employee/${employeeId}${cycleId ? `?cycleId=${cycleId}` : ""}`);

export const getModificationRequests = (status: string = "PENDING") =>
    api<ModificationRequest[]>(`/manager/goal-modification-requests?status=${status}`);

export const approveModificationRequest = (id: number, comment?: string) =>
    api<ModificationRequest>(`/manager/goal-modification-requests/${id}/approve`, {
        method: "PATCH",
        body: JSON.stringify({ comment })
    });

export const rejectModificationRequest = (id: number, comment?: string) =>
    api<ModificationRequest>(`/manager/goal-modification-requests/${id}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ comment })
    });

export const searchManagerGoals = (params: Record<string, string | number | boolean | undefined | null>) => {
    const cleanParams: Record<string, string> = {};
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            cleanParams[k] = String(v);
        }
    });
    const query = new URLSearchParams(cleanParams).toString();
    return api<PagedResponse<Goal>>(`/manager/goals/search?${query}`);
};

export const searchGoals = searchManagerGoals;