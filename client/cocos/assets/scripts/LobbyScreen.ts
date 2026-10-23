import { _decorator, Component, Label } from "cc";
import { GameService, DemoGameId, GameSummary } from "./GameService";

const { ccclass, property } = _decorator;

/** Minimal lobby controller; wire a Label and buttons to refresh/playSelected in the Inspector. */
@ccclass("LobbyScreen")
export class LobbyScreen extends Component {
    @property(Label) catalogueLabel: Label | null = null;
    @property(Label) statusLabel: Label | null = null;

    private games: GameSummary[] = [];
    private selectedGameId: DemoGameId = "skyline-crash";

    async refresh(): Promise<void> {
        this.setStatus("Loading demo games…");
        try {
            this.games = await GameService.catalogue();
            if (!this.games.some(game => game.id === this.selectedGameId) && this.games.length > 0) {
                this.selectedGameId = this.games[0].id;
            }
            if (this.catalogueLabel) {
                this.catalogueLabel.string = this.games.length
                    ? this.games.map(game => game.title + "\n" + game.description).join("\n\n")
                    : "No demo games are currently available.";
            }
            this.setStatus("Demo credits have no monetary value.");
        } catch (error) {
            this.setStatus(error instanceof Error ? error.message : "Could not load games.");
        }
    }

    selectGame(gameId: DemoGameId): void {
        this.selectedGameId = gameId;
        this.setStatus("Selected: " + gameId);
    }

    async playSelected(): Promise<void> {
        this.setStatus("Starting demo session…");
        try {
            const session = await GameService.start(this.selectedGameId);
            this.setStatus("Demo session started. Completing sample session…");
            const result = await GameService.complete(session.sessionId);
            this.setStatus("Demo session complete. Sample score: " + result.score + ". No monetary value.");
        } catch (error) {
            this.setStatus(error instanceof Error ? error.message : "Could not start demo session.");
        }
    }

    private setStatus(message: string): void {
        if (this.statusLabel) this.statusLabel.string = message;
    }
}
