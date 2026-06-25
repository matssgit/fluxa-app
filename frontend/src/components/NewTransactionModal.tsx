import { X } from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAccounts } from "../hooks/useAccounts";
import { useCategories } from "../hooks/useCategories";
import { api } from "../api/client";
import { useQueryClient } from "@tanstack/react-query";

const transactionSchema = z.object({
   type: z.enum(["income", "expense"]),
   title: z.string().min(2, "O título é obrigatório"),
   amount: z.number().min(0.01, "O valor deve ser maior que zero"),
   account_id: z.string().min(1, "Selecione uma conta"),
   category_id: z.string().optional(),
   status: z.enum(["pending", "completed"]), // Agora controlado pelo usuário
   date: z.string().min(1, "A data é obrigatória"), // Captura a data do input (YYYY-MM-DD)
});

type TransactionForm = z.infer<typeof transactionSchema>;

interface NewTransactionModalProps {
   isOpen: boolean;
   onClose: () => void;
}

export function NewTransactionModal({
   isOpen,
   onClose,
}: NewTransactionModalProps) {
   const queryClient = useQueryClient();
   const { accounts } = useAccounts();
   const { categories } = useCategories();

   // Define a data de hoje como padrão para o input no formato YYYY-MM-DD
   const today = new Date().toISOString().split("T")[0];

   const {
      register,
      handleSubmit,
      reset,
      control,
      formState: { errors, isSubmitting },
   } = useForm<TransactionForm>({
      resolver: zodResolver(transactionSchema),
      defaultValues: {
         type: "expense",
         status: "completed", // Padrão: já foi pago/recebido
         date: today,
      },
   });

   const selectedType = useWatch({
      control,
      name: "type",
      defaultValue: "expense",
   });
   const selectedStatus = useWatch({
      control,
      name: "status",
      defaultValue: "completed",
   });

   const filteredCategories = categories.filter(
      (cat) => cat.type === selectedType,
   );

   if (!isOpen) return null;

   async function onSubmit(data: TransactionForm) {
      try {
         const finalAmount =
            data.type === "expense" ? -data.amount : data.amount;

         // Alinhamento Arquitetural:
         // Se concluído -> preenche completed_date. Se pendente -> preenche expected_date.
         const payload = {
            title: data.title,
            amount: finalAmount,
            account_id: data.account_id,
            category_id: data.category_id || undefined,
            status: data.status,
            expected_date: data.status === "pending" ? data.date : undefined,
            completed_date: data.status === "completed" ? data.date : undefined,
         };

         await api.post("/transactions", payload);

         // Invalida os caches para atualizar o dashboard e tabelas
         queryClient.invalidateQueries({ queryKey: ["transactions"] });
         queryClient.invalidateQueries({ queryKey: ["summary"] });

         reset({ type: "expense", status: "completed", date: today });
         onClose();
      } catch (error) {
         console.error("Erro ao criar transação", error);
         alert("Erro ao salvar lançamento.");
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
         <div className="bg-white rounded-3xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Novo Lançamento
               </h2>
               <button
                  onClick={onClose}
                  className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-2 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
               {/* Seletor Tipo (Receita/Despesa) */}
               <div className="flex gap-4 p-1 bg-slate-100 rounded-xl">
                  <label className="flex-1 cursor-pointer">
                     <input
                        type="radio"
                        value="income"
                        {...register("type")}
                        className="peer sr-only"
                     />
                     <div className="text-center py-2 rounded-lg text-sm font-medium text-slate-500 peer-checked:bg-white peer-checked:text-emerald-500 peer-checked:shadow-sm transition-all">
                        Receita
                     </div>
                  </label>
                  <label className="flex-1 cursor-pointer">
                     <input
                        type="radio"
                        value="expense"
                        {...register("type")}
                        className="peer sr-only"
                     />
                     <div className="text-center py-2 rounded-lg text-sm font-medium text-slate-500 peer-checked:bg-white peer-checked:text-red-500 peer-checked:shadow-sm transition-all">
                        Despesa
                     </div>
                  </label>
               </div>

               {/* Descrição e Valor */}
               <div className="grid grid-cols-2 gap-4">
                  <div className="col-span-2">
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Descrição
                     </label>
                     <input
                        type="text"
                        placeholder="Ex: Almoço, Internet, Salário..."
                        {...register("title")}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white"
                     />
                     {errors.title && (
                        <span className="text-red-500 text-xs mt-1 block">
                           {errors.title.message}
                        </span>
                     )}
                  </div>

                  <div className="col-span-2">
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Valor (R$)
                     </label>
                     <input
                        type="number"
                        step="0.01"
                        placeholder="0,00"
                        {...register("amount", { valueAsNumber: true })}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white"
                     />
                     {errors.amount && (
                        <span className="text-red-500 text-xs mt-1 block">
                           {errors.amount.message}
                        </span>
                     )}
                  </div>
               </div>

               {/* Linha Dinâmica: Data e Status */}
               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        {selectedStatus === "completed"
                           ? "Data do Fluxo"
                           : "Data de Vencimento"}
                     </label>
                     <input
                        type="date"
                        {...register("date")}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white text-slate-700"
                     />
                     {errors.date && (
                        <span className="text-red-500 text-xs mt-1 block">
                           {errors.date.message}
                        </span>
                     )}
                  </div>

                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Situação
                     </label>
                     <select
                        {...register("status")}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white text-slate-700"
                     >
                        <option value="completed">✔ Concluído (Pago)</option>
                        <option value="pending">⏳ Pendente (A pagar)</option>
                     </select>
                  </div>
               </div>

               {/* Conta e Categoria */}
               <div className="space-y-4 pt-2">
                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Conta de Origem/Destino *
                     </label>
                     <select
                        {...register("account_id")}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white"
                     >
                        <option value="">Selecione uma conta...</option>
                        {accounts.map((acc) => (
                           <option key={acc.id} value={acc.id}>
                              {acc.name}
                           </option>
                        ))}
                     </select>
                     {errors.account_id && (
                        <span className="text-red-500 text-xs mt-1 block">
                           {errors.account_id.message}
                        </span>
                     )}
                  </div>

                  <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">
                        Categoria
                     </label>
                     <select
                        {...register("category_id")}
                        className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 outline-none bg-slate-50 focus:bg-white"
                     >
                        <option value="">Sem categoria</option>
                        {filteredCategories.map((cat) => (
                           <option key={cat.id} value={cat.id}>
                              {cat.name}
                           </option>
                        ))}
                     </select>
                  </div>
               </div>

               {/* Botões */}
               <div className="pt-2 flex gap-3">
                  <button
                     type="button"
                     onClick={onClose}
                     className="flex-1 px-4 py-3 rounded-xl border border-slate-200 text-slate-700 font-medium hover:bg-slate-50 transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     type="submit"
                     disabled={isSubmitting}
                     className="flex-1 px-4 py-3 rounded-xl bg-emerald-500 text-white font-medium hover:bg-emerald-600 disabled:opacity-70 transition-colors"
                  >
                     {isSubmitting ? "Salvando..." : "Lançar"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
