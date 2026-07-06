import { Link, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  CreditCard,
  Repeat,
  Settings,
  LogOut,
  Wallet,
  Landmark,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";

export function Sidebar() {
  const { user, signOut } = useAuth();
  const location = useLocation();

  const navItems = [
    { path: "/", icon: LayoutDashboard, label: "Visão Geral" },
    { path: "/cards", icon: CreditCard, label: "Cartões" },
    { path: "/subscriptions", icon: Repeat, label: "Assinaturas" },
    { path: "/accounts", icon: Landmark, label: "Contas" },
  ];

  return (
    <>
      {/* SIDEBAR DESKTOP */}
      <aside className="hidden md:flex flex-col w-64 bg-surface min-h-screen fixed left-0 top-0 border-r border-border z-50">
        {/* Logo */}
        <div className="p-6 flex items-center gap-3 border-b border-subtle">
          <div className="bg-brand p-2 rounded-lg shadow-xs">
            <Wallet className="text-surface" size={24} />
          </div>
          <span className="text-primary font-bold text-xl tracking-tight">
            Finance Beta
          </span>
        </div>

        {/* Menu Principal */}
        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          <p className="px-3 text-xs font-semibold text-muted uppercase tracking-wider mb-4">
            Menu Principal
          </p>

          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            const Icon = item.icon;

            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 font-medium text-sm ${
                  isActive
                    ? "bg-brand/10 text-brand font-semibold"
                    : "hover:bg-elevated text-secondary hover:text-primary"
                }`}
              >
                <Icon
                  size={20}
                  className={
                    isActive
                      ? "text-brand"
                      : "text-muted transition-colors group-hover:text-primary"
                  }
                />
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Rodapé da Sidebar */}
        <div className="p-4 border-t border-subtle space-y-2">
          <Link
            to="/settings"
            className={`flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 font-medium text-sm ${
              location.pathname === "/settings"
                ? "bg-brand/10 text-brand font-semibold"
                : "hover:bg-elevated text-secondary hover:text-primary"
            }`}
          >
            <Settings size={20} />
            Configurações
          </Link>

          <div className="flex items-center justify-between px-3 py-3 mt-2 bg-elevated rounded-xl border border-subtle">
            <div className="flex flex-col overflow-hidden">
              <span className="text-xs text-muted">Logado como</span>
              <span className="text-sm font-bold text-primary truncate">
                {user?.name}
              </span>
            </div>
            <button
              onClick={signOut}
              className="p-2 text-muted hover:text-brand rounded-lg transition-colors cursor-pointer"
              title="Sair"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      {/* BOTTOM NAVIGATION MOBILE */}
      <nav className="md:hidden fixed bottom-0 left-0 w-full bg-surface border-t border-border z-50 px-2 py-2 flex justify-between items-center pb-safe">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          const Icon = item.icon;

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex flex-col items-center justify-center w-full py-2 gap-1 rounded-xl transition-colors ${
                isActive
                  ? "text-brand font-semibold"
                  : "text-muted hover:text-primary"
              }`}
            >
              <Icon size={20} />
              <span className="text-[10px] font-medium">{item.label}</span>
            </Link>
          );
        })}
        <Link
          to="/settings"
          className={`flex flex-col items-center justify-center w-full py-2 gap-1 rounded-xl transition-colors ${
            location.pathname === "/settings"
              ? "text-brand font-semibold"
              : "text-muted hover:text-primary"
          }`}
        >
          <Settings size={20} />
          <span className="text-[10px] font-medium">Ajustes</span>
        </Link>
      </nav>
    </>
  );
}
