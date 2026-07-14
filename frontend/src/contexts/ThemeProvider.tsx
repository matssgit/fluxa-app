import { useState, useEffect } from "react";
import type { ReactNode } from "react";

import { ThemeContext } from "./ThemeContext";
import type { Theme } from "./ThemeContext";

interface ThemeProviderProps {
  children: ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const [theme, setTheme] = useState<Theme>(() => {
    // SSR Safe
    if (typeof window === "undefined") return "light";

    const savedTheme = localStorage.getItem("fluxa-theme") as Theme | null;
    if (savedTheme) return savedTheme;

    // Fallback SO
    if (
      window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches
    ) {
      return "dark";
    }

    return "light";
  });

  // Efeito 1: Sincronização de DOM e LocalStorage
  useEffect(() => {
    if (typeof window === "undefined") return;

    const root = document.documentElement;
    if (theme === "dark") {
      root.classList.add("dark");
    } else {
      root.classList.remove("dark");
    }

    localStorage.setItem("fluxa-theme", theme);
  }, [theme]);

  // Efeito 2: Listener do SO (Reatividade)
  useEffect(() => {
    if (typeof window === "undefined") return;

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const handleChange = (e: MediaQueryListEvent) => {
      if (!localStorage.getItem("fluxa-theme")) {
        setTheme(e.matches ? "dark" : "light");
      }
    };

    mediaQuery.addEventListener("change", handleChange);

    return () => {
      mediaQuery.removeEventListener("change", handleChange);
    };
  }, []);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "light" ? "dark" : "light"));
  };

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
