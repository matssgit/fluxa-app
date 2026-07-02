import { useState } from "react";
import type { FormEvent } from "react";
import { X } from "lucide-react";
// 1. O hook deve ser importado individualmente
import { useCreatePurchase, useCards } from "../hooks/useCredit";
import { useCategories } from "../hooks/useCategories";

interface CreatePurchaseModalProps {
   isOpen: boolean;
   onClose: () => void;
}

export function CreatePurchaseModal({
   isOpen,
   onClose,
}: CreatePurchaseModalProps) {
   // 2. Aqui a desestruturação está correta agora
   const { mutateAsync: createPurchase } = useCreatePurchase();
   const { categories = [] } = useCategories();
   const { data: cards = [] } = useCards();

   const [title, setTitle] = useState("");
   const [store, setStore] = useState("");
   const [amount, setAmount] = useState("");
   const [installments, setInstallments] = useState("1");
   const [date, setDate] = useState("");
   const [cardId, setCardId] = useState("");
   const [categoryId, setCategoryId] = useState("");
   const [isSubmitting, setIsSubmitting] = useState(false);

   if (!isOpen) return null;

   async function handleSubmit(e: FormEvent) {
      e.preventDefault();
      setIsSubmitting(true);

      try {
         // Converte a data local do input para ISO String
         const isoDate = new Date(date).toISOString();

         await createPurchase({
            title,
            store,
            total_amount: Number(amount),
            total_installments: Number(installments),
            purchase_date: isoDate,
            card_id: cardId,
            category_id: categoryId,
         });

         // Reseta o estado
         setTitle("");
         setStore("");
         setAmount("");
         setInstallments("1");
         setDate("");
         setCardId("");
         setCategoryId("");
         onClose();
      } catch (error) {
         console.error("Erro ao lançar compra:", error);
         alert("Erro ao salvar a compra. Verifique os dados.");
      } finally {
         setIsSubmitting(false);
      }
   }

   return (
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
         <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Lançar Compra no Crédito
               </h2>
               <button
                  onClick={onClose}
                  className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-2 rounded-full transition-colors"
                  type="button"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Cartão de Crédito
                     </label>
                     <select
                        required
                        value={cardId}
                        onChange={(e) => setCardId(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 focus:border-transparent transition-all bg-white"
                     >
                        <option value="" disabled>
                           Selecione o cartão
                        </option>
                        {cards.map((card) => (
                           <option key={card.id} value={card.id}>
                              {card.name} (Dia {card.due_day})
                           </option>
                        ))}
                     </select>
                  </div>

                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Categoria
                     </label>
                     <select
                        required
                        value={categoryId}
                        onChange={(e) => setCategoryId(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 focus:border-transparent transition-all bg-white"
                     >
                        <option value="" disabled>
                           Selecione a categoria
                        </option>

                        {categories.map((cat: { id: string; name: string }) => (
                           <option key={cat.id} value={cat.id}>
                              {cat.name}
                           </option>
                        ))}
                     </select>
                  </div>
               </div>

               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                     O que você comprou?
                  </label>
                  <input
                     type="text"
                     required
                     value={title}
                     onChange={(e) => setTitle(e.target.value)}
                     className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 transition-all"
                     placeholder="Ex: Monitor Gamer"
                  />
               </div>

               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Loja / Estabelecimento
                     </label>
                     <input
                        type="text"
                        required
                        value={store}
                        onChange={(e) => setStore(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 transition-all"
                        placeholder="Ex: Kabum"
                     />
                  </div>
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Data da Compra
                     </label>
                     <input
                        type="date"
                        required
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 transition-all"
                     />
                  </div>
               </div>

               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Valor Total (R$)
                     </label>
                     <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        required
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 transition-all"
                        placeholder="0.00"
                     />
                  </div>
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Parcelas
                     </label>
                     <input
                        type="number"
                        min="1"
                        max="48"
                        required
                        value={installments}
                        onChange={(e) => setInstallments(e.target.value)}
                        className="w-full border border-slate-200 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-purple-600 transition-all"
                     />
                  </div>
               </div>

               <div className="pt-4 mt-6 border-t border-slate-100 flex justify-end gap-3">
                  <button
                     type="button"
                     onClick={onClose}
                     className="px-5 py-2.5 text-slate-600 hover:bg-slate-100 rounded-xl font-medium transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     type="submit"
                     disabled={isSubmitting}
                     className="px-5 py-2.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl font-medium transition-colors disabled:opacity-50"
                  >
                     {isSubmitting ? "Lançando..." : "Lançar Compra"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
