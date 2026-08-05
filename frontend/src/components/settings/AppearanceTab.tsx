import { useTheme } from "../../hooks/useTheme";
import { Sun, Moon, Monitor } from "lucide-react";

export function AppearanceTab() {
  const { theme, setTheme } = useTheme();

  return (
    <div className="space-y-8 animate-fade-in max-w-2xl">
      <div>
        <h2 className="text-lg font-bold text-primary">Aparência</h2>
        <p className="text-xs sm:text-sm text-muted mt-1">
          Personalize a interface do Fluxa para o seu conforto visual.
        </p>
      </div>

      <div className="space-y-5">
        <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
          Tema da Interface
        </label>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
          <button
            onClick={() => setTheme("light")}
            className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
              theme === "light"
                ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
            }`}
          >
            <div
              className={`p-2 rounded-xl mb-3 transition-colors ${
                theme === "light"
                  ? "bg-brand text-white"
                  : "bg-elevated text-muted"
              }`}
            >
              <Sun size={20} />
            </div>
            <span
              className={`text-sm font-bold block ${
                theme === "light" ? "text-primary" : "text-secondary"
              }`}
            >
              Claro
            </span>
            <span className="text-[11px] font-medium text-muted mt-0.5 block">
              Fundo branco e limpo
            </span>
          </button>

          <button
            onClick={() => setTheme("dark")}
            className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
              theme === "dark"
                ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
            }`}
          >
            <div
              className={`p-2 rounded-xl mb-3 transition-colors ${
                theme === "dark"
                  ? "bg-brand text-white"
                  : "bg-elevated text-muted"
              }`}
            >
              <Moon size={20} />
            </div>
            <span
              className={`text-sm font-bold block ${
                theme === "dark" ? "text-primary" : "text-secondary"
              }`}
            >
              Escuro
            </span>
            <span className="text-[11px] font-medium text-muted mt-0.5 block">
              Modo Pine & Sage
            </span>
          </button>

          <button
            onClick={() => setTheme("system")}
            className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
              theme === "system"
                ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
            }`}
          >
            <div
              className={`p-2 rounded-xl mb-3 transition-colors ${
                theme === "system"
                  ? "bg-brand text-white"
                  : "bg-elevated text-muted"
              }`}
            >
              <Monitor size={20} />
            </div>
            <span
              className={`text-sm font-bold block ${
                theme === "system" ? "text-primary" : "text-secondary"
              }`}
            >
              Sistema
            </span>
            <span className="text-[11px] font-medium text-muted mt-0.5 block">
              Segue o dispositivo
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
