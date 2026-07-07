import { Link, useLocation } from "react-router-dom";
import { Home, ArrowLeftRight, CreditCard, Repeat, Wallet } from "lucide-react";

// Constante interna para manter o Fast Refresh feliz
const MOBILE_NAV_ITEMS = [
  { label: "Início", path: "/dashboard", icon: Home },
  { label: "Caixa", path: "/transactions", icon: ArrowLeftRight },
  { label: "Cartões", path: "/cards", icon: CreditCard },
  { label: "Assinaturas", path: "/subscriptions", icon: Repeat },
  { label: "Contas", path: "/accounts", icon: Wallet },
];

export function BottomNav() {
  const location = useLocation();

  return (
    <nav className="lg:hidden fixed bottom-4 left-4 right-4 z-50">
      {/* Pílula Flutuante com Efeito Neumórfico/Vidro Acetinado */}
      <div className="bg-surface/85 backdrop-blur-md border border-white/60 dark:border-subtle/30 rounded-2xl shadow-md px-2 py-2 flex items-center justify-around transition-all duration-300">
        {MOBILE_NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`relative flex flex-col items-center justify-center min-w-14 min-h-12.5 rounded-xl px-2 py-1 transition-all duration-200 select-none ${
                isActive
                  ? "text-brand font-bold scale-105"
                  : "text-muted hover:text-secondary"
              }`}
            >
              {/* Brilho de Fundo no Item Ativo */}
              {isActive && (
                <span className="absolute inset-0 bg-brand/10 rounded-xl -z-10 animate-fade-in" />
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
