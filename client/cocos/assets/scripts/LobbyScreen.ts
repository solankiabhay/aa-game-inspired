import { _decorator, Component, Label, Button } from "cc";
import { GameService, DemoGameId, GameSummary, GameSession } from "./GameService";

const { ccclass, property } = _decorator;

/**
 * Native lobby controller. Wire labels and buttons in Creator's Inspector.
 * Starting a round no longer auto-completes it: the player must explicitly finish.
 */
@ccclass("LobbyScreen")
export class LobbyScreen extends Component {
    @property(Label) catalogueLabel: Label | null = null;
    @property(Label) statusLabel: Label | null = null;
    @property(Label) scoreLabel: Label | null = null;
    @property(Label) historyLabel: Label | null = null;
    @property(Button) playButton: Button | null = null;
    @property(Button) finishButton: Button | null = null;

    private games: GameSummary[] = [];
    private selectedGameId: DemoGameId = "skyline-crash";
    private activeSession: GameSession | null = null;
    private localActivity = 0;
    private busy = false;

    onLoad(): void {
        this.setButtonEnabled(this.finishButton, false);
        this.refresh().catch(() => undefined);
    }

    async refresh(): Promise<void> {
        this.setStatus("Loading demo games…");
        try {
            this.games = await GameService.catalogue();
            if (!this.games.some(game => game.id === this.selectedGameId) && this.games.length > 0) {
                this.selectedGameId = this.games[0].id;
            }
            if (this.catalogueLabel) {
                this.catalogueLabel.string = this.games.length
                    ? this.games.map(game =>
                        (game.id === this.selectedGameId ? "▶ " : "") +
                        game.title + "\n" + game.category + "\n" + game.description).join("\n\n")
                    : "No demo games are currently available.";
            }
            this.setStatus("Choose a game. Demo credits have no monetary value.");
            await this.refreshHistory();
        } catch (error) {
            this.setStatus(this.message(error));
        }
    }

    selectGame(gameId: DemoGameId): void {
        if (this.activeSession || this.busy) {
            this.setStatus("Finish the active round before changing games.");
            return;
        }
        if (!this.games.some(game => game.id === gameId)) {
            this.setStatus("That game is not currently available.");
            return;
        }
        this.selectedGameId = gameId;
        if (this.catalogueLabel) {
            this.catalogueLabel.string = this.games.map(game =>
                (game.id === this.selectedGameId ? "▶ " : "") +
                game.title + "\n" + game.category + "\n" + game.description).join("\n\n");
        }
        this.setStatus("Selected: " + (this.games.find(game => game.id === gameId)?.title ?? gameId));
    }

    async playSelected(): Promise<void> {
        if (this.busy || this.activeSession) {
            this.setStatus("A round is already active. Finish it before starting another.");
            return;
        }
        this.busy = true;
        this.setButtonEnabled(this.playButton, false);
        this.setStatus("Starting secure demo session…");
        try {
            this.activeSession = await GameService.start(this.selectedGameId);
            this.localActivity = 0;
            this.updateScore();
            this.setButtonEnabled(this.finishButton, true);
            this.setStatus("Round active. Play your mini-challenge, then choose Finish round.");
        } catch (error) {
            this.setStatus(this.message(error));
        } finally {
            this.busy = false;
            this.setButtonEnabled(this.playButton, !this.activeSession);
        }
    }

    /** Wire a gameplay button to this method. The activity score is local and cosmetic. */
    recordAction(): void {
        if (!this.activeSession || this.busy) return;
        this.localActivity = Math.min(9999, this.localActivity + 1);
        this.updateScore();
        this.setStatus("Nice! Local activity: " + this.localActivity + ". Finish the round to save the server score.");
    }

    async finishRound(): Promise<void> {
        if (!this.activeSession || this.busy) return;
        this.busy = true;
        this.setButtonEnabled(this.finishButton, false);
        this.setStatus("Saving your round…");
        const sessionId = this.activeSession.sessionId;
        try {
            const result = await GameService.complete(sessionId);
            this.activeSession = null;
            if (this.scoreLabel) this.scoreLabel.string =
                "Server score: " + result.score + "\nYour activity: " + this.localActivity;
            this.setStatus("Round saved. Server-generated score: " + result.score + ". No credits were wagered.");
            await this.refreshHistory();
        } catch (error) {
            this.setStatus(this.message(error));
            this.setButtonEnabled(this.finishButton, true);
        } finally {
            this.busy = false;
            this.setButtonEnabled(this.playButton, !this.activeSession);
        }
    }

    private async refreshHistory(): Promise<void> {
        try {
            const sessions = await GameService.recent(8);
            if (this.historyLabel) {
                this.historyLabel.string = sessions.length
                    ? sessions.map(session => {
                        const game = this.games.find(item => item.id === session.gameId);
                        return (game?.title ?? session.gameId) + " · " + session.status +
                            " · score " + session.score;
                    }).join("\n")
                    : "No rounds yet. Start your first demo game.";
            }
        } catch {
            if (this.historyLabel) this.historyLabel.string = "Sign in to view recent rounds.";
        }
    }

    private updateScore(): void {
        if (this.scoreLabel) this.scoreLabel.string =
            "Local activity: " + this.localActivity + "\nServer score is calculated when the round is completed.";
    }

    private setButtonEnabled(button: Button | null, enabled: boolean): void {
        if (button) button.interactable = enabled;
    }

    private setStatus(message: string): void {
        if (this.statusLabel) this.statusLabel.string = message;
    }

    private message(error: unknown): string {
        return error instanceof Error ? error.message : "The request could not be completed.";
    }
}
