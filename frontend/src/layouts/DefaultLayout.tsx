import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/Sidebar";

export function DefaultLayout() {
   return (
      <div className="min-h-screen bg-slate-50 flex">
         {/* A Sidebar fica fixa aqui */}
         <Sidebar />

         {/* Área principal de conteúdo */}
         {/* No Desktop (md), damos um padding-left de 64 (256px) para a tela não ficar embaixo da Sidebar */}
         {/* No Mobile, damos um padding-bottom para a tela não ficar embaixo do Bottom Navigation */}
         <main className="flex-1 md:pl-64 pb-20 md:pb-0 min-w-0">
            {/* O Outlet é onde as páginas (Dashboard, Cards, etc) serão renderizadas */}
            <Outlet />
         </main>
      </div>
   );
}
