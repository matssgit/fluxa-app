import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import {
   transactionSchema,
   type TransactionFormData,
} from "../schemas/transactionSchema";
import { useTransactions } from "../hooks/useTransactions";
import { useAccounts } from "../hooks/useAccounts";
import { useCategories } from "../hooks/useCategories";

export function TransactionForm() {
   const { createTransaction, isCreating } = useTransactions();
   const { accounts } = useAccounts();
   const { categories } = useCategories();

   const {
      register,
      handleSubmit,
      reset,
      formState: { errors },
   } = useForm<TransactionFormData>({
      resolver: zodResolver(transactionSchema),
      defaultValues: { type: "entrada" },
   });

   const onSubmit = async (data: TransactionFormData) => {
      try {
         await createTransaction({
            ...data,
            status: "completed",
         } as TransactionFormData & { status: string });

         reset();
      } catch (error) {
         console.error("Erro ao criar transação", error);
      }
   };

   return (
      <form
         onSubmit={handleSubmit(onSubmit)}
         className="bg-premium-card p-6 rounded-2xl mb-8 space-y-5"
      >
         <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div className="w-full">
               <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                  Descrição
               </label>
               <input
                  type="text"
                  placeholder="Ex: Salário, Mercado..."
                  {...register("title")}
                  className="input-premium w-full px-4 py-3 rounded-xl outline-none"
               />
               {errors.title && (
                  <span className="text-finance-saida text-xs mt-1.5 block font-medium">
                     {errors.title.message}
                  </span>
               )}
            </div>

            <div className="w-full">
               <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                  Valor
               </label>
               <input
                  type="number"
                  step="0.01"
                  placeholder="0,00"
                  {...register("amount", { valueAsNumber: true })}
                  className="input-premium w-full px-4 py-3 rounded-xl outline-none"
               />
               {errors.amount && (
                  <span className="text-finance-saida text-xs mt-1.5 block font-medium">
                     {errors.amount.message}
                  </span>
               )}
            </div>

            <div className="w-full">
               <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                  Tipo
               </label>
               <div className="relative">
                  <select
                     {...register("type")}
                     className="input-premium w-full px-4 py-3 rounded-xl outline-none appearance-none cursor-pointer font-medium"
                  >
                     <option value="entrada">Receita</option>
                     <option value="saida">Despesa</option>
                  </select>
               </div>
            </div>
         </div>

         <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="w-full">
               <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                  Conta
               </label>
               <div className="relative">
                  <select
                     {...register("account_id")}
                     className="input-premium w-full px-4 py-3 rounded-xl outline-none appearance-none cursor-pointer font-medium"
                  >
                     <option value="">Selecione onde...</option>
                     {accounts?.map((acc: { id: string; name: string }) => (
                        <option key={acc.id} value={acc.id}>
                           {acc.name}
                        </option>
                     ))}
                  </select>
                  {errors.account_id && (
                     <span className="text-finance-saida text-xs mt-1.5 block font-medium">
                        {errors.account_id.message}
                     </span>
                  )}
               </div>
            </div>

            <div className="w-full">
               <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                  Categoria
               </label>
               <div className="relative">
                  <select
                     {...register("category_id")}
                     className="input-premium w-full px-4 py-3 rounded-xl outline-none appearance-none cursor-pointer font-medium"
                  >
                     <option value="">Selecione do que se trata...</option>
                     {categories?.map((cat: { id: string; name: string }) => (
                        <option key={cat.id} value={cat.id}>
                           {cat.name}
                        </option>
                     ))}
                  </select>
                  {errors.category_id && (
                     <span className="text-finance-saida text-xs mt-1.5 block font-medium">
                        {errors.category_id.message}
                     </span>
                  )}
               </div>
            </div>
         </div>

         <div className="flex justify-end pt-4 mt-2 border-t border-finance-primary/10">
            <button
               type="submit"
               disabled={isCreating}
               className="btn-premium-primary w-full md:w-auto px-8 py-3 rounded-xl flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
            >
               {isCreating ? (
                  "Salvando..."
               ) : (
                  <>
                     <Plus size={18} /> Adicionar Lançamento
                  </>
               )}
            </button>
         </div>
      </form>
   );
}
