// frontend/src/hooks/useNotificationSystem.ts
import { useEffect } from "react";
import { useAuth } from "./useAuth";
import { useSubscriptions } from "./useSubscriptions";
import { PermissionManager } from "../services/notifications/permission-manager";
import { NotificationScheduler } from "../services/notifications/scheduler";

export function useNotificationSystem() {
  const { user } = useAuth();
  const { data: subscriptions } = useSubscriptions();

  useEffect(() => {
    // 1. Verificações de escape rápidas (Se o utilizador desligou, não fazemos nada)
    if (!user || !user.preferences.notifications?.subscriptions_enabled) return;
    if (!subscriptions || subscriptions.length === 0) return;

    const initNotifications = async () => {
      // 2. Pede permissão nativa ao SO/Browser
      const hasPermission = await PermissionManager.requestPermission();

      if (hasPermission) {
        // 3. Instancia e roda a regra de negócio
        const scheduler = new NotificationScheduler();
        await scheduler.checkAndNotifySubscriptions(subscriptions);
      }
    };

    initNotifications();
  }, [user, subscriptions]);
}
