import { Plus, ShoppingBag, CreditCard as CardIcon } from "lucide-react";
import { Header } from "../components/Header";
import { useCredit } from "../hooks/useCredit";

export function CreditCards() {
   const { cards, isLoadingCards } = useCredit();

   return (
      <div className="min-h-screen bg-slate-50 pb-12">
         <Header />

         <main className="max-w-6xl mx-auto px-6 -mt-10">
            {/* Cabeçalho da Seção */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
               <div>
                  <h2 className="text-2xl font-bold text-white sm:text-slate-800 sm:-mt-16">
                     Meus Cartões
                  </h2>
                  <p className="text-slate-400 sm:text-slate-500 text-sm mt-1">
                     Gerencie seus limites e faturas
                  </p>
               </div>

               <div className="flex gap-3 w-full sm:w-auto">
                  <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-slate-200 hover:bg-slate-300 text-slate-700 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm">
                     <Plus size={18} />
                     Novo Cartão
                  </button>
                  <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm">
                     <ShoppingBag size={18} />
                     Lançar Compra
                  </button>
               </div>
            </div>

            {/* Lista de Cartões */}
            {isLoadingCards ? (
               <p className="text-slate-500">Carregando cartões...</p>
            ) : cards.length === 0 ? (
               <div className="bg-white rounded-2xl border border-slate-100 p-12 text-center shadow-sm">
                  <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4">
                     <CardIcon size={32} />
                  </div>
                  <p className="text-slate-500 mb-2">
                     Você ainda não possui cartões cadastrados.
                  </p>
                  <p className="text-sm text-slate-400">
                     Adicione seu primeiro cartão para começar a registrar
                     compras.
                  </p>
               </div>
            ) : (
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {cards.map((card) => (
                     <div
                        key={card.id}
                        className="bg-gradient-to-tr from-slate-900 to-slate-800 rounded-2xl p-6 text-white shadow-xl relative overflow-hidden"
                     >
                        {/* Efeito de brilho do cartão */}
                        <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full blur-2xl -mr-10 -mt-10"></div>

                        <div className="flex justify-between items-start mb-8 relative z-10">
                           <span className="font-semibold text-lg">
                              {card.name}
                           </span>
                           <span className="text-xs font-medium uppercase tracking-wider bg-white/10 px-2 py-1 rounded-md">
                              {card.brand}
                           </span>
                        </div>

                        <div className="relative z-10">
                           <p className="text-slate-400 text-sm mb-1">
                              Limite Disponível (Simulado)
                           </p>
                           <p className="text-2xl font-bold">
                              {new Intl.NumberFormat("pt-BR", {
                                 style: "currency",
                                 currency: "BRL",
                              }).format(card.limit_amount)}
                           </p>
                        </div>

                        <div className="mt-6 pt-4 border-t border-white/10 flex justify-between text-sm text-slate-300 relative z-10">
                           <span>Vencimento</span>
                           <span className="font-medium">
                              Dia {card.due_day}
                           </span>
                        </div>
                     </div>
                  ))}
               </div>
            )}
         </main>
      </div>
   );
}
