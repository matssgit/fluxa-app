import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import {
   transactionSchema,
   type TransactionFormData,
} from "../schemas/transactionSchema";
import { useTransactions } from "../hooks/useTransactions";

export function TransactionForm() {
   const { createTransaction, isCreating } = useTransactions();

   const {
      register,
      handleSubmit,
      reset,
      formState: { errors },
   } = useForm<TransactionFormData>({
      resolver: zodResolver(transactionSchema),
      defaultValues: { type: "credit" },
   });

   const onSubmit = async (data: TransactionFormData) => {
      try {
         await createTransaction(data);
         reset(); // Limpa o formulário após sucesso
      } catch (error) {
         console.error(error);
      }
   };

   return (
      <form
         onSubmit={handleSubmit(onSubmit)}
         className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 mb-8 flex flex-col md:flex-row gap-4 items-start md:items-end"
      >
         <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-slate-700 mb-1">
               Descrição
            </label>
            <input
               type="text"
               placeholder="Ex: Salário, Mercado..."
               {...register("title")}
               className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-emerald-500 outline-none transition-all"
            />
            {errors.title && (
               <span className="text-red-500 text-xs mt-1 block">
                  {errors.title.message}
               </span>
            )}
         </div>

         <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-slate-700 mb-1">
               Valor
            </label>
            <input
               type="number"
               step="0.01"
               placeholder="0,00"
               // O SEGREDO ESTÁ AQUI NA LINHA ABAIXO:
               {...register("amount", { valueAsNumber: true })}
               className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-emerald-500 outline-none transition-all"
            />
            {errors.amount && (
               <span className="text-red-500 text-xs mt-1 block">
                  {errors.amount.message}
               </span>
            )}
         </div>

         <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-slate-700 mb-1">
               Tipo
            </label>
            <div className="relative">
               <select
                  {...register("type")}
                  className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-emerald-500 outline-none transition-all appearance-none cursor-pointer"
               >
                  <option value="credit">Receita</option>
                  <option value="debit">Despesa</option>
               </select>
            </div>
         </div>

         <button
            type="submit"
            disabled={isCreating}
            className="w-full md:w-auto px-6 py-3 bg-slate-900 text-white rounded-xl font-medium hover:bg-slate-800 transition-colors flex items-center justify-center gap-2 disabled:opacity-70"
         >
            {isCreating ? (
               "Salvando..."
            ) : (
               <>
                  <Plus size={20} /> Adicionar
               </>
            )}
         </button>
      </form>
   );
}
