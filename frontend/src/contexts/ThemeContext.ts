import { createContext } from "react";


export type Theme = "light" | "dark" | "system";

export interface ThemeContextData {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextData>(
  {} as ThemeContextData
);