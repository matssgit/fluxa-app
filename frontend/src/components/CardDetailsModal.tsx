import { useState } from "react";
import { X, Edit2, Trash2, CreditCard, Calendar } from "lucide-react";
import { usePurchases, type Card, type Purchase } from "../hooks/useCredit";
import { getCardGradient } from "../utils/cardColors";
import { PurchasesList } from "./PurchasesList";
import { PurchaseDetailsModal } from "./PurchaseDetailsModal"; // Modal importado!

interface CardDetailsModalProps {
   isOpen: boolean;
   onClose: () => void;
   card: Card | null;
   onEditClick: () => void;
   onDeleteClick: () => void;
}

export function CardDetailsModal({
   isOpen,
   onClose,
   card,
   onEditClick,
   onDeleteClick,
}: CardDetailsModalProps) {
   // Estados para controlar o modal de detalhes da compra
   const [selectedPurchase, setSelectedPurchase] = useState<Purchase | null>(
      null,
   );
   const [isPurchaseModalOpen, setIsPurchaseModalOpen] = useState(false);

   const { data: purchases = [] } = usePurchases();

   if (!isOpen || !card) return null;

   const gradientClass = getCardGradient(card.color);

   const totalLimit = Number(card.total_limit || 0);
   const availableLimit = Number(card.available_limit || 0);
   const consumedLimit = totalLimit - availableLimit;
   const consumedPercentage =
      totalLimit > 0 ? (consumedLimit / totalLimit) * 100 : 0;

   // Filtro Seguro
   const safePurchases = Array.isArray(purchases) ? purchases : [];
   const cardPurchases = safePurchases.filter((p) => p.card_id === card.id);

   return (
      <>
         <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
            <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
               {/* Cabeçalho colorido dinâmico */}
               <div
                  className={`p-6 bg-gradient-to-tr ${gradientClass} text-white relative`}
               >
                  <button
                     onClick={onClose}
                     className="absolute top-6 right-6 p-2 bg-white/10 hover:bg-white/20 rounded-full transition-colors backdrop-blur-sm"
                  >
                     <X size={20} />
                  </button>
                  <div className="flex items-center gap-3 mb-1">
                     <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                        <CreditCard size={20} />
                     </div>
                     <div>
                        <h2 className="text-xl font-bold leading-tight">
                           {card.name}
                        </h2>
                        <span className="text-xs font-medium uppercase tracking-wider text-white/80">
                           {card.brand}
                        </span>
                     </div>
                  </div>
               </div>

               {/* Corpo do Modal */}
               <div className="p-6 overflow-y-auto max-h-[65vh]">
                  {/* Resumo do Limite */}
                  <div className="bg-slate-50 rounded-2xl p-5 mb-6 border border-slate-100">
                     <div className="flex justify-between items-end mb-2">
                        <div>
                           <p className="text-sm font-medium text-slate-500">
                              Limite Disponível
                           </p>
                           <p className="text-2xl font-bold text-slate-800">
                              {new Intl.NumberFormat("pt-BR", {
                                 style: "currency",
                                 currency: "BRL",
                              }).format(availableLimit)}
                           </p>
                        </div>
                        <div className="text-right">
                           <p className="text-xs font-medium text-slate-500 mb-1">
                              Limite Total
                           </p>
                           <p className="text-sm font-semibold text-slate-700">
                              {new Intl.NumberFormat("pt-BR", {
                                 style: "currency",
                                 currency: "BRL",
                              }).format(totalLimit)}
                           </p>
                        </div>
                     </div>

                     {/* Barra de Progresso do Limite */}
                     <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden mt-3">
                        <div
                           className="h-full bg-purple-500 rounded-full transition-all duration-500"
                           style={{
                              width: `${Math.min(consumedPercentage, 100)}%`,
                           }}
                        />
                     </div>
                     <p className="text-xs text-right mt-2 text-slate-400">
                        {consumedPercentage.toFixed(1)}% utilizado
                     </p>
                  </div>

                  {/* Informações Adicionais */}
                  <div className="flex items-center justify-between p-4 bg-slate-50 rounded-2xl border border-slate-100 mb-6">
                     <div className="flex items-center gap-3 text-slate-600">
                        <Calendar size={18} className="text-slate-400" />
                        <span className="font-medium text-sm">
                           Vencimento da Fatura
                        </span>
                     </div>
                     <span className="font-bold text-slate-800">
                        Dia {card.due_day}
                     </span>
                  </div>

                  {/* Ações Rápidas */}
                  <div className="grid grid-cols-2 gap-3 pt-2 mb-6">
                     <button
                        onClick={onEditClick}
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-medium transition-colors"
                     >
                        <Edit2 size={16} /> Editar Cartão
                     </button>
                     <button
                        onClick={onDeleteClick}
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl font-medium transition-colors"
                     >
                        <Trash2 size={16} /> Excluir Cartão
                     </button>
                  </div>

                  {/* Lista de Compras Conectada */}
                  <PurchasesList
                     purchases={cardPurchases}
                     onPurchaseClick={(purchase) => {
                        setSelectedPurchase(purchase);
                        setIsPurchaseModalOpen(true);
                     }}
                  />
               </div>
            </div>
         </div>

         {/* Modal de Detalhes da Compra */}
         <PurchaseDetailsModal
            isOpen={isPurchaseModalOpen}
            onClose={() => {
               setIsPurchaseModalOpen(false);
               setSelectedPurchase(null);
            }}
            purchase={selectedPurchase}
         />
      </>
   );
}
