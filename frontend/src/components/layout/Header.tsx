import { Link } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { useTheme } from "../../hooks/useTheme";
import { useState, useRef, useEffect } from "react";
import {
  LogOut,
  ShieldCheck,
  Tag,
  Settings,
  ChevronDown,
  Repeat,
  Sun,
  Moon,
} from "lucide-react";

export function Header() {
  const { user, signOut } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent): void {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const getUserInitials = (name?: string): string => {
    if (!name) return "US";
    const parts = name.trim().split(" ");
    if (parts.length >= 2)
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    return name.substring(0, 2).toUpperCase();
  };

  const firstName = user?.name?.trim().split(" ")[0] || "você";

  return (
    <header className="min-h-22 w-full bg-background/72 backdrop-blur-xl sticky top-0 z-30 px-4 sm:px-7 lg:px-10 xl:px-12 flex items-center justify-between transition-all duration-300">
      <div className="min-w-0 pr-4 select-none">
        <span className="text-[9px] sm:text-[10px] font-extrabold uppercase tracking-[0.2em] text-muted block mb-1">
          Seu espaço financeiro
        </span>
        <h1 className="text-lg sm:text-xl font-extrabold text-primary tracking-[-0.035em] leading-none truncate">
          Olá, {firstName}.
        </h1>
      </div>

      <div className="flex items-center gap-3 sm:gap-4 shrink-0">
        <button
          onClick={toggleTheme}
          className="w-11 h-11 rounded-full bg-surface hover:bg-elevated text-secondary transition-all duration-300 cursor-pointer flex items-center justify-center shadow-xs border border-border"
          aria-label="Alternar tema"
          title={theme === "light" ? "Ativar Dark Mode" : "Ativar Light Mode"}
        >
          {theme === "light" ? (
            <Moon size={18} className="animate-scale-in" />
          ) : (
            <Sun size={18} className="animate-scale-in" />
          )}
        </button>

        <div className="hidden md:flex items-center gap-1.5 px-3.5 py-2 rounded-[1rem_1rem_1rem_0.3rem] bg-income-soft text-[11px] font-bold text-income">
          <ShieldCheck className="w-3.5 h-3.5 text-brand" />
          <span>Ambiente Seguro</span>
        </div>

        <div
          className="relative pl-2 sm:pl-3 border-l border-subtle/20"
          ref={menuRef}
        >
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center gap-2.5 sm:gap-3 p-1 rounded-2xl hover:bg-elevated/60 transition-all duration-200 cursor-pointer select-none group focus:outline-none"
          >
            <div className="text-right hidden sm:block">
              <span className="text-xs font-bold text-primary block leading-none group-hover:text-brand transition-colors">
                {user?.name || "Usuário"}
              </span>
              <span className="text-[10px] font-medium text-muted mt-0.5 block truncate max-w-32">
                {user?.email}
              </span>
            </div>

            <div className="relative">
              {user?.avatar_url ? (
                <img
                  src={user.avatar_url}
                  alt={user.name}
                  className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl object-cover shadow-sm border border-subtle/30 group-hover:scale-105 transition-transform duration-200 ring-2 ring-transparent group-hover:ring-brand/30"
                />
              ) : (
                <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-brand text-white dark:text-[#102f29] font-extrabold text-xs sm:text-sm flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform duration-200 ring-2 ring-transparent group-hover:ring-brand/20">
                  {getUserInitials(user?.name)}
                </div>
              )}
              <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-income border-2 border-surface rounded-full" />
            </div>

            <ChevronDown
              className={`w-4 h-4 text-muted transition-transform duration-200 hidden sm:block ${isMenuOpen ? "rotate-180 text-primary" : ""}`}
            />
          </button>

          {isMenuOpen && (
            <div className="absolute right-0 mt-3 w-56 bg-surface/95 backdrop-blur-xl rounded-2xl shadow-xl border border-subtle/30 py-2 animate-fade-in z-50 divide-y divide-subtle/10">
              <div className="px-4 py-3 sm:hidden">
                <p className="text-xs font-bold text-primary truncate">
                  {user?.name || "Usuário"}
                </p>
                <p className="text-[10px] font-medium text-muted truncate">
                  {user?.email}
                </p>
              </div>

              <div className="py-1">
                <Link
                  to="/categories"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors xl:hidden"
                >
                  <Tag className="w-4 h-4 text-muted" />
                  <span>Categorias e Tags</span>
                </Link>
                <Link
                  to="/subscriptions"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors xl:hidden"
                >
                  <Repeat className="w-4 h-4 text-brand" />
                  <span>Assinaturas</span>
                </Link>
                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors"
                >
                  <Settings className="w-4 h-4 text-muted" />
                  <span>Configurações</span>
                </Link>
              </div>

              <div className="pt-1">
                <button
                  onClick={() => {
                    setIsMenuOpen(false);
                    signOut();
                  }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-xs font-bold text-expense hover:bg-expense-soft transition-colors text-left cursor-pointer"
                >
                  <LogOut className="w-4 h-4" />
                  <span>Sair do Sistema</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
