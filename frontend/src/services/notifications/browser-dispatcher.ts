// frontend/src/services/notifications/browser-dispatcher.ts
import type { NotificationDispatcher, NotificationPayload } from "./dispatcher";

export class BrowserDispatcher implements NotificationDispatcher {
  async send(payload: NotificationPayload): Promise<void> {
    // Falha silenciosa caso o browser não suporte
    if (!("Notification" in window)) return;

    if (Notification.permission === "granted") {
      const notification = new Notification(payload.title, {
        body: payload.body,
        icon: payload.icon || "/favicon.ico",
      });

      if (payload.actionUrl) {
        notification.onclick = () => {
          window.focus();
        };
      }
    }
  }
}
