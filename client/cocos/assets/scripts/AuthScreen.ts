import { _decorator, Component, EditBox, Label } from "cc";
import { ApiClient } from "./ApiClient";
import { AuthService } from "./AuthService";

const { ccclass, property } = _decorator;

/** Attach to a scene node and wire EditBoxes/Label in Creator's Inspector. */
@ccclass("AuthScreen")
export class AuthScreen extends Component {
    @property({ tooltip: "HTTPS API origin in production; localhost is for local development only." })
    apiBaseUrl = "http://localhost:8080";

    @property(EditBox) emailInput: EditBox | null = null;
    @property(EditBox) passwordInput: EditBox | null = null;
    @property(Label) statusLabel: Label | null = null;

    onLoad(): void {
        ApiClient.configure(this.apiBaseUrl);
        this.show("Sign in or create a demo account.");
    }

    async register(): Promise<void> {
        const email = this.emailInput?.string.trim() ?? "";
        const password = this.passwordInput?.string ?? "";
        if (!email || !password) {
            this.show("Enter your email and password.");
            return;
        }
        try {
            const result = await AuthService.register(email, password);
            this.show(result.status === "VERIFICATION_REQUIRED"
                ? "Account created. Check your email and verify before signing in. Demo credits: " + result.startingDemoCredits + "."
                : "Account created. Demo credits: " + result.startingDemoCredits + ". Sign in to continue.");
        } catch (error) {
            this.show(this.message(error));
        }
    }

    async login(): Promise<void> {
        const email = this.emailInput?.string.trim() ?? "";
        const password = this.passwordInput?.string ?? "";
        if (!email || !password) {
            this.show("Enter your email and password.");
            return;
        }
        try {
            await AuthService.login(email, password);
            const profile = await AuthService.profile();
            this.show("Welcome, " + profile.email + ". Your demo session is ready.");
        } catch (error) {
            this.show(this.message(error));
        }
    }

    private show(message: string): void {
        if (this.statusLabel) this.statusLabel.string = message;
    }

    private message(error: unknown): string {
        return error instanceof Error ? error.message : "The request could not be completed.";
    }
}
