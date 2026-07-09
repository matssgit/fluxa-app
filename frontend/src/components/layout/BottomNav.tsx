import { Link, useLocation } from "react-router-dom";
import {
  Home,
  ArrowLeftRight,
  CreditCard,
  Repeat,
  Wallet,
  type LucideIcon,
} from "lucide-react";

interface MobileNavItem {
  label: string;
  path: string;
  icon: LucideIcon;
}

const MOBILE_NAV_ITEMS: MobileNavItem[] = [
  { label: "Início", path: "/dashboard", icon: Home },
  { label: "Caixa", path: "/transactions", icon: ArrowLeftRight },
  { label: "Cartões", path: "/cards", icon: CreditCard },
  { label: "Assinaturas", path: "/subscriptions", icon: Repeat },
  { label: "Contas", path: "/accounts", icon: Wallet },
];

export function BottomNav() {
  const location = useLocation();

  return (
    /* ✨ GLASSMORPHISM RAIZ: Vidro fosco real (backdrop-blur-xl) na sua barra original sem duplicar nada! */
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-surface/60 dark:bg-surface/70 backdrop-blur-xl border-t border-white/40 dark:border-subtle/30 shadow-[0_-10px_30px_rgba(0,0,0,0.08)] px-4 py-2.5 transition-all duration-300">
      <div className="flex items-center justify-around transition-all duration-300 max-w-md mx-auto">
        {MOBILE_NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`relative flex flex-col items-center justify-center min-w-14 min-h-12 rounded-xl px-2 py-1 transition-all duration-200 select-none ${
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
                className={`text-[10px] tracking-tight mt-1 transition-colors duration-200 ${
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
