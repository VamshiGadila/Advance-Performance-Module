const API_URL =
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export async function api<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> {
    let token =
        typeof window !== "undefined"
            ? localStorage.getItem("ascend_token")
            : null;

    if (!token && typeof window !== "undefined") {
        try {
            const rawUser = localStorage.getItem("ascend_user");
            if (rawUser) {
                const parsed = JSON.parse(rawUser);
                if (parsed?.token && parsed.token !== "OAUTH2_SESSION") {
                    token = parsed.token;
                }
            }
        } catch {}
    }

    const headers = new Headers(options.headers);

    // Only set application/json when there is a request body and Content-Type is not already provided
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    // Attach JWT Bearer Authorization header
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers,
        credentials: "include",
        cache: "no-store"
    });

    // =================================================
    // ERROR HANDLING ACROSS ALL HTTP STATUS CODES
    // =================================================
    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;

        try {
            const data = await response.json();

            if (data?.message && typeof data.message === "string" && data.message.trim() !== "") {
                message = data.message;
            } else if (data?.error && typeof data.error === "string" && data.error.trim() !== "") {
                message = data.error;
            } else if (data?.errors && Array.isArray(data.errors)) {
                message = data.errors.join(", ");
            } else if (data?.errors && typeof data.errors === "object") {
                const details = Object.entries(data.errors)
                    .map(([k, v]) => `${k}: ${v}`)
                    .join(", ");
                message = `Validation error: ${details}`;
            }
        } catch {
            // Non-JSON or empty response payload
        }

        // Standardized Safe Security Status Mapping
        if (response.status === 401) {
            const isAuthEndpoint = endpoint.startsWith("/auth/") || endpoint.startsWith("auth/");
            if (typeof window !== "undefined") {
                try {
                    localStorage.removeItem("ascend_token");
                    localStorage.removeItem("ascend_user");
                    const path = window.location.pathname;
                    if (!isAuthEndpoint && path !== "/login" && path !== "/forgot-password" && path !== "/signup") {
                        window.location.href = "/login?expired=true";
                    }
                } catch {}
            }
            if (!message || message.startsWith("Request failed")) {
                message = "Your session has expired. Please log in again.";
            }
        } else if (response.status === 403) {
            if (!message || message.startsWith("Request failed")) {
                message = "You do not have permission to perform this action.";
            }
        } else if (response.status === 429) {
            message = "Too many requests. Please wait before trying again.";
        } else if (response.status === 404 && message.startsWith("Request failed")) {
            message = "Requested resource not found.";
        } else if (response.status >= 500 && message.startsWith("Request failed")) {
            message = "An unexpected server error occurred. Please try again later.";
        }

        throw new Error(message);
    }

    // =================================================
    // NO CONTENT / EMPTY BODY
    // =================================================
    if (response.status === 204) {
        return undefined as T;
    }

    const text = await response.text();
    if (!text || text.trim() === "") {
        return undefined as T;
    }

    try {
        const json = JSON.parse(text);
        if (json && typeof json === "object" && "data" in json && "success" in json) {
            return json.data as T;
        }
        return json as T;
    } catch {
        return text as unknown as T;
    }
}