import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAccounts } from "../hooks/useAccounts";
import { usePaySubscription } from "../hooks/useSubscriptions";

const paySchema = z.object({
   account_id: z.string().uuid("Selecione uma conta"),
});

type PayForm = z.infer<typeof paySchema>;

interface Props {
   isOpen: boolean;
   onClose: () => void;
   subscriptionId: string | null;
}

export function PaySubscriptionModal({
   isOpen,
   onClose,
   subscriptionId,
}: Props) {
   const { accounts } = useAccounts();
   const { mutateAsync: paySub, isPending } = usePaySubscription();

   const {
      register,
      handleSubmit,
      reset,
      formState: { errors },
   } = useForm<PayForm>({
      resolver: zodResolver(paySchema),
   });

   if (!isOpen || !subscriptionId) return null;

   async function onSubmit(data: PayForm) {
      try {
         await paySub({ id: subscriptionId!, account_id: data.account_id });
         reset();
         onClose();
      } catch (error) {
         console.error("Erro ao registrar pagamento da assinatura", error);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
         <div className="bg-white rounded-3xl w-full max-w-md shadow-xl overflow-hidden">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center">
               <h2 className="text-xl font-bold text-slate-800">
                  Baixar Assinatura
               </h2>
               <button
                  onClick={onClose}
                  className="p-2 text-slate-400 hover:bg-slate-100 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                     De qual conta o dinheiro saiu?
                  </label>
                  <select
                     {...register("account_id")}
                     className="w-full rounded-xl border border-slate-200 px-4 py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  >
                     <option value="">Selecione a conta...</option>
                     {accounts?.map((acc: { id: string; name: string }) => (
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

               <button
                  type="submit"
                  disabled={isPending}
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-medium py-3 rounded-xl mt-6 transition-colors disabled:opacity-70"
               >
                  {isPending ? "Processando..." : "Confirmar Pagamento"}
               </button>
            </form>
         </div>
      </div>
   );
}
