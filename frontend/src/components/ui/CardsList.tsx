import { CreditCard } from "lucide-react";
import { useCards } from "../../hooks/useCredit";
import { Skeleton } from "./Skeleton";
// Importe seu componente de CardItem aqui se ele for separado

export function CardsSection() {
   // 1. Aqui está o segredo: desestruturamos o isLoading que o React Query já gerencia pra nós!
   //    const { data: cards = [], isLoading } = useCards();
   const { data: cards = [] } = useCards();
   const isLoading = true; // 🔥 Mudei na mão só para testar o visual!

   return (
      <div className="mt-8">
         <h2 className="text-lg font-bold text-slate-800 mb-4">Meus Cartões</h2>

         {/* ESTADO 1: LOADING (Skeletons) */}
         {isLoading && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
               {[1, 2, 3].map((i) => (
                  <div
                     key={i}
                     className="p-6 rounded-3xl border border-slate-100 bg-white h-48 flex flex-col justify-between shadow-sm"
                  >
                     <div className="flex justify-between items-start">
                        <Skeleton className="h-10 w-10 rounded-full" />
                        <Skeleton className="h-6 w-16 rounded-full" />
                     </div>
                     <div>
                        <Skeleton className="h-4 w-24 mb-2" />
                        <Skeleton className="h-8 w-32" />
                     </div>
                  </div>
               ))}
            </div>
         )}

         {/* ESTADO 2: EMPTY STATE (Sem cartões) */}
         {!isLoading && cards.length === 0 && (
            <div className="flex flex-col items-center justify-center p-10 bg-slate-50 border border-slate-100 rounded-3xl text-center">
               <div className="w-16 h-16 bg-white shadow-sm rounded-full flex items-center justify-center text-slate-300 mb-4">
                  <CreditCard size={32} />
               </div>
               <h3 className="text-slate-700 font-semibold mb-1">
                  Nenhum cartão cadastrado
               </h3>
               <p className="text-slate-500 text-sm max-w-xs">
                  Adicione seu primeiro cartão de crédito para começar a
                  controlar seus limites e parcelamentos.
               </p>
            </div>
         )}

         {/* ESTADO 3: SUCESSO (Lista os cartões reais) */}
         {!isLoading && cards.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
               {cards.map((card) => (
                  // Substitua pela forma como você renderiza o cartão hoje
                  <div
                     key={card.id}
                     className="p-6 rounded-3xl border bg-white shadow-sm"
                  >
                     {card.name} - {card.brand}
                  </div>
               ))}
            </div>
         )}
      </div>
   );
}
