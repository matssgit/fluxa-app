import { Link, useLocation } from "react-router-dom";
import {
  Home,
  ArrowLeftRight,
  CreditCard,
  Repeat,
  Wallet,
  Settings,
  Target,
  Tag,
  type LucideIcon,
} from "lucide-react";

interface NavItem {
  label: string;
  path: string;
  icon: LucideIcon;
}

const NAV_ITEMS: NavItem[] = [
  { label: "Início", path: "/dashboard", icon: Home },
  { label: "Caixa", path: "/transactions", icon: ArrowLeftRight },
  { label: "Contas", path: "/accounts", icon: Wallet },
  { label: "Categorias", path: "/categories", icon: Tag },
  { label: "Cartões", path: "/cards", icon: CreditCard },
  { label: "Assinaturas", path: "/subscriptions", icon: Repeat },
  { label: "Objetivos", path: "/wallets", icon: Target },
];

export function Sidebar() {
  const location = useLocation();

  return (
    <aside className="hidden xl:flex flex-col w-60 h-screen fixed top-0 left-0 bg-surface/82 backdrop-blur-xl z-40 transition-all duration-300 p-3">
      <div className="h-22 flex items-center px-4">
        <div className="flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-2xl bg-[#173f36] flex items-center justify-center text-[#fffdf7] shadow-sm shrink-0">
            <ArrowLeftRight size={19} strokeWidth={2.2} />
          </div>
          <div>
            <span className="font-extrabold text-primary tracking-[-0.04em] text-xl block leading-none">
              Fluxa
            </span>
            <span className="text-[9px] uppercase tracking-[0.22em] text-muted font-bold">
              vida financeira
            </span>
          </div>
        </div>
      </div>

      <nav className="flex-1 px-1 py-4 space-y-1 overflow-y-auto tour-sidebar-nav rounded-[2rem_2rem_2rem_0.75rem] bg-elevated/45 border border-border">
        <div className="px-4 pt-2 pb-3 text-[10px] font-extrabold uppercase tracking-[0.2em] text-muted select-none">
          Seu fluxo
        </div>

        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`group relative flex items-center gap-3 px-3 py-2.5 mx-2 rounded-[1.25rem_1.25rem_1.25rem_0.4rem] text-sm font-semibold transition-all duration-200 select-none ${
                isActive
                  ? "bg-surface text-primary font-bold shadow-sm"
                  : "text-secondary hover:bg-surface/70 hover:text-primary"
              }`}
            >
              <span className={`w-9 h-9 rounded-[0.9rem_0.9rem_0.9rem_0.3rem] flex items-center justify-center transition-colors ${
                isActive ? "bg-brand text-white dark:text-[#102f29]" : "bg-surface text-muted"
              }`}>
                <Icon
                  strokeWidth={1.9}
                  className={`w-4.5 h-4.5 transition-transform duration-200 ${
                  isActive
                    ? "text-white dark:text-[#102f29]"
                    : "text-muted group-hover:text-primary"
                  }`}
                />
              </span>
              <span>{item.label}</span>
              {isActive && <span className="ml-auto w-1.5 h-1.5 rounded-full bg-accent" />}
            </Link>
          );
        })}
      </nav>

      <div className="pt-3 space-y-2">
        <Link
          to="/settings"
          className="flex items-center gap-3 px-4 py-3 rounded-[1.25rem_1.25rem_1.25rem_0.4rem] text-sm font-semibold text-secondary hover:bg-elevated hover:text-primary transition-all duration-200 select-none"
        >
          <Settings className="w-5 h-5 text-muted" />
          <span>Configurações</span>
        </Link>
      </div>
    </aside>
  );
}
