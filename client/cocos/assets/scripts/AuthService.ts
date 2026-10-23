import { ApiClient } from "./ApiClient";

export interface SessionResponse {
    accessToken: string;
    tokenType: "Bearer";
    expiresAt: string;
}

export interface UserProfile {
    id: number;
    email: string;
    role: "USER" | "ADMIN";
}

export interface DemoCreditBalance {
    balance: number;
    currency: "DEMO_CREDITS";
    redeemable: false;
}

/** Authentication and profile methods. Token stays in memory for this app session. */
export class AuthService {
    static async register(email: string, password: string): Promise<{status: string; email: string; startingDemoCredits: number}> {
        return ApiClient.post<{status: string; email: string; startingDemoCredits: number}>("/api/auth/register", { email: email.trim(), password });
    }

    static async login(email: string, password: string): Promise<SessionResponse> {
        const session = await ApiClient.post<SessionResponse>("/api/auth/login", { email: email.trim(), password });
        ApiClient.setToken(session.accessToken);
        return session;
    }

    static async verifyEmail(token: string): Promise<{status: string; message: string}> {
        return ApiClient.post<{status: string; message: string}>("/api/auth/verify-email", { token });
    }

    static async requestPasswordReset(email: string): Promise<{status: string; message: string}> {
        return ApiClient.post<{status: string; message: string}>("/api/auth/password-reset/request", { email: email.trim() });
    }

    static async confirmPasswordReset(token: string, newPassword: string): Promise<{status: string; message: string}> {
        return ApiClient.post<{status: string; message: string}>("/api/auth/password-reset/confirm", { token, newPassword });
    }

    static async profile(): Promise<UserProfile> {
        return ApiClient.get<UserProfile>("/api/auth/me");
    }

    static async demoCredits(): Promise<DemoCreditBalance> {
        return ApiClient.get<DemoCreditBalance>("/api/auth/me/demo-credits");
    }

    static async logout(): Promise<void> {
        try {
            await ApiClient.post<void>("/api/auth/logout");
        } finally {
            ApiClient.setToken(null);
        }
    }
}
