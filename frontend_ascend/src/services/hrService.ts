import { api } from "@/lib/api";

export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export interface Employee {
    id: number;
    employeeCode: string;
    name: string;
    email: string;
    departmentId: number | null;
    departmentName?: string | null;
    role: "HR" | "MANAGER" | "EMPLOYEE";
    designation?: string | null;
    managerId?: number | null;
    managerName?: string | null;
    managerCode?: string | null;
    skill?: string | null;
    location?: string | null;
    domain?: string | null;
    experienceYears?: number | null;
    active?: boolean;
}

export interface ManagerHierarchyNode {
    managerId: number;
    managerCode: string;
    managerName: string;
    managerEmail: string;
    managerDesignation?: string | null;
    departmentId: number | null;
    departmentName?: string | null;
    totalReports: number;
    directReports: Employee[];
}

export interface Dashboard {
    employeeCount: number;
    managerCount: number;
    activeCycleCount: number;
    activeAssignmentCount: number;
}

export interface Cycle {
    id: number;
    name: string;
    description?: string | null;
    startDate: string;
    endDate: string;
    status: "DRAFT" | "ACTIVE" | "CLOSED";
}

export interface Department {
    id: number;
    name: string;
    description?: string | null;
    defaultManagerId?: number | null;
    defaultManagerName?: string | null;
    employeeCount?: number;
}

export interface Assignment {
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

export interface CreateManagerRequest {
    name: string;
    email: string;
    password?: string;
    temporaryPassword?: string;
    confirmPassword?: string;
    departmentId: number;
}

export async function getHRDashboard(): Promise<Dashboard> {
    try {
        const [employees, managers, cycles, assignments] = await Promise.all([
            getEmployees().catch(() => []),
            getManagers().catch(() => []),
            getCycles().catch(() => []),
            getAssignments().catch(() => [])
        ]);

        const activeCycles = cycles.filter((c) => c.status === "ACTIVE");
        const activeAssignments = assignments.filter((a) => a.active);

        return {
            employeeCount: employees.length,
            managerCount: managers.length,
            activeCycleCount: activeCycles.length,
            activeAssignmentCount: activeAssignments.length
        };
    } catch {
        return {
            employeeCount: 0,
            managerCount: 0,
            activeCycleCount: 0,
            activeAssignmentCount: 0
        };
    }
}

export const getEmployees = () => api<Employee[]>("/hr/employees");

export const getManagers = () => api<Employee[]>("/hr/employees/managers");

export const createManager = (data: CreateManagerRequest) =>
    api<Employee>("/hr/employees/managers", {
        method: "POST",
        body: JSON.stringify({
            name: data.name,
            email: data.email,
            password: data.password || data.temporaryPassword || "Password1",
            confirmPassword: data.confirmPassword || data.password || data.temporaryPassword || "Password1",
            departmentId: data.departmentId
        })
    });

export const promoteEmployee = (id: number) =>
    api<Employee>(`/hr/employees/${id}/promote`, {
        method: "PATCH"
    });

export const searchEmployees = (params: Record<string, string | number | boolean | undefined | null>) => {
    const cleanParams: Record<string, string> = {};
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            cleanParams[k] = String(v);
        }
    });
    const query = new URLSearchParams(cleanParams).toString();
    return api<PagedResponse<Employee>>(`/hr/employees/search?${query}`);
};

export const getDepartments = () => api<Department[]>("/hr/departments");

export const createDepartment = (data: { name: string; description?: string; defaultManagerId?: number }) =>
    api<Department>("/hr/departments", {
        method: "POST",
        body: JSON.stringify(data)
    });

export const getAssignments = () => api<Assignment[]>("/hr/manager-assignments");

export const searchAssignments = (params: Record<string, string | number | boolean | undefined | null>) => {
    const cleanParams: Record<string, string> = {};
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            cleanParams[k] = String(v);
        }
    });
    const query = new URLSearchParams(cleanParams).toString();
    return api<PagedResponse<Assignment>>(`/hr/manager-assignments/search?${query}`);
};

export const assignManager = (employeeId: number, managerId: number, performanceCycleId?: number) =>
    api<Assignment>("/hr/manager-assignments", {
        method: "POST",
        body: JSON.stringify({ employeeId, managerId, performanceCycleId })
    });

export const updateAssignment = (id: number, managerId: number, active?: boolean) =>
    api<Assignment>(`/hr/manager-assignments/${id}`, {
        method: "PUT",
        body: JSON.stringify({ managerId, active })
    });

export const deleteAssignment = (id: number) =>
    api<void>(`/hr/manager-assignments/${id}`, {
        method: "DELETE"
    });

export const getCycles = () => api<Cycle[]>("/hr/performance-cycles");

export const searchCycles = (params: Record<string, string | number | boolean | undefined | null>) => {
    const cleanParams: Record<string, string> = {};
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
            cleanParams[k] = String(v);
        }
    });
    const query = new URLSearchParams(cleanParams).toString();
    return api<PagedResponse<Cycle>>(`/hr/performance-cycles/search?${query}`);
};

export const getActiveCycle = () => api<Cycle>("/hr/performance-cycles/active");

export const getCycleById = (id: number) => api<Cycle>(`/hr/performance-cycles/${id}`);

export const createCycle = (data: {
    name: string;
    description?: string;
    startDate: string;
    endDate: string;
}) =>
    api<Cycle>("/hr/performance-cycles", {
        method: "POST",
        body: JSON.stringify(data)
    });

export const launchCycle = (id: number) =>
    api<Cycle>(`/hr/performance-cycles/${id}/launch`, {
        method: "PATCH"
    });

export const closeCycle = (id: number) =>
    api<Cycle>(`/hr/performance-cycles/${id}/close`, {
        method: "PATCH"
    });

export const activateCycle = launchCycle;

export const getAllStaff = () => api<Employee[]>("/hr/employees/all");

export const changeEmployeeManager = (employeeId: number, managerId: number) =>
    api<Employee>(`/hr/employees/${employeeId}/manager`, {
        method: "PATCH",
        body: JSON.stringify({ managerId })
    });

export const transferEmployeeDepartment = (employeeId: number, departmentId: number) =>
    api<Employee>(`/hr/employees/${employeeId}/department`, {
        method: "PATCH",
        body: JSON.stringify({ departmentId })
    });

export const getManagerHierarchy = () => api<ManagerHierarchyNode[]>("/hr/employees/hierarchy");