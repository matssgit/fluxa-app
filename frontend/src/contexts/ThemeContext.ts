import { createContext } from "react";

export type Theme = "light" | "dark";

export interface ThemeContextData {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextData | undefined>(undefined);