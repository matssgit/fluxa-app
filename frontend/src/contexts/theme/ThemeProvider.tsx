import type { ReactNode } from "react";
import { useAuth } from "../../hooks/useAuth";
import type { Theme } from "./ThemeContext";
import { useEffect, useState } from "react";
import { ThemeContext } from "./ThemeContext";

interface ThemeProviderProps {
  children: ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const { user, updatePreferences } = useAuth();

  const [localTheme, setLocalTheme] = useState<Theme>(() => {
    if (typeof window === "undefined") return "system";
    const savedTheme = localStorage.getItem("fluxa-theme") as Theme | null;
    return savedTheme || "system";
  });

  const resolvedTheme: Theme =
    (user?.preferences?.theme as Theme) || localTheme;

  useEffect(() => {
    if (typeof window === "undefined") return;

    const root = document.documentElement;

    localStorage.setItem("fluxa-theme", resolvedTheme);

    const applyTheme = (theme: "light" | "dark") => {
      if (theme === "dark") {
        root.classList.add("dark");
      } else {
        root.classList.remove("dark");
      }
    };

    if (resolvedTheme === "system") {
      const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
      applyTheme(mediaQuery.matches ? "dark" : "light");

      const handleChange = (e: MediaQueryListEvent) => {
        applyTheme(e.matches ? "dark" : "light");
      };

      mediaQuery.addEventListener("change", handleChange);
      return () => mediaQuery.removeEventListener("change", handleChange);
    } else {
      applyTheme(resolvedTheme);
    }
  }, [resolvedTheme]);

  const handleSetTheme = async (newTheme: Theme) => {
    setLocalTheme(newTheme);
    if (user) {
      await updatePreferences({ theme: newTheme });
    }
  };

  const toggleTheme = () => {
    const isDark =
      resolvedTheme === "dark" ||
      (resolvedTheme === "system" &&
        window.matchMedia("(prefers-color-scheme: dark)").matches);
    handleSetTheme(isDark ? "light" : "dark");
  };

  return (
    <ThemeContext.Provider
      value={{
        theme: resolvedTheme,
        setTheme: handleSetTheme,
        toggleTheme,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}
