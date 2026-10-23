/**
 * Cocos Creator 3.x API client for the AA Game demo backend.
 * Keep the token in memory; never put it in a URL or log it.
 */
export class ApiError extends Error {
    constructor(public readonly status: number, public readonly code: string, message: string) {
        super(message);
        this.name = "ApiError";
    }
}

export class ApiClient {
    private static baseUrl = "http://localhost:8080";
    private static accessToken: string | null = null;
    private static readonly timeoutMs = 10000;

    static configure(baseUrl: string): void {
        this.baseUrl = baseUrl.replace(/\/+$/, "");
    }

    static setToken(token: string | null): void {
        this.accessToken = token;
    }

    static async get<T>(path: string): Promise<T> {
        return this.request<T>("GET", path);
    }

    static async post<T>(path: string, body?: unknown): Promise<T> {
        return this.request<T>("POST", path, body);
    }

    private static request<T>(method: string, path: string, body?: unknown): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open(method, this.baseUrl + path, true);
            xhr.timeout = this.timeoutMs;
            xhr.setRequestHeader("Accept", "application/json");
            if (body !== undefined) xhr.setRequestHeader("Content-Type", "application/json");
            if (this.accessToken) xhr.setRequestHeader("Authorization", "Bearer " + this.accessToken);
            xhr.onload = () => {
                let payload: any = null;
                try { payload = xhr.responseText ? JSON.parse(xhr.responseText) : null; }
                catch { reject(new ApiError(xhr.status, "INVALID_RESPONSE", "The server returned an invalid response.")); return; }
                if (xhr.status >= 200 && xhr.status < 300) { resolve(payload as T); return; }
                reject(new ApiError(xhr.status, payload?.code || "REQUEST_FAILED",
                    payload?.message || "The request could not be completed."));
            };
            xhr.onerror = () => reject(new ApiError(0, "NETWORK_ERROR", "Could not reach the API."));
            xhr.ontimeout = () => reject(new ApiError(0, "TIMEOUT", "The API request timed out."));
            xhr.send(body === undefined ? null : JSON.stringify(body));
        });
    }
}
