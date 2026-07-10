import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/layout/Sidebar";
import { Header } from "../components/layout/Header";
import { BottomNav } from "../components/layout/BottomNav";

export function DefaultLayout() {
  return (
    <div className="min-h-screen w-full bg-page text-primary flex flex-col lg:flex-row font-sans selection:bg-brand/20 selection:text-brand transition-colors duration-300">
      {/* 1. SIDEBAR LATERAL (Visível apenas em Desktop lg:flex) */}
      <Sidebar />

      {/* 2. ÁREA DE CONTEÚDO PRINCIPAL */}
      {/* No desktop (lg:pl-64), empurramos o conteúdo 64px para a direita para não ficar embaixo da Sidebar fixa */}
      <div className="flex-1 flex flex-col min-h-screen lg:pl-64 transition-all duration-300 relative">
        {/* Cabeçalho Superior Fixo */}
        <Header />

        {/* 🏛️ REGRA 2: O CONTAINER GLOBAL (Single Source of Truth) */}
        {/* px-3 no mobile = 94% de aproveitamento útil. pb-28 garante respiro absoluto acima da BottomNav! */}
        <main className="flex-1 w-full max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 pt-4 pb-28 sm:py-8 animate-fade-in flex flex-col">
          <Outlet />
        </main>
      </div>

      {/* 3. BARRA DE NAVEGAÇÃO MOBILE (Visível apenas em Mobile lg:hidden) */}
      <BottomNav />
    </div>
  );
}
