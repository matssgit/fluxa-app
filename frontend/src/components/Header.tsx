import { Wallet } from "lucide-react";

export function Header() {
   return (
      <header className="bg-slate-900 pt-8 pb-32 px-6">
         <div className="max-w-6xl mx-auto flex items-center gap-3">
            <div className="bg-emerald-500 p-2 rounded-lg">
               <Wallet className="text-slate-900" size={28} />
            </div>
            <div>
               <h1 className="text-white text-2xl font-bold tracking-tight">
                  Finance Dashboard
               </h1>
               <p className="text-slate-400 text-sm">
                  Controle simples de receitas e despesas
               </p>
            </div>
         </div>
      </header>
   );
}
