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
         <aside className="hidden md:flex flex-col w-64 bg-white min-h-screen fixed left-0 top-0 border-r border-accent-primary/10 z-50">
            {/* Logo */}
            <div className="p-6 flex items-center gap-3 border-b border-accent-primary/5">
               <div className="bg-accent-primary p-2 rounded-lg">
                  <Wallet className="text-white" size={24} />
               </div>
               <span className="text-text-primary font-bold text-xl tracking-tight">
                  Finance Beta
               </span>
            </div>

            {/* Menu Principal */}
            <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
               <p className="px-3 text-xs font-semibold text-text-primary/40 uppercase tracking-wider mb-4">
                  Menu Principal
               </p>

               {navItems.map((item) => {
                  const isActive = location.pathname === item.path;
                  const Icon = item.icon;

                  return (
                     <Link
                        key={item.path}
                        to={item.path}
                        className={`flex items-center gap-3 px-3 py-3 rounded-xl transition-all font-medium text-sm ${
                           isActive
                              ? "bg-accent-primary/10 text-accent-primary"
                              : "hover:bg-accent-primary/5 text-text-primary/70 hover:text-text-primary"
                        }`}
                     >
                        <Icon
                           size={20}
                           className={
                              isActive
                                 ? "text-accent-primary"
                                 : "text-text-primary/40"
                           }
                        />
                        {item.label}
                     </Link>
                  );
               })}
            </nav>

            {/* Rodapé da Sidebar */}
            <div className="p-4 border-t border-accent-primary/5 space-y-2">
               <Link
                  to="/settings"
                  className={`flex items-center gap-3 px-3 py-3 rounded-xl transition-all font-medium text-sm ${
                     location.pathname === "/settings"
                        ? "bg-accent-primary/10 text-accent-primary"
                        : "hover:bg-accent-primary/5 text-text-primary/70 hover:text-text-primary"
                  }`}
               >
                  <Settings size={20} />
                  Configurações
               </Link>

               <div className="flex items-center justify-between px-3 py-3 mt-2 bg-accent-primary/5 rounded-xl border border-accent-primary/10">
                  <div className="flex flex-col overflow-hidden">
                     <span className="text-xs text-text-primary/40">
                        Logado como
                     </span>
                     <span className="text-sm font-bold text-text-primary truncate">
                        {user?.name}
                     </span>
                  </div>
                  <button
                     onClick={signOut}
                     className="p-2 text-text-primary/40 hover:text-accent-primary rounded-lg transition-colors"
                     title="Sair"
                  >
                     <LogOut size={18} />
                  </button>
               </div>
            </div>
         </aside>

         {/* BOTTOM NAVIGATION MOBILE */}
         <nav className="md:hidden fixed bottom-0 left-0 w-full bg-white border-t border-accent-primary/10 z-50 px-2 py-2 flex justify-between items-center pb-safe">
            {navItems.map((item) => {
               const isActive = location.pathname === item.path;
               const Icon = item.icon;

               return (
                  <Link
                     key={item.path}
                     to={item.path}
                     className={`flex flex-col items-center justify-center w-full py-2 gap-1 rounded-xl transition-colors ${
                        isActive
                           ? "text-accent-primary"
                           : "text-text-primary/40 hover:text-text-primary"
                     }`}
                  >
                     <Icon size={20} />
                     <span className="text-[10px] font-medium">
                        {item.label}
                     </span>
                  </Link>
               );
            })}
            <Link
               to="/settings"
               className={`flex flex-col items-center justify-center w-full py-2 gap-1 rounded-xl transition-colors ${
                  location.pathname === "/settings"
                     ? "text-accent-primary"
                     : "text-text-primary/40 hover:text-text-primary"
               }`}
            >
               <Settings size={20} />
               <span className="text-[10px] font-medium">Ajustes</span>
            </Link>
         </nav>
      </>
   );
}
