import { useLocation } from "react-router-dom";
import { LogOut, ShieldCheck } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

// Mapeamento simples de títulos para o contexto do Header
const PAGE_TITLES: Record<string, { title: string; subtitle: string }> = {
  "/dashboard": {
    title: "Central de Comando",
    subtitle: "Visão geral e projeções do mês",
  },
  "/transactions": {
    title: "Fluxo de Caixa",
    subtitle: "Entradas, saídas e controle diário",
  },
  "/cards": {
    title: "Ecossistema de Crédito",
    subtitle: "Gestão de cartões, faturas e parcelas",
  },
  "/subscriptions": {
    title: "Assinaturas Recorrentes",
    subtitle: "Monitoramento de serviços e cobranças",
  },
  "/accounts": {
    title: "Contas & Carteiras",
    subtitle: "Saldo bancário e liquidez imediata",
  },
  "/settings": {
    title: "Configurações",
    subtitle: "Preferências globais da plataforma",
  },
};

export function Header() {
  const location = useLocation();
  const { user, signOut } = useAuth();

  // Encontra o título correspondente à rota atual ou define um fallback elegante
  const currentPathKey = Object.keys(PAGE_TITLES).find((key) =>
    location.pathname.startsWith(key),
  );

  const pageInfo = currentPathKey
    ? PAGE_TITLES[currentPathKey]
    : { title: "Finance App", subtitle: "Gestão financeira inteligente" };

  return (
    <header className="h-20 w-full bg-surface/80 backdrop-blur-md border-b border-subtle/20 sticky top-0 z-30 px-6 lg:px-10 flex items-center justify-between transition-all duration-300">
      {/* 1. CONTEXTO DA PÁGINA (Título e Subtítulo) */}
      <div className="space-y-0.5">
        <h1 className="text-base lg:text-lg font-bold text-primary tracking-tight leading-none">
          {pageInfo.title}
        </h1>
        <p className="text-xs font-medium text-muted hidden sm:block">
          {pageInfo.subtitle}
        </p>
      </div>

      {/* 2. AÇÕES DO USUÁRIO E STATUS */}
      <div className="flex items-center gap-4">
        {/* Badge Sutil de Segurança/Ambiente */}
        <div className="hidden md:flex items-center gap-1.5 px-3 py-1 rounded-full bg-elevated/80 border border-subtle/30 text-[11px] font-semibold text-secondary">
          <ShieldCheck className="w-3.5 h-3.5 text-brand" />
          <span>Ambiente Protegido</span>
        </div>

        {/* Perfil e Saída */}
        <div className="flex items-center gap-3 pl-2 border-l border-subtle/20">
          <div className="text-right hidden sm:block">
            <span className="text-xs font-bold text-primary block leading-none">
              {user?.name || "Usuário Beta"}
            </span>
            <span className="text-[10px] font-medium text-muted mt-0.5 block">
              {user?.email || "contato@financeapp.com"}
            </span>
          </div>

          <button
            onClick={signOut}
            title="Sair do sistema"
            className="w-9 h-9 rounded-xl bg-elevated/60 hover:bg-red-500/10 hover:text-red-500 text-secondary flex items-center justify-center transition-all duration-200 cursor-pointer shadow-xs hover:border hover:border-red-500/20"
          >
            <LogOut className="w-4 h-4 transition-transform duration-200 hover:-translate-x-0.5" />
          </button>
        </div>
      </div>
    </header>
  );
}
