import { createContext } from "react";

export interface PrivacyContextData {
  hideBalance: boolean;
}

export const PrivacyContext = createContext<PrivacyContextData>({
  hideBalance: false,
});
