import { BrowserDispatcher } from "./browser-dispatcher";
import type { NotificationDispatcher } from "./dispatcher";
import type { Subscription } from "../../types/subscription";

export class NotificationScheduler {
  private dispatcher: NotificationDispatcher;

  constructor() {
    this.dispatcher = new BrowserDispatcher();
  }

  public async checkAndNotifySubscriptions(subscriptions: Subscription[]) {
    const today = new Date().toISOString().split("T")[0];
    const lastCheck = localStorage.getItem("@Fluxa:last_sub_check");

    if (lastCheck === today) return;

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowDay = tomorrow.getDate();

    const dueTomorrow = subscriptions.filter(
      (sub) => sub.status === "active" && sub.due_day === tomorrowDay,
    );

    for (const sub of dueTomorrow) {
      await this.dispatcher.send({
        title: "Assinatura a Vencer 🔄",
        body: `A assinatura ${sub.title} vence amanhã (Dia ${sub.due_day}). Certifique-se de ter limite ou saldo!`,
      });
    }

    localStorage.setItem("@Fluxa:last_sub_check", today);
  }
}
