// frontend/src/services/notifications/permission-manager.ts

export class PermissionManager {
  static async requestPermission(): Promise<boolean> {
    if (!("Notification" in window)) return false;

    if (Notification.permission === "granted") return true;

    if (Notification.permission !== "denied") {
      const permission = await Notification.requestPermission();
      return permission === "granted";
    }

    return false;
  }

  static hasPermission(): boolean {
    if (!("Notification" in window)) return false;
    return Notification.permission === "granted";
  }
}
