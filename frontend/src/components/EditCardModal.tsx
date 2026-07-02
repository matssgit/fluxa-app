import React, { useState } from "react";
import { X, CreditCard, Calendar, DollarSign } from "lucide-react";
// 1. Ajuste de importação: importamos o hook específico de edição
import { useEditCard, type Card } from "../hooks/useCredit";
import { CARD_COLORS } from "../utils/cardColors";

interface EditCardModalProps {
   isOpen: boolean;
   onClose: () => void;
   card: Card | null;
}

export function EditCardModal({ isOpen, onClose, card }: EditCardModalProps) {
   // 2. Usamos o hook específico de edição
   const { mutateAsync: editCard } = useEditCard();

   // Estados iniciais
   const [name, setName] = useState(card?.name || "");
   const [brand, setBrand] = useState(card?.brand || "Visa");
   const [totalLimit, setTotalLimit] = useState(
      card?.total_limit ? String(card.total_limit) : "",
   );
   const [dueDay, setDueDay] = useState(
      card?.due_day ? String(card.due_day) : "",
   );
   const [color, setColor] = useState(card?.color || "slate");
   const [isSubmitting, setIsSubmitting] = useState(false);

   // Se o modal não estiver aberto ou não tiver card, não renderiza
   if (!isOpen || !card) return null;

   async function handleSubmit(e: React.FormEvent) {
      e.preventDefault();

      if (!card) return;

      try {
         setIsSubmitting(true);

         // 3. Chamada da mutation com tipagem correta
         await editCard({
            id: card.id,
            name,
            brand,
            total_limit: Number(totalLimit),
            due_day: Number(dueDay),
            color,
         });

         onClose();
      } catch (error) {
         console.error("Erro ao editar o cartão:", error);
         alert("Erro ao salvar as alterações.");
      } finally {
         setIsSubmitting(false);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
         <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Editar Cartão
               </h2>
               <button
                  onClick={onClose}
                  className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
               {/* Nome */}
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">
                     Apelido do Cartão
                  </label>
                  <div className="relative">
                     <CreditCard
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                        size={18}
                     />
                     <input
                        required
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none transition-all"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                     />
                  </div>
               </div>

               {/* Bandeira */}
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">
                     Bandeira
                  </label>
                  <select
                     className="w-full pl-4 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none appearance-none"
                     value={brand}
                     onChange={(e) => setBrand(e.target.value)}
                  >
                     <option value="Visa">Visa</option>
                     <option value="Mastercard">Mastercard</option>
                     <option value="Elo">Elo</option>
                     <option value="American Express">Amex</option>
                     <option value="Outra">Outra</option>
                  </select>
               </div>

               {/* Limite e Vencimento */}
               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1.5">
                        Limite Total
                     </label>
                     <div className="relative">
                        <DollarSign
                           className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                           size={18}
                        />
                        <input
                           required
                           type="number"
                           className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none"
                           value={totalLimit}
                           onChange={(e) => setTotalLimit(e.target.value)}
                        />
                     </div>
                  </div>
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1.5">
                        Vencimento (Dia)
                     </label>
                     <div className="relative">
                        <Calendar
                           className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                           size={18}
                        />
                        <input
                           required
                           type="number"
                           min="1"
                           max="31"
                           className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none"
                           value={dueDay}
                           onChange={(e) => setDueDay(e.target.value)}
                        />
                     </div>
                  </div>
               </div>

               {/* Cores */}
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                     Cor do Cartão
                  </label>
                  <div className="flex flex-wrap gap-3">
                     {CARD_COLORS.map((c) => (
                        <button
                           key={c.id}
                           type="button"
                           onClick={() => setColor(c.id)}
                           className={`w-8 h-8 rounded-full transition-all shadow-sm ${c.bgClass} ${
                              color === c.id
                                 ? "ring-2 ring-offset-2 ring-purple-600 scale-110"
                                 : "opacity-60 hover:opacity-100 hover:scale-105"
                           }`}
                        />
                     ))}
                  </div>
               </div>

               <div className="pt-4 flex gap-3">
                  <button
                     type="button"
                     onClick={onClose}
                     className="flex-1 px-4 py-3 text-slate-600 font-medium hover:bg-slate-100 rounded-xl transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     type="submit"
                     disabled={isSubmitting}
                     className="flex-1 bg-purple-600 hover:bg-purple-700 text-white px-4 py-3 rounded-xl font-medium transition-colors shadow-sm disabled:opacity-50"
                  >
                     {isSubmitting ? "Salvando..." : "Salvar Alterações"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
