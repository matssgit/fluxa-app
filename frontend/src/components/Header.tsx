import {
  Wallet,
  LogOut,
  Settings,
  LayoutDashboard,
  CreditCard,
  Repeat,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { Link, useLocation } from "react-router-dom";

export function Header() {
  const { user, signOut } = useAuth();
  const location = useLocation();

  return (
    <header className="bg-primary pt-8 pb-32 px-6 shadow-xs">
      <div className="max-w-6xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-6 md:gap-0">
        {/* BLOCO ESQUERDO: Logo, Textos e Navegação */}
        <div className="flex flex-col md:flex-row md:items-center gap-6">
          {/* Logo e Título */}
          <div className="flex items-center gap-3">
            <div className="bg-accent p-2 rounded-lg shadow-xs">
              <Wallet className="text-primary" size={28} />
            </div>
            <Link to="/">
              <h1 className="text-surface text-2xl font-bold tracking-tight hover:text-accent transition-colors">
                Finance Dashboard
              </h1>
              <p className="text-surface/60 text-sm hidden lg:block">
                Controle simples de receitas e despesas
              </p>
            </Link>
          </div>

          {/* Separador visual */}
          <div className="hidden md:block h-8 w-px bg-surface/10 mx-2"></div>

          {/* Menu de Navegação */}
          <nav className="flex flex-wrap items-center gap-2">
            <Link
              to="/"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 text-sm font-medium ${
                location.pathname === "/"
                  ? "text-surface bg-surface/10 font-semibold"
                  : "text-surface/60 hover:text-surface hover:bg-surface/5"
              }`}
            >
              <LayoutDashboard size={18} />
              Visão Geral
            </Link>

            <Link
              to="/cards"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 text-sm font-medium ${
                location.pathname === "/cards"
                  ? "text-surface bg-surface/10 font-semibold"
                  : "text-surface/60 hover:text-surface hover:bg-surface/5"
              }`}
            >
              <CreditCard size={18} />
              Cartões
            </Link>

            <Link
              to="/subscriptions"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-all duration-200 text-sm font-medium ${
                location.pathname === "/subscriptions"
                  ? "text-surface bg-surface/10 font-semibold"
                  : "text-surface/60 hover:text-surface hover:bg-surface/5"
              }`}
            >
              <Repeat size={18} />
              Assinaturas
            </Link>
          </nav>
        </div>

        {/* BLOCO DIREITO: Saudação, Configurações e Logout */}
        <div className="flex items-center justify-between md:justify-end gap-4">
          <span className="text-surface/70 text-sm hidden sm:block">
            Olá,{" "}
            <strong className="text-surface font-semibold">{user?.name}</strong>
          </span>

          <div className="flex items-center gap-2">
            <Link
              to="/settings"
              className="text-surface/60 hover:text-surface p-2 rounded-lg hover:bg-surface/10 transition-colors"
              title="Configurações"
            >
              <Settings size={20} />
            </Link>

            <button
              onClick={signOut}
              className="flex items-center gap-2 text-accent hover:text-surface bg-surface/10 hover:bg-surface/20 px-3 py-2 rounded-lg transition-all duration-200 cursor-pointer"
              title="Sair da conta"
            >
              <LogOut size={20} />
              <span className="hidden sm:inline font-medium text-sm">Sair</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
