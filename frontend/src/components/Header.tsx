import { useState, useRef, useEffect } from "react";
import { useLocation, Link } from "react-router-dom";
import {
  LogOut,
  ShieldCheck,
  User,
  Tag,
  Palette,
  Database,
  Settings,
  ChevronDown,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";

const PAGE_TITLES: Record<string, { title: string; subtitle: string }> = {
  "/dashboard": {
    title: "Início",
    subtitle: "Central analítica, projeções e inteligência financeira",
  },
  "/transactions": {
    title: "Caixa",
    subtitle: "Operação diária, conciliações e fluxo de entradas e saídas",
  },
  "/cards": {
    title: "Ecossistema de Crédito",
    subtitle: "Gestão de cartões, faturas abertas e parcelas",
  },
  "/subscriptions": {
    title: "Assinaturas Recorrentes",
    subtitle: "Monitoramento de serviços e cobranças periódicas",
  },
  "/accounts": {
    title: "Contas & Carteiras",
    subtitle: "Saldo bancário consolidado e liquidez imediata",
  },
  "/settings": {
    title: "Configurações",
    subtitle: "Preferências globais e administração da conta",
  },
};

export function Header() {
  const location = useLocation();
  const { user, signOut } = useAuth();
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

  const currentPathKey = Object.keys(PAGE_TITLES).find((key) =>
    location.pathname.startsWith(key),
  );

  const pageInfo = currentPathKey
    ? PAGE_TITLES[currentPathKey]
    : { title: "Finance App", subtitle: "Gestão financeira inteligente" };

  const getUserInitials = (name?: string): string => {
    if (!name) return "MS";
    const parts = name.trim().split(" ");
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  return (
    <header className="h-20 w-full bg-surface/80 backdrop-blur-md border-b border-subtle/20 sticky top-0 z-30 px-6 lg:px-10 flex items-center justify-between transition-all duration-300">
      <div className="space-y-0.5">
        <h1 className="text-base lg:text-lg font-bold text-primary tracking-tight leading-none">
          {pageInfo.title}
        </h1>
        <p className="text-xs font-medium text-muted hidden sm:block">
          {pageInfo.subtitle}
        </p>
      </div>

      <div className="flex items-center gap-4">
        <div className="hidden md:flex items-center gap-1.5 px-3 py-1 rounded-full bg-elevated/80 border border-subtle/30 text-[11px] font-semibold text-secondary">
          <ShieldCheck className="w-3.5 h-3.5 text-brand" />
          <span>Ambiente Protegido</span>
        </div>

        <div className="relative pl-2 border-l border-subtle/20" ref={menuRef}>
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center gap-3 p-1 rounded-2xl hover:bg-elevated/60 transition-all duration-200 cursor-pointer select-none group"
          >
            <div className="text-right hidden sm:block">
              <span className="text-xs font-bold text-primary block leading-none group-hover:text-brand transition-colors">
                {user?.name || "Matheus Silva"}
              </span>
              <span className="text-[10px] font-medium text-muted mt-0.5 block">
                {user?.email || "matheus@financeapp.com"}
              </span>
            </div>

            <div className="w-10 h-10 rounded-xl bg-brand text-white font-extrabold text-sm flex items-center justify-center shadow-sm border border-brand-light/30 group-hover:scale-105 transition-transform duration-200">
              {getUserInitials(user?.name)}
            </div>

            <ChevronDown
              className={`w-4 h-4 text-muted transition-transform duration-200 hidden sm:block ${
                isMenuOpen ? "rotate-180 text-primary" : ""
              }`}
            />
          </button>

          {isMenuOpen && (
            <div className="absolute right-0 mt-3 w-56 bg-surface/95 backdrop-blur-md rounded-2xl shadow-xl border border-subtle/30 py-2 animate-fade-in z-50">
              <div className="px-4 py-3 border-b border-subtle/20 sm:hidden">
                <p className="text-xs font-bold text-primary truncate">
                  {user?.name || "Matheus Silva"}
                </p>
                <p className="text-[10px] font-medium text-muted truncate">
                  {user?.email || "matheus@financeapp.com"}
                </p>
              </div>

              <div className="py-1">
                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors"
                >
                  <User className="w-4 h-4 text-brand" />
                  <span>Meu Perfil</span>
                </Link>

                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors"
                >
                  <Tag className="w-4 h-4 text-muted" />
                  <span>Categorias & Tags</span>
                </Link>

                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors"
                >
                  <Palette className="w-4 h-4 text-muted" />
                  <span>Tema (Dark / Light)</span>
                </Link>

                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors"
                >
                  <Database className="w-4 h-4 text-muted" />
                  <span>Backup & Dados</span>
                </Link>

                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors lg:hidden"
                >
                  <Settings className="w-4 h-4 text-muted" />
                  <span>Configurações</span>
                </Link>
              </div>

              <div className="border-t border-subtle/20 mt-1 pt-1">
                {/* Botão padronizado com token text-danger e hover:bg-danger/10 */}
                <button
                  onClick={() => {
                    setIsMenuOpen(false);
                    signOut();
                  }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-xs font-bold text-danger hover:bg-danger/10 transition-colors text-left cursor-pointer"
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
