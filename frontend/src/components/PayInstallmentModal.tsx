import { useState } from "react";
import { X, CheckCircle2, Wallet } from "lucide-react";
import { usePayInstallment } from "../hooks/useCredit";
import { useAccounts } from "../hooks/useAccounts";

interface PayInstallmentModalProps {
   isOpen: boolean;
   onClose: () => void;
   installmentId: string | null;
}

export function PayInstallmentModal({
   isOpen,
   onClose,
   installmentId,
}: PayInstallmentModalProps) {
   // 1. Usando o novo hook atômico
   const { mutateAsync: payInstallment } = usePayInstallment();

   // 2. Ajuste na desestruturação (mesma correção que fizemos no outro modal)
   const { accounts = [], isLoading: isLoadingAccounts } = useAccounts();

   const [selectedAccountId, setSelectedAccountId] = useState("");
   const [isSubmitting, setIsSubmitting] = useState(false);

   if (!isOpen || !installmentId) return null;

   async function handlePay(e: React.FormEvent) {
      e.preventDefault();

      // 1. O TypeScript agora tem 100% de certeza que installmentId não é null
      if (!installmentId) return;

      if (!selectedAccountId)
         return alert("Selecione uma conta para o pagamento.");

      try {
         setIsSubmitting(true);
         await payInstallment({
            installmentId, // Sem o erro do null!
            accountId: selectedAccountId,
         });
         onClose();
         setSelectedAccountId(""); // reseta o estado
      } catch (error) {
         console.error("Erro ao pagar fatura:", error);
         alert("Ocorreu um erro ao registrar o pagamento.");
      } finally {
         setIsSubmitting(false);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
         <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Pagar Parcela
               </h2>
               <button
                  onClick={onClose}
                  className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handlePay} className="p-6">
               <div className="mb-6">
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                     De onde o dinheiro vai sair?
                  </label>
                  <div className="relative">
                     <Wallet
                        className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                        size={20}
                     />
                     <select
                        required
                        value={selectedAccountId}
                        onChange={(e) => setSelectedAccountId(e.target.value)}
                        className="w-full pl-12 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600/20 focus:border-purple-600 appearance-none"
                     >
                        <option value="" disabled>
                           Selecione uma conta...
                        </option>
                        {isLoadingAccounts ? (
                           <option disabled>Carregando contas...</option>
                        ) : (
                           accounts.map((account) => (
                              <option key={account.id} value={account.id}>
                                 {account.name}
                              </option>
                           ))
                        )}
                     </select>
                  </div>
                  <p className="text-xs text-slate-500 mt-2">
                     O valor da parcela será debitado desta conta no seu
                     Dashboard geral.
                  </p>
               </div>

               <div className="flex gap-3 pt-2">
                  <button
                     type="button"
                     onClick={onClose}
                     className="flex-1 px-4 py-3 text-slate-600 font-medium hover:bg-slate-100 rounded-xl transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     type="submit"
                     disabled={isSubmitting || !selectedAccountId}
                     className="flex-1 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed text-white px-4 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors shadow-sm"
                  >
                     {isSubmitting ? (
                        <span className="animate-pulse">Processando...</span>
                     ) : (
                        <>
                           <CheckCircle2 size={20} /> Confirmar Pagamento
                        </>
                     )}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
