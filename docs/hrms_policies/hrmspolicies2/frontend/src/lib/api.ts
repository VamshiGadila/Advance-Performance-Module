const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

export async function api<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = typeof window !== "undefined" ? localStorage.getItem("hrms_token") : null;
    const headers = new Headers(options.headers);

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers,
        credentials: "include"
    });

    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;
        try {
            const data = await response.json();
            if (data?.message) message = data.message;
            else if (data?.errors && Array.isArray(data.errors)) message = data.errors.join(", ");
        } catch {}

        if (response.status === 401 && typeof window !== "undefined") {
            localStorage.removeItem("hrms_token");
            localStorage.removeItem("hrms_user");
            if (!endpoint.includes("/auth/")) {
                window.location.href = "/login?expired=true";
            }
        }
        throw new Error(message);
    }

    if (response.status === 204) return undefined as T;

    const text = await response.text();
    if (!text || text.trim() === "") return undefined as T;

    try {
        const json = JSON.parse(text);
        if (json && typeof json === "object" && "data" in json) {
            return json.data as T;
        }
        return json as T;
    } catch {
        return text as unknown as T;
    }
}
