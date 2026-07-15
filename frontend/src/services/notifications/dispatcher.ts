// frontend/src/services/notifications/dispatcher.ts

export interface NotificationPayload {
  title: string;
  body: string;
  icon?: string;
  actionUrl?: string;
}

export interface NotificationDispatcher {
  send(payload: NotificationPayload): Promise<void>;
}