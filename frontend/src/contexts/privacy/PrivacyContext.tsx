import { useContext } from "react";
import { PrivacyContext } from "../../contexts/privacy/PrivacyContext";

export function usePrivacy() {
  const context = useContext(PrivacyContext);

  if (context === undefined) {
    throw new Error("usePrivacy deve ser usado dentro de um PrivacyProvider");
  }

  return context;
}
