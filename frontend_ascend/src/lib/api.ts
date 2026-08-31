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
            // Response was not JSON (e.g. text/html or blank 500/403)
            if (response.status === 401) {
                message = "Session expired or invalid credentials. Please log in.";
            } else if (response.status === 403) {
                message = "Access denied: You do not have permission to access this resource.";
            } else if (response.status === 404) {
                message = "Requested resource not found.";
            } else if (response.status === 409) {
                message = "A conflict occurred with an existing record.";
            } else if (response.status >= 500) {
                message = "Internal server error. Please try again later.";
            }
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