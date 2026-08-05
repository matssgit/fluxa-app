import { useState } from "react";

export function useHiddenSubscriptions(userId: string = "default") {
  const storageKey = `@fluxa:hidden-subscriptions:${userId}`;

  const [hiddenIds, setHiddenIds] = useState<string[]>(() => {
    try {
      const stored = localStorage.getItem(storageKey);
      return stored ? JSON.parse(stored) : [];
    } catch (error) {
      console.error("Erro ao ler preferências do localStorage", error);
      return [];
    }
  });

  const toggleHide = (subscriptionId: string) => {
    setHiddenIds((prev) => {
      const newHiddenIds = prev.includes(subscriptionId)
        ? prev.filter((id) => id !== subscriptionId)
        : [...prev, subscriptionId];

      localStorage.setItem(storageKey, JSON.stringify(newHiddenIds));
      return newHiddenIds;
    });
  };

  return { hiddenIds, toggleHide };
}
