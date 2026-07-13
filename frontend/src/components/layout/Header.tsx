import { useState, useRef, useEffect } from "react";
import { useLocation, Link } from "react-router-dom";
import {
  LogOut,
  ShieldCheck,
  User,
  Tag,
  Settings,
  ChevronDown,
  Repeat,
} from "lucide-react";
import { useAuth } from "../../hooks/useAuth";

const PAGE_TITLES: Record<string, { title: string; subtitle: string }> = {
  "/dashboard": {
    title: "INÍCIO",
    subtitle: "Seu ecossistema analítico e saúde do patrimônio",
  },
  "/transactions": {
    title: "CAIXA & TRANSAÇÕES",
    subtitle: "Operação diária, conciliações e fluxo de entradas e saídas",
  },
  "/cards": {
    title: "CARTÕES DE CRÉDITO",
    subtitle: "Gestão de cartões, faturas abertas e parcelas",
  },
  "/subscriptions": {
    title: "ASSINATURAS & SERVIÇOS",
    subtitle: "Monitoramento de serviços fixos e cobranças periódicas",
  },
  "/accounts": {
    title: "CONTAS & CARTEIRAS",
    subtitle: "Saldo bancário consolidado e liquidez imediata",
  },
  "/wallets": {
    title: "OBJETIVOS FINANCEIROS",
    subtitle: "Organize reservas, metas de curto prazo e patrimônio futuro",
  },
  "/settings": {
    title: "CONFIGURAÇÕES",
    subtitle: "Preferências globais e administração do ecossistema",
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
    : { title: "Fluxa", subtitle: "Cultive uma vida financeira saudável" };

  const getUserInitials = (name?: string): string => {
    if (!name) return "MS";
    const parts = name.trim().split(" ");
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  return (
    <header className="h-20 w-full bg-surface/75 backdrop-blur-md border-b border-subtle/20 sticky top-0 z-30 px-4 sm:px-6 lg:px-10 flex items-center justify-between transition-all duration-300 shadow-[0_4px_20px_rgba(0,0,0,0.02)]">
      <div className="space-y-0.5 min-w-0 pr-4">
        <div className="flex items-center gap-2">
          <h1 className="text-base sm:text-lg font-extrabold text-primary tracking-tight leading-none truncate">
            {pageInfo.title}
          </h1>
          <span className="w-1.5 h-1.5 rounded-full bg-brand hidden sm:inline-block shrink-0" />
        </div>
        <p className="text-xs font-medium text-muted hidden sm:block truncate">
          {pageInfo.subtitle}
        </p>
      </div>

      <div className="flex items-center gap-3 sm:gap-4 shrink-0">
        <div className="hidden md:flex items-center gap-1.5 px-3 py-1 rounded-full bg-elevated/60 border border-subtle/30 text-[11px] font-semibold text-secondary shadow-2xs">
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
                {user?.name || "Matheus Santana"}
              </span>
              <span className="text-[10px] font-medium text-muted mt-0.5 block truncate max-w-32">
                {user?.email || "matheus@fluxa.com"}
              </span>
            </div>

            <div className="relative">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-linear-to-tr from-brand to-emerald-600 text-white font-extrabold text-xs sm:text-sm flex items-center justify-center shadow-sm border border-white/20 group-hover:scale-105 transition-transform duration-200 ring-2 ring-transparent group-hover:ring-brand/30">
                {getUserInitials(user?.name)}
              </div>
              <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-500 border-2 border-surface rounded-full" />
            </div>

            <ChevronDown
              className={`w-4 h-4 text-muted transition-transform duration-200 hidden sm:block ${
                isMenuOpen ? "rotate-180 text-primary" : ""
              }`}
            />
          </button>

          {isMenuOpen && (
            <div className="absolute right-0 mt-3 w-56 bg-surface/95 backdrop-blur-xl rounded-2xl shadow-xl border border-subtle/30 py-2 animate-fade-in z-50 divide-y divide-subtle/10">
              <div className="px-4 py-3 sm:hidden">
                <p className="text-xs font-bold text-primary truncate">
                  {user?.name || "Matheus Santana"}
                </p>
                <p className="text-[10px] font-medium text-muted truncate">
                  {user?.email || "matheus@fluxa.com"}
                </p>
              </div>

              <div className="py-1">
                {/* ✨ Assinaturas injetada apenas para usuários de celular/tablet! */}
                <Link
                  to="/subscriptions"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors min-[1400px]:hidden"
                >
                  <Repeat className="w-4 h-4 text-brand" />
                  <span>Assinaturas</span>
                </Link>

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
                  className="flex items-center gap-3 px-4 py-2.5 text-xs font-semibold text-secondary hover:bg-elevated/80 hover:text-primary transition-colors min-[1400px]:hidden"
                >
                  <Settings className="w-4 h-4 text-muted" />
                  <span>Configurações globais</span>
                </Link>
              </div>

              <div className="pt-1">
                <button
                  onClick={() => {
                    setIsMenuOpen(false);
                    signOut();
                  }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-xs font-bold text-red-500 hover:bg-red-500/10 transition-colors text-left cursor-pointer"
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
