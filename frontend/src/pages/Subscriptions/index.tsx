import { useState } from "react";
import { Plus, Repeat } from "lucide-react"; // Agora será lido abaixo
import { CreateSubscriptionModal } from "./CreateSubscriptionModal";
import { useSubscriptions } from "../../hooks/useSubscriptions";
import { type Subscription } from "../../types/subscription";

export function Subscriptions() {
   const { data: subscriptions = [], isLoading } = useSubscriptions();
   const [isNewSubModalOpen, setIsNewSubModalOpen] = useState(false);

   return (
      <div className="w-full pb-16">
         <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
               <div>
                  <h2 className="text-2xl font-bold text-finance-primary tracking-tight">
                     Minhas Assinaturas
                  </h2>
                  <p className="text-finance-primary/60 text-sm mt-1">
                     Gerencie seus pagamentos recorrentes
                  </p>
               </div>
               <button
                  onClick={() => setIsNewSubModalOpen(true)}
                  className="btn-premium-primary flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium"
               >
                  <Plus size={18} /> Nova Assinatura
               </button>
            </div>

            {isLoading ? (
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {[1, 2, 3].map((i) => (
                     <div
                        key={i}
                        className="bg-premium-card p-6 rounded-2xl h-32 animate-pulse"
                     />
                  ))}
               </div>
            ) : subscriptions.length === 0 ? (
               <div className="bg-premium-card rounded-2xl p-12 text-center">
                  {/* Uso do Repeat para silenciar o erro do linter */}
                  <div className="w-16 h-16 bg-finance-primary/5 text-finance-primary/40 rounded-full flex items-center justify-center mx-auto mb-5 border border-finance-primary/10">
                     <Repeat size={32} />
                  </div>
                  <p className="text-finance-primary font-bold">
                     Nenhuma assinatura ativa.
                  </p>
               </div>
            ) : (
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {subscriptions.map((sub: Subscription) => (
                     <div
                        key={sub.id}
                        className="bg-premium-card p-6 rounded-2xl border border-finance-primary/5"
                     >
                        <h3 className="font-bold text-finance-primary">
                           {sub.title}
                        </h3>
                        <p className="text-finance-primary/50 text-xs">
                           {new Intl.NumberFormat("pt-BR", {
                              style: "currency",
                              currency: "BRL",
                           }).format(sub.amount)}
                        </p>
                     </div>
                  ))}
               </div>
            )}
         </main>

         <CreateSubscriptionModal
            isOpen={isNewSubModalOpen}
            onClose={() => setIsNewSubModalOpen(false)}
         />
      </div>
   );
}
