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
    <aside className="hidden min-[1400px]:flex flex-col w-64 h-screen fixed top-0 left-0 bg-surface/90 backdrop-blur-md border-r border-subtle/30 z-40 transition-all duration-300">
      <div className="h-20 flex items-center px-8 border-b border-subtle/20">
        <div className="flex items-center gap-3.5">
          <div className="w-9 h-9 rounded-xl bg-brand/10 border border-brand/20 flex items-center justify-center text-brand font-black text-lg shadow-2xs shrink-0">
            <span role="img" aria-label="Fluxa Sprout" className="select-none">
              🌱
            </span>
          </div>
          <div>
            <span className="font-extrabold text-primary tracking-tight text-base block leading-none">
              Fluxa
            </span>
          </div>
        </div>
      </div>

      <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        <div className="px-4 pb-2 text-[11px] font-extrabold uppercase tracking-[0.18em] text-muted select-none">
          Menu principal
        </div>

        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`group relative flex items-center gap-3.5 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 select-none ${
                isActive
                  ? "bg-brand/10 text-brand font-bold shadow-xs"
                  : "text-secondary hover:bg-elevated/60 hover:text-primary"
              }`}
            >
              {isActive && (
                <span className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-5 bg-brand rounded-r-full animate-fade-in" />
              )}

              <Icon
                className={`w-5 h-5 transition-transform duration-200 group-hover:scale-110 ${
                  isActive
                    ? "text-brand"
                    : "text-muted group-hover:text-primary"
                }`}
              />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-subtle/20 space-y-2">
        <Link
          to="/settings"
          className="flex items-center gap-3.5 px-4 py-3 rounded-xl text-sm font-medium text-secondary hover:bg-elevated/60 hover:text-primary transition-all duration-200 select-none"
        >
          <Settings className="w-5 h-5 text-muted" />
          <span>Configurações</span>
        </Link>
      </div>
    </aside>
  );
}
