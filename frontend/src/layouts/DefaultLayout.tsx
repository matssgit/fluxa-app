import { BottomNav } from "../components/layout/BottomNav";
import { Header } from "../components/layout/Header";
import { Outlet } from "react-router-dom";
import { ProductTour } from "../components/ui/ProductTour";
import { Sidebar } from "../components/layout/Sidebar";
import { useNotificationSystem } from "../hooks/useNotificationSystem";

export function DefaultLayout() {
  useNotificationSystem();

  return (
    <div className="min-h-screen bg-background text-primary flex selection:bg-brand/30 selection:text-brand-light transition-colors duration-300">
      <ProductTour />

      <Sidebar />

      <div className="flex-1 flex flex-col min-[1400px]:ml-64 w-full relative min-w-0">
        <Header />

        <main className="flex-1 p-4 sm:p-6 lg:p-10 pb-24 sm:pb-6 lg:pb-10 overflow-x-hidden max-w-[1600px] mx-auto w-full min-w-0">
          <Outlet />
        </main>
        <BottomNav />
      </div>
    </div>
  );
}
