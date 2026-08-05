import { useContext } from "react";
import { ThemeContext } from "../contexts/theme/ThemeContext";

export function useTheme() {
  const context = useContext(ThemeContext);

  if (context === undefined) {
    throw new Error(
      "useTheme deve ser utilizado dentro de um ThemeProvider. Verifique a raiz da sua aplicação.",
    );
  }

  return context;
}
