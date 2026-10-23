import { _decorator, Component, EditBox, Label, Node } from "cc";
import { ApiClient } from "./ApiClient";
import { AuthService } from "./AuthService";
import { LobbyScreen } from "./LobbyScreen";

const { ccclass, property } = _decorator;

/** Auth screen controller; assign all fields and the lobby root in Creator's Inspector. */
@ccclass("AuthScreen")
export class AuthScreen extends Component {
    @property({ tooltip: "HTTPS API origin in production; localhost is for local development only." })
    apiBaseUrl = "http://localhost:8080";

    @property(EditBox) emailInput: EditBox | null = null;
    @property(EditBox) passwordInput: EditBox | null = null;
    @property(Label) statusLabel: Label | null = null;
    @property(Node) authRoot: Node | null = null;
    @property(Node) lobbyRoot: Node | null = null;

    private busy = false;

    onLoad(): void {
        ApiClient.configure(this.apiBaseUrl);
        if (!/^https:\/\//i.test(this.apiBaseUrl) &&
            !/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(this.apiBaseUrl)) {
            this.show("Security warning: configure an HTTPS API URL before using this client outside local development.");
        } else {
            this.show("Welcome to Arcade Lab. Sign in or create a demo account.");
        }
        if (this.lobbyRoot) this.lobbyRoot.active = false;
    }

    async register(): Promise<void> {
        if (this.busy) return;
        const email = this.emailInput?.string.trim() ?? "";
        const password = this.passwordInput?.string ?? "";
        if (!this.validEmail(email)) {
            this.show("Enter a valid email address.");
            return;
        }
        if (password.length < 12 || password.length > 72) {
            this.show("Use a password between 12 and 72 characters.");
            return;
        }
        this.busy = true;
        this.show("Creating your account…");
        try {
            const result = await AuthService.register(email, password);
            if (this.passwordInput) this.passwordInput.string = "";
            this.show(result.status === "VERIFICATION_REQUIRED"
                ? "Account created. Check your email and verify before signing in. Starting demo credits: " + result.startingDemoCredits + "."
                : "Account created. Starting demo credits: " + result.startingDemoCredits + ". Sign in to continue.");
        } catch (error) {
            this.show(this.message(error));
        } finally {
            this.busy = false;
        }
    }

    async requestPasswordReset(): Promise<void> {
        if (this.busy) return;
        const email = this.emailInput?.string.trim() ?? "";
        if (!this.validEmail(email)) {
            this.show("Enter your account email first.");
            return;
        }
        this.busy = true;
        this.show("Requesting password-reset instructions…");
        try {
            const result = await AuthService.requestPasswordReset(email);
            this.show(result.message || "If the account exists, reset instructions will be sent.");
        } catch (error) {
            this.show(this.message(error));
        } finally {
            this.busy = false;
        }
    }

    async login(): Promise<void> {
        if (this.busy) return;
        const email = this.emailInput?.string.trim() ?? "";
        const password = this.passwordInput?.string ?? "";
        if (!this.validEmail(email) || !password) {
            this.show("Enter a valid email and password.");
            return;
        }
        this.busy = true;
        this.show("Signing in securely…");
        try {
            await AuthService.login(email, password);
            const profile = await AuthService.profile();
            if (this.passwordInput) this.passwordInput.string = "";
            this.show("Welcome, " + profile.email + ". Opening your arcade…");
            if (this.lobbyRoot) {
                this.lobbyRoot.active = true;
                const lobby = this.lobbyRoot.getComponent(LobbyScreen);
                if (lobby) await lobby.refresh();
                if (this.authRoot) this.authRoot.active = false;
            }
        } catch (error) {
            AuthService.clearSession();
            this.show(this.message(error));
        } finally {
            this.busy = false;
        }
    }

    private validEmail(email: string): boolean {
        return email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    private show(message: string): void {
        if (this.statusLabel) this.statusLabel.string = message;
    }

    private message(error: unknown): string {
        return error instanceof Error ? error.message : "The request could not be completed.";
    }
}
