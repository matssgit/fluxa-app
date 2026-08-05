import { useEffect } from "react";
import { useAuth } from "./useAuth";
import { useSubscriptions } from "./useSubscriptions";
import { NotificationScheduler } from "../lib/notifications/scheduler";
import { PermissionManager } from "../lib/notifications/permission-manager";

export function useNotificationSystem() {
  const { user } = useAuth();
  const { data: subscriptions } = useSubscriptions();

  useEffect(() => {
    if (!user || !user.preferences.notifications?.subscriptions_enabled) return;
    if (!subscriptions || subscriptions.length === 0) return;

    const initNotifications = async () => {
      const hasPermission = await PermissionManager.requestPermission();

      if (hasPermission) {
        const scheduler = new NotificationScheduler();
        await scheduler.checkAndNotifySubscriptions(subscriptions);
      }
    };

    initNotifications();
  }, [user, subscriptions]);
}
