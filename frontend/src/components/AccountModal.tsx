import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAccounts } from "../hooks/useAccounts";

const accountSchema = z.object({
   name: z.string().min(2, "O nome da conta é obrigatório"),
   type: z.enum(["checking", "wallet", "savings"]),
});

type AccountForm = z.infer<typeof accountSchema>;

interface AccountModalProps {
   isOpen: boolean;
   onClose: () => void;
}

export function AccountModal({ isOpen, onClose }: AccountModalProps) {
   const { createAccount, isCreating } = useAccounts();

   const {
      register,
      handleSubmit,
      reset,
      formState: { errors },
   } = useForm<AccountForm>({
      resolver: zodResolver(accountSchema),
   });

   // Se o modal estiver fechado, não renderiza nada
   if (!isOpen) return null;

   async function onSubmit(data: AccountForm) {
      await createAccount(data);
      reset(); // Limpa os campos
      onClose(); // Fecha o modal após salvar
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
         {/* Container do Modal */}
         <div className="bg-white rounded-3xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
            {/* Cabeçalho */}
            <div className="flex justify-between items-center p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">Nova Conta</h2>
               <button
                  onClick={onClose}
                  className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-2 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            {/* Formulário */}
            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                     Nome da Conta
                  </label>
                  <input
                     type="text"
                     placeholder="Ex: Nubank, Carteira Física..."
                     {...register("name")}
                     className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none bg-slate-50 focus:bg-white transition-all"
                  />
                  {errors.name && (
                     <span className="text-red-500 text-xs mt-1 block">
                        {errors.name.message}
                     </span>
                  )}
               </div>

               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                     Tipo de Conta
                  </label>
                  <select
                     {...register("type")}
                     className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none bg-slate-50 focus:bg-white transition-all"
                  >
                     <option value="checking">Conta Corrente (Bancos)</option>
                     <option value="wallet">
                        Carteira (Dinheiro em Espécie)
                     </option>
                     <option value="savings">Poupança / Investimentos</option>
                  </select>
               </div>

               {/* Botões de Ação */}
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
                     disabled={isCreating}
                     className="flex-1 px-4 py-3 rounded-xl bg-emerald-500 text-white font-medium hover:bg-emerald-600 disabled:opacity-70 transition-colors"
                  >
                     {isCreating ? "Salvando..." : "Salvar Conta"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
