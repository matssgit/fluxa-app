import {
   Wallet,
   LogOut,
   Settings,
   LayoutDashboard,
   CreditCard,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { Link, useLocation } from "react-router-dom";

export function Header() {
   const { user, signOut } = useAuth();
   const location = useLocation(); // Ajuda a saber qual página está ativa

   return (
      <header className="bg-slate-900 pt-8 pb-32 px-6">
         {/* Transformei em flex-col no mobile e flex-row no desktop para o menu não espremer */}
         <div className="max-w-6xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-6 md:gap-0">
            {/* BLOCO ESQUERDO: Logo, Textos e Navegação */}
            <div className="flex flex-col md:flex-row md:items-center gap-6">
               {/* Logo e Título */}
               <div className="flex items-center gap-3">
                  <div className="bg-emerald-500 p-2 rounded-lg">
                     <Wallet className="text-slate-900" size={28} />
                  </div>
                  <Link to="/">
                     <h1 className="text-white text-2xl font-bold tracking-tight hover:text-emerald-400 transition-colors">
                        Finance Dashboard
                     </h1>
                     <p className="text-slate-400 text-sm hidden lg:block">
                        Controle simples de receitas e despesas
                     </p>
                  </Link>
               </div>

               {/* Separador visual (visível apenas em telas maiores) */}
               <div className="hidden md:block h-8 w-px bg-slate-700 mx-2"></div>

               {/* Menu de Navegação */}
               <nav className="flex items-center gap-2">
                  <Link
                     to="/"
                     className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-colors text-sm font-medium ${
                        location.pathname === "/"
                           ? "text-white bg-slate-800" // Acende se estiver na Home
                           : "text-slate-400 hover:text-white"
                     }`}
                  >
                     <LayoutDashboard size={18} />
                     Visão Geral
                  </Link>

                  <Link
                     to="/cards"
                     className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-colors text-sm font-medium ${
                        location.pathname === "/cards"
                           ? "text-white bg-slate-800" // Acende se estiver nos Cartões
                           : "text-slate-400 hover:text-white"
                     }`}
                  >
                     <CreditCard size={18} />
                     Cartões
                  </Link>
               </nav>
            </div>

            {/* BLOCO DIREITO: Saudação, Configurações e Logout */}
            <div className="flex items-center justify-between md:justify-end gap-4">
               <span className="text-slate-300 text-sm hidden sm:block">
                  Olá, <strong className="text-white">{user?.name}</strong>
               </span>

               <div className="flex items-center gap-2">
                  {/* Botão de Configurações */}
                  <Link
                     to="/settings"
                     className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition-colors"
                     title="Configurações"
                  >
                     <Settings size={20} />
                  </Link>

                  <button
                     onClick={signOut}
                     className="flex items-center gap-2 text-red-400 hover:text-red-300 bg-slate-800/50 hover:bg-slate-800 px-3 py-2 rounded-lg transition-colors"
                     title="Sair da conta"
                  >
                     <LogOut size={20} />
                     <span className="hidden sm:inline font-medium text-sm">
                        Sair
                     </span>
                  </button>
               </div>
            </div>
         </div>
      </header>
   );
}
