import { _decorator, Component, Label, Button } from "cc";
import { GameService, DemoGameId, GameSession } from "./GameService";

const { ccclass, property } = _decorator;

const SYMBOLS = ["✦", "◆", "●", "▲"];

/**
 * Reusable native mini-game controller for all three original demo games.
 * Wire one start button, one finish button, a status/instruction/score Label,
 * and (for neon-reels) four tile buttons to this component in Cocos Creator.
 */
@ccclass("MiniGameController")
export class MiniGameController extends Component {
    @property({ tooltip: "One of skyline-crash, neon-reels, reef-ranger." })
    gameId: DemoGameId = "skyline-crash";

    @property(Label) statusLabel: Label | null = null;
    @property(Label) instructionLabel: Label | null = null;
    @property(Label) scoreLabel: Label | null = null;
    @property(Button) startButton: Button | null = null;
    @property(Button) actionButton: Button | null = null;
    @property(Button) finishButton: Button | null = null;
    @property(Button) tile0: Button | null = null;
    @property(Button) tile1: Button | null = null;
    @property(Button) tile2: Button | null = null;
    @property(Button) tile3: Button | null = null;

    private session: GameSession | null = null;
    private busy = false;
    private activity = 0;
    private secondsLeft = 15;
    private sequence: number[] = [];
    private sequenceIndex = 0;
    private inputLocked = false;
    private taps: Button[] = [];

    onLoad(): void {
        this.taps = [this.tile0, this.tile1, this.tile2, this.tile3].filter((b): b is Button => b !== null);
        this.setButton(this.finishButton, false);
        this.setButton(this.actionButton, false);
        this.setTiles(false);
        this.setStatus("Ready to play. Demo mode only.");
        this.setInstruction(this.instructions());
        this.updateScore();
    }

    onDestroy(): void {
        this.unscheduleAllCallbacks();
    }

    async startRound(): Promise<void> {
        if (this.busy || this.session) return;
        this.busy = true;
        this.setButton(this.startButton, false);
        this.setStatus("Connecting to the arcade…");
        try {
            this.session = await GameService.start(this.gameId);
            this.activity = 0;
            this.secondsLeft = 15;
            this.sequenceIndex = 0;
            this.sequence = [];
            this.updateScore();
            this.setButton(this.finishButton, true);
            this.setButton(this.actionButton, true);
            this.setStatus("Round active");
            if (this.gameId === "skyline-crash") this.startSkyline();
            else if (this.gameId === "neon-reels") this.startNeon();
            else this.startReef();
        } catch (error) {
            this.setStatus(this.message(error));
            this.setButton(this.startButton, true);
        } finally {
            this.busy = false;
        }
    }

    /** Bind to the main gameplay button for Skyline Dash and Reef Ranger. */
    action(): void {
        if (!this.session || this.busy || this.inputLocked || this.gameId === "neon-reels") return;
        this.activity = Math.min(9999, this.activity + 1);
        this.updateScore();
        if (this.gameId === "skyline-crash") {
            this.setStatus("Great timing! " + this.secondsLeft + " seconds left.");
        } else {
            this.setStatus("Reef catch recorded! Keep going.");
        }
    }

    tile0Action(): void { this.tapTile(0); }
    tile1Action(): void { this.tapTile(1); }
    tile2Action(): void { this.tapTile(2); }
    tile3Action(): void { this.tapTile(3); }

    async finishRound(): Promise<void> {
        if (!this.session || this.busy) return;
        this.busy = true;
        this.setButton(this.finishButton, false);
        this.setButton(this.actionButton, false);
        this.setTiles(false);
        this.unscheduleAllCallbacks();
        this.setStatus("Saving round…");
        const id = this.session.sessionId;
        try {
            const result = await GameService.complete(id);
            this.session = null;
            if (this.scoreLabel) this.scoreLabel.string =
                "SERVER SCORE  " + result.score + "\nLOCAL ACTIVITY  " + this.activity;
            this.setStatus("Round saved successfully. No credits were wagered.");
            this.setInstruction("Ready for another round? Start when you are.");
            this.setButton(this.startButton, true);
        } catch (error) {
            this.setStatus(this.message(error));
            this.setButton(this.finishButton, true);
            this.setButton(this.actionButton, true);
            if (this.gameId === "neon-reels") this.setTiles(true);
        } finally {
            this.busy = false;
        }
    }

