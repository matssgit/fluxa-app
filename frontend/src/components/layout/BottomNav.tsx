import { Link, useLocation } from "react-router-dom";
import {
  Home,
  ArrowLeftRight,
  CreditCard,
  Wallet,
  Target,
  Repeat,
  Tag,
  type LucideIcon,
} from "lucide-react";

interface MobileNavItem {
  label: string;
  path: string;
  icon: LucideIcon;
  isPrimary?: boolean;
}

const MOBILE_NAV_ITEMS: MobileNavItem[] = [
  { label: "Início", path: "/dashboard", icon: Home },
  { label: "Contas", path: "/accounts", icon: Wallet },
  { label: "Categorias", path: "/categories", icon: Tag },
  {
    label: "Caixa",
    path: "/transactions",
    icon: ArrowLeftRight,
    isPrimary: true,
  },
  { label: "Cartões", path: "/cards", icon: CreditCard },
  { label: "Assinaturas", path: "/subscriptions", icon: Repeat },
  { label: "Metas", path: "/wallets", icon: Target },
];

export function BottomNav() {
  const location = useLocation();

  return (
    <nav className="xl:hidden fixed bottom-3 left-3 right-3 z-50 bg-surface/90 backdrop-blur-xl border border-border shadow-md px-1 sm:px-2 py-2 rounded-[1.75rem_1.75rem_1.75rem_0.65rem] transition-all duration-300 tour-bottom-nav">
      <div className="flex items-center justify-around transition-all duration-300 max-w-lg mx-auto">
        {MOBILE_NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);

          // Renderização do Botão Principal (CAIXA)
          if (item.isPrimary) {
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`relative flex flex-col items-center justify-center min-w-13 h-12 rounded-2xl px-1 py-1 mx-0.5 transition-all duration-300 select-none ${
                  isActive
                    ? "bg-brand text-white dark:text-[#102f29] shadow-sm"
                    : "bg-elevated text-secondary hover:text-primary"
                }`}
              >
                <Icon className="w-5 h-5 mb-0.5" />
                <span className="text-[9px] font-extrabold tracking-wide">
                  {item.label}
                </span>
              </Link>
            );
          }

          // Renderização Padrão
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`relative flex flex-col items-center justify-center min-w-12 min-h-12 rounded-xl px-0.5 py-1 transition-all duration-200 select-none ${
                isActive
                  ? "text-brand font-bold"
                  : "text-muted hover:text-secondary"
              }`}
            >
              {isActive && (
                <span className="absolute inset-0 bg-brand/10 rounded-2xl -z-10 animate-fade-in" />
              )}
              <Icon
                className={`w-5 h-5 transition-transform duration-200 ${
                  isActive ? "text-brand scale-110" : "text-muted"
                }`}
              />
              <span
                className={`text-[8.5px] sm:text-[9px] tracking-tight mt-1 transition-colors duration-200 ${
                  isActive
                    ? "text-brand font-extrabold"
                    : "text-muted font-medium"
                }`}
              >
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
