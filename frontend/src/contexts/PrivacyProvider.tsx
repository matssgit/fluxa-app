import type { ReactNode } from "react";
import { PrivacyContext } from "./PrivacyContext";
import { useAuth } from "../hooks/useAuth";

interface PrivacyProviderProps {
  children: ReactNode;
}

export function PrivacyProvider({ children }: PrivacyProviderProps) {
  const { user } = useAuth();

  // Acesso seguro. Se o utilizador não tiver preferência, mostra o saldo.
  const hideBalance = user?.preferences?.privacy?.hide_balance ?? false;

  return (
    <PrivacyContext.Provider value={{ hideBalance }}>
      {children}
    </PrivacyContext.Provider>
  );
}
