// frontend/src/services/notifications/scheduler.ts
import type { Subscription } from "../../types/subscription";
import { BrowserDispatcher } from "./browser-dispatcher";
import type { NotificationDispatcher } from "./dispatcher";

export class NotificationScheduler {
  private dispatcher: NotificationDispatcher;

  constructor() {
    // Injeção de dependência simples. No React Native, injetaremos o ExpoDispatcher aqui.
    this.dispatcher = new BrowserDispatcher();
  }

  public async checkAndNotifySubscriptions(subscriptions: Subscription[]) {
    // 1. Prevenção de Spam: Só verifica uma vez por dia (evita notificar a cada F5)
    const today = new Date().toISOString().split("T")[0];
    const lastCheck = localStorage.getItem("@Fluxa:last_sub_check");

    if (lastCheck === today) return;

    // 2. Lógica de Data: Descobrir o dia de amanhã
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowDay = tomorrow.getDate();

    // 3. Filtro de Negócio: Assinaturas ativas que vencem amanhã
    const dueTomorrow = subscriptions.filter(
      (sub) => sub.status === "active" && sub.due_day === tomorrowDay,
    );

    // 4. Disparo
    for (const sub of dueTomorrow) {
      await this.dispatcher.send({
        title: "Assinatura a Vencer 🔄",
        body: `A assinatura ${sub.title} vence amanhã (Dia ${sub.due_day}). Certifique-se de ter limite ou saldo!`,
      });
    }

    // 5. Marca como verificado hoje
    localStorage.setItem("@Fluxa:last_sub_check", today);
  }
}
