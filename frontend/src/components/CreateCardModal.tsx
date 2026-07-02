import type { FormEvent } from "react";
import { useState } from "react";
import { X, CreditCard, Landmark, Calendar, DollarSign } from "lucide-react";
import { useCreateCard } from "../hooks/useCredit";
import { CARD_COLORS } from "../utils/cardColors"; // Importando as cores do nosso utilitário

interface CreateCardModalProps {
   isOpen: boolean;
   onClose: () => void;
}

export function CreateCardModal({ isOpen, onClose }: CreateCardModalProps) {
   const { mutate: createCard } = useCreateCard();

   // --- Meus estados para criação do cartão ---
   const [name, setName] = useState("");
   const [brand, setBrand] = useState("");
   const [limitAmount, setLimitAmount] = useState("");
   const [dueDay, setDueDay] = useState("");
   const [color, setColor] = useState("slate"); // Iniciamos o cartão com a cor "slate" (cinza) por padrão
   const [isSubmitting, setIsSubmitting] = useState(false);

   if (!isOpen) return null;

   // --- Minha lógica de salvamento ---
   async function handleSubmit(e: FormEvent) {
      e.preventDefault();
      setIsSubmitting(true);

      try {
         // Passando todos os dados, incluindo a nova propriedade de cor
         await createCard({
            name,
            brand,
            limit_amount: Number(limitAmount),
            due_day: Number(dueDay),
            color, // <--- A cor agora vai pro backend
         });

         // Sucesso! Limpa os campos para a próxima criação
         setName("");
         setBrand("");
         setLimitAmount("");
         setDueDay("");
         setColor("slate"); // Reseta a cor para o padrão

         onClose(); // Fecha o modal
      } catch (error) {
         console.error("Erro ao criar cartão:", error);
         alert("Vish, ocorreu um erro ao salvar o cartão. Verifique os dados.");
      } finally {
         setIsSubmitting(false);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
         <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Header do Modal */}
            <div className="flex items-center justify-between p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Novo Cartão de Crédito
               </h2>
               <button
                  onClick={onClose}
                  className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
                  type="button"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
               {/* Nome / Apelido */}
               <div>
                  <label
                     htmlFor="name"
                     className="block text-sm font-medium text-slate-700 mb-1.5"
                  >
                     Apelido do Cartão
                  </label>
                  <div className="relative">
                     <CreditCard
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                        size={18}
                     />
                     <input
                        id="name"
                        type="text"
                        required
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none transition-all"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="Ex: Nubank, Itaú Black"
                     />
                  </div>
               </div>

               {/* Bandeira */}
               <div>
                  <label
                     htmlFor="brand"
                     className="block text-sm font-medium text-slate-700 mb-1.5"
                  >
                     Bandeira
                  </label>
                  <div className="relative">
                     <Landmark
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                        size={18}
                     />
                     <select
                        id="brand"
                        required
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none appearance-none"
                        value={brand}
                        onChange={(e) => setBrand(e.target.value)}
                     >
                        <option value="" disabled>
                           Selecione a bandeira
                        </option>
                        <option value="Mastercard">Mastercard</option>
                        <option value="Visa">Visa</option>
                        <option value="Elo">Elo</option>
                        <option value="American Express">Amex</option>
                        <option value="Outra">Outra</option>
                     </select>
                  </div>
               </div>

               {/* Grid de Limite e Vencimento */}
               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label
                        htmlFor="limitAmount"
                        className="block text-sm font-medium text-slate-700 mb-1.5"
                     >
                        Limite Total
                     </label>
                     <div className="relative">
                        <DollarSign
                           className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                           size={18}
                        />
                        <input
                           id="limitAmount"
                           type="number"
                           step="0.01"
                           min="0"
                           required
                           className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none transition-all"
                           value={limitAmount}
                           onChange={(e) => setLimitAmount(e.target.value)}
                           placeholder="0.00"
                        />
                     </div>
                  </div>

                  <div>
                     <label
                        htmlFor="dueDay"
                        className="block text-sm font-medium text-slate-700 mb-1.5"
                     >
                        Vencimento (Dia)
                     </label>
                     <div className="relative">
                        <Calendar
                           className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                           size={18}
                        />
                        <input
                           id="dueDay"
                           type="number"
                           min="1"
                           max="31"
                           required
                           className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 outline-none transition-all"
                           value={dueDay}
                           onChange={(e) => setDueDay(e.target.value)}
                           placeholder="Ex: 5"
                        />
                     </div>
                  </div>
               </div>

               {/* Meus seletores de cor para o cartão novo */}
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
                           title={c.label}
                           className={`w-8 h-8 rounded-full transition-all shadow-sm ${c.bgClass} ${
                              color === c.id
                                 ? "ring-2 ring-offset-2 ring-purple-600 scale-110"
                                 : "opacity-60 hover:opacity-100 hover:scale-105"
                           }`}
                        />
                     ))}
                  </div>
               </div>

               {/* Ações do Footer */}
               <div className="pt-4 flex gap-3 mt-2">
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
                     className="flex-1 bg-purple-600 hover:bg-purple-700 text-white px-4 py-3 rounded-xl font-medium transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                     {isSubmitting ? "Salvando..." : "Salvar Cartão"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
