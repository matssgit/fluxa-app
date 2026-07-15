import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/layout/Sidebar";
import { Header } from "../components/layout/Header";
import { BottomNav } from "../components/layout/BottomNav";
import { useNotificationSystem } from "../hooks/useNotificationSystem"; // ✨ IMPORTADO

export function DefaultLayout() {
  useNotificationSystem();

  return (
    <div className="min-h-screen bg-background text-primary flex selection:bg-brand/30 selection:text-brand-light transition-colors duration-300">
      <Sidebar />
      <div className="flex-1 flex flex-col min-[1400px]:ml-64 w-full relative">
        <Header />
        <main className="flex-1 p-4 sm:p-6 lg:p-10 pb-28 sm:pb-6 lg:pb-10 overflow-x-hidden max-w-[1600px] mx-auto w-full">
          <Outlet />
        </main>
        <BottomNav />
      </div>
    </div>
  );
}
