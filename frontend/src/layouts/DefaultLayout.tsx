import { BottomNav } from "../components/layout/BottomNav";
import { Header } from "../components/layout/Header";
import { Outlet } from "react-router-dom";
import { ProductTour } from "../components/ui/ProductTour";
import { Sidebar } from "../components/layout/Sidebar";
import { useNotificationSystem } from "../hooks/useNotificationSystem";

export function DefaultLayout() {
  useNotificationSystem();

  return (
    <div className="min-h-screen bg-background text-primary flex selection:bg-brand/20 selection:text-primary transition-colors duration-300">
      <ProductTour />

      <Sidebar />

      <div className="flex-1 flex flex-col xl:ml-60 w-full relative min-w-0">
        <Header />

        <main className="flex-1 px-4 pt-6 sm:px-7 sm:pt-8 lg:px-10 lg:pt-10 xl:px-12 pb-28 sm:pb-24 xl:pb-14 overflow-x-hidden max-w-[1480px] mx-auto w-full min-w-0">
          <Outlet />
        </main>
        <BottomNav />
      </div>
    </div>
  );
}
