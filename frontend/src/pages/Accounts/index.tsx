import { Landmark } from "lucide-react";

export function Accounts() {
   return (
      <div className="p-8 max-w-4xl mx-auto">
         <div className="bg-white p-16 rounded-3xl border border-slate-100 text-center shadow-sm flex flex-col items-center">
            <div className="bg-purple-50 p-4 rounded-full mb-6">
               <Landmark size={48} className="text-purple-600" />
            </div>
            <h2 className="text-2xl font-bold text-slate-800">
               Módulo de Contas
            </h2>
            <p className="text-slate-500 mt-3 max-w-md">
               Estamos construindo este espaço. Em breve, você gerenciará suas
               Carteiras e Categorias por aqui, fora do menu de configurações.
            </p>
         </div>
      </div>
   );
}
