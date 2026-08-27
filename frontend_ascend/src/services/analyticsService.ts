import { api } from "@/lib/api";

export interface DepartmentMetric {
    departmentId: number;
    departmentName: string;
    employeeCount: number;
    goalCompletionRate: number;
}

export interface HrAnalytics {
    totalEmployees: number;
    totalManagers: number;
    activeAssignments: number;
    totalCycles: number;
    activeCycleId: number | null;
    activeCycleName: string | null;
    activeCycleStartDate: string | null;
    activeCycleEndDate: string | null;
    totalGoals: number;
    completedGoals: number;
    completionRate: number;
    goalsByStatus: Record<string, number>;
    departmentMetrics: DepartmentMetric[];
}

export interface ManagerAnalytics {
    teamSize: number;
    totalGoals: number;
    completedGoals: number;
    inProgressGoals: number;
    pendingAcceptanceGoals: number;
    teamCompletionRate: number;
    pendingModificationRequests: number;
    goalsByStatus: Record<string, number>;
}

export interface EmployeeAnalytics {
    totalGoals: number;
    totalWeightAllocated: number;
    averageProgress: number;
    completedGoals: number;
    inProgressGoals: number;
    pendingAcceptanceGoals: number;
    activeCycleId: number | null;
    activeCycleName: string | null;
    goalsByStatus: Record<string, number>;
}

export const getHrAnalytics = () => api<HrAnalytics>("/hr/analytics");
export const getManagerAnalytics = () => api<ManagerAnalytics>("/manager/analytics");
export const getEmployeeAnalytics = () => api<EmployeeAnalytics>("/employee/analytics");
