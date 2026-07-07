import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/Sidebar";
import { Header } from "../components/Header";
import { BottomNav } from "../components/BottomNav";

export function DefaultLayout() {
  return (
    <div className="min-h-screen w-full bg-page text-primary flex flex-col lg:flex-row font-sans selection:bg-brand/20 selection:text-brand transition-colors duration-300">
      {/* 1. SIDEBAR LATERAL (Visível apenas em Desktop lg:flex) */}
      <Sidebar />

      {/* 2. ÁREA DE CONTEÚDO PRINCIPAL */}
      {/* No desktop (lg:pl-64), empurramos o conteúdo 64px (w-64) para a direita para não ficar embaixo da Sidebar fixa */}
      <div className="flex-1 flex flex-col min-h-screen lg:pl-64 transition-all duration-300">
        {/* Cabeçalho Superior Fixo */}
        <Header />

        {/* Orquestrador de Rotas (Onde as páginas como Dashboard e Cartões são renderizadas) */}
        {/* pb-24 no mobile garante que a BottomNav flutuante nunca cubra o final do conteúdo! */}
        <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-10 py-6 pb-24 lg:pb-10 animate-fade-in">
          <Outlet />
        </main>
      </div>

      {/* 3. BARRA DE NAVEGAÇÃO MOBILE (Visível apenas em Mobile lg:hidden) */}
      <BottomNav />
    </div>
  );
}
