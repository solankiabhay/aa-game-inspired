import { ApiClient } from "./ApiClient";

export type DemoGameId = "skyline-crash" | "neon-reels" | "reef-ranger";

export interface GameSummary {
    id: DemoGameId;
    title: string;
    category: string;
    description: string;
    enabled: boolean;
}

export interface GameSession {
    sessionId: string;
    gameId: DemoGameId;
    status: "ACTIVE" | "COMPLETED";
    score: number;
    createdAt?: string;
    finishedAt?: string | null;
}

export interface SessionList {
    sessions: GameSession[];
    limit: number;
}

/** Typed API boundary for the server-authoritative demo-game catalogue and sessions. */
export class GameService {
    static async catalogue(): Promise<GameSummary[]> {
        const response = await ApiClient.get<{count: number; games: GameSummary[]}>("/api/games");
        return response.games.filter(game => game.enabled);
    }

    static async start(gameId: DemoGameId): Promise<GameSession> {
        return ApiClient.post<GameSession>("/api/games/" + encodeURIComponent(gameId) + "/sessions");
    }

    static async complete(sessionId: string): Promise<GameSession> {
        return ApiClient.post<GameSession>("/api/games/sessions/" + encodeURIComponent(sessionId) + "/complete");
    }

    static async recent(limit = 20): Promise<GameSession[]> {
        const safeLimit = Math.max(1, Math.min(Math.floor(limit), 100));
        const response = await ApiClient.get<SessionList>("/api/games/sessions?limit=" + safeLimit);
        return response.sessions;
    }
}
