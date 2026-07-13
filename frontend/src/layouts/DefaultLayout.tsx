import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/layout/Sidebar";
import { Header } from "../components/layout/Header";
import { BottomNav } from "../components/layout/BottomNav";

export function DefaultLayout() {
  return (
    <div className="min-h-screen w-full bg-page text-primary flex flex-col min-[1400px]:flex-row font-sans selection:bg-brand/20 selection:text-brand transition-colors duration-300">
      {/* 1. SIDEBAR LATERAL (Visível apenas em Desktop largo >= 1400px) */}
      <Sidebar />

      {/* 2. ÁREA DE CONTEÚDO PRINCIPAL */}
      {/* No desktop largo, empurramos o conteúdo 64px (256px) para a direita */}
      <div className="flex-1 flex flex-col min-h-screen min-[1400px]:pl-64 transition-all duration-300 relative">
        <Header />

        <main className="flex-1 w-full max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 pt-4 pb-28 sm:py-8 animate-fade-in flex flex-col">
          <Outlet />
        </main>
      </div>

      {/* 3. BARRA DE NAVEGAÇÃO MOBILE (Surgirá abaixo de 1400px) */}
      <BottomNav />
    </div>
  );
}
