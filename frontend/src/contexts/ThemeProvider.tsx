import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { ThemeContext } from "./ThemeContext";
import type { Theme } from "./ThemeContext";
import { useAuth } from "../hooks/useAuth";

interface ThemeProviderProps {
  children: ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const { user, updatePreferences } = useAuth();

  // 1. Fallback para a tela de Login (Lê o cache do navegador)
  const [localTheme, setLocalTheme] = useState<Theme>(() => {
    if (typeof window === "undefined") return "system";
    const savedTheme = localStorage.getItem("fluxa-theme") as Theme | null;
    return savedTheme || "system";
  });

  // 2. A Fonte da Verdade (Banco de Dados > LocalStorage)
  const resolvedTheme: Theme =
    (user?.preferences?.theme as Theme) || localTheme;

  useEffect(() => {
    if (typeof window === "undefined") return;

    const root = document.documentElement;

    // Salva no cache para a próxima vez que abrir a tela de Login
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

  // 3. Função Inteligente: Sabe se deve salvar no Banco ou no Navegador
  const handleSetTheme = async (newTheme: Theme) => {
    setLocalTheme(newTheme); // Atualiza a UI imediatamente (Optimistic Update)
    if (user) {
      await updatePreferences({ theme: newTheme }); // Salva no Banco de Dados silenciosamente
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
