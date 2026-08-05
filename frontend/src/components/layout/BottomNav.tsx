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
    <nav className="min-[1400px]:hidden fixed bottom-0 left-0 right-0 z-50 bg-surface/60 dark:bg-surface/70 backdrop-blur-xl border-t border-white/40 dark:border-subtle/30 shadow-[0_-10px_30px_rgba(0,0,0,0.08)] px-1 sm:px-2 py-2.5 transition-all duration-300 tour-bottom-nav">
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
                className={`relative flex flex-col items-center justify-center min-w-13 h-12 rounded-2xl px-1 py-1 mx-0.5 transition-all duration-300 select-none shadow-md ${
                  isActive
                    ? "bg-brand text-white scale-105"
                    : "bg-primary/90 text-surface/90 hover:bg-brand hover:scale-105"
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
                  ? "text-brand font-bold scale-105"
                  : "text-muted hover:text-secondary"
              }`}
            >
              {isActive && (
                <span className="absolute inset-0 bg-brand/10 rounded-xl -z-10 animate-fade-in shadow-2xs" />
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