    private startSkyline(): void {
        this.setInstruction("Tap the action button as many times as you can before the 15-second timer ends.");
        this.setStatus("15 seconds. Go!");
        this.schedule(() => {
            this.secondsLeft -= 1;
            if (this.secondsLeft <= 0) {
                this.unscheduleAllCallbacks();
                this.setButton(this.actionButton, false);
                this.setStatus("Time is up! You made " + this.activity + " taps. Finish to save.");
            } else {
                this.setStatus(this.secondsLeft + " seconds remaining · " + this.activity + " taps");
            }
        }, 1);
    }

    private startReef(): void {
        this.setInstruction("Tap the action button to catch friendly fish. Every tap is a local demo action.");
        this.setStatus("The reef is alive. Start catching!");
    }

    private startNeon(): void {
        this.setInstruction("Watch the four-tile pattern, then repeat it in the same order.");
        this.sequence = [this.randomTile(), this.randomTile(), this.randomTile()];
        this.showSequence();
    }

    private showSequence(): void {
        this.inputLocked = true;
        this.setTiles(false);
        this.setStatus("Memorize the pattern…");
        const shown = this.sequence.slice();
        shown.forEach((tile, index) => {
            this.scheduleOnce(() => {
                this.setInstruction("Pattern " + (index + 1) + " of " + shown.length + ": " + SYMBOLS[tile]);
            }, index * 0.8);
        });
        this.scheduleOnce(() => {
            this.inputLocked = false;
            this.sequenceIndex = 0;
            this.setTiles(true);
            this.setInstruction("Your turn. Repeat " + this.sequence.length + " tiles.");
            this.setStatus("Choose the first tile.");
        }, shown.length * 0.8 + 0.3);
    }

    private tapTile(index: number): void {
        if (!this.session || this.busy || this.inputLocked || this.gameId !== "neon-reels") return;
        if (this.sequence[index] !== this.sequence[this.sequenceIndex]) {
            this.sequenceIndex = 0;
            this.activity = Math.max(0, this.activity - 1);
            this.updateScore();
            this.setStatus("Not quite. Pattern replaying — try again.");
            this.showSequence();
            return;
        }
        this.sequenceIndex += 1;
        this.activity += 1;
        this.updateScore();
        this.setStatus("Correct tile " + this.sequenceIndex + " of " + this.sequence.length + "!");
        if (this.sequenceIndex >= this.sequence.length) {
            this.sequence.push(this.randomTile());
            this.scheduleOnce(() => {
                if (!this.session) return;
                this.setStatus("Level up!");
                this.showSequence();
            }, 0.5);
        }
    }

    private randomTile(): number {
        return Math.floor(Math.random() * 4);
    }

    private instructions(): string {
        if (this.gameId === "skyline-crash") return "Skyline Dash · Tap fast for 15 seconds.";
        if (this.gameId === "neon-reels") return "Neon Sequence · Memorize and repeat patterns.";
        return "Reef Ranger · Tap to collect friendly fish.";
    }

    private updateScore(): void {
        if (this.scoreLabel) this.scoreLabel.string =
            "LOCAL ACTIVITY  " + this.activity + "\nSERVER SCORE  awarded on completion";
    }

    private setTiles(enabled: boolean): void {
        this.taps.forEach((button, index) => {
            button.interactable = enabled;
            const label = button.getComponentInChildren(Label);
            if (label) label.string = SYMBOLS[index];
        });
    }

    private setButton(button: Button | null, enabled: boolean): void {
        if (button) button.interactable = enabled;
    }

    private setStatus(value: string): void {
        if (this.statusLabel) this.statusLabel.string = value;
    }

    private setInstruction(value: string): void {
        if (this.instructionLabel) this.instructionLabel.string = value;
    }

    private message(error: unknown): string {
        return error instanceof Error ? error.message : "The request could not be completed.";
    }
}
