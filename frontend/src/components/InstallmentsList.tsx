import { useState } from "react";
import { CheckCircle2, Clock, Ban, CreditCard as CardIcon } from "lucide-react";
// 1. Substitua o import antigo pelos hooks atômicos
import { useInstallments, useCancelPurchase } from "../hooks/useCredit";
import { PayInstallmentModal } from "./PayInstallmentModal";
import { CancelPurchaseModal } from "./CancelPurchaseModal";

export function InstallmentsList() {
   // 2. Ajuste a desestruturação para os hooks individuais
   const { data: installments = [], isLoading: isLoadingInstallments } =
      useInstallments();
   const { mutateAsync: cancelPurchase } = useCancelPurchase();

   const [payingInstallmentId, setPayingInstallmentId] = useState<
      string | null
   >(null);
   const [cancellingPurchaseId, setCancellingPurchaseId] = useState<
      string | null
   >(null);

   if (isLoadingInstallments) {
      return (
         <div className="mt-12 text-center text-slate-500 py-8">
            <span className="animate-pulse">Carregando faturas...</span>
         </div>
      );
   }

   // ... (o restante do seu código permanece idêntico)

   if (installments.length === 0) {
      return (
         <div className="mt-12 bg-white rounded-2xl border border-slate-100 p-12 text-center shadow-sm">
            <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4">
               <Clock size={32} />
            </div>
            <p className="text-slate-500 mb-2">
               Você ainda não possui faturas geradas.
            </p>
            <p className="text-sm text-slate-400">
               Suas compras parceladas aparecerão aqui.
            </p>
         </div>
      );
   }

   return (
      <div className="mt-12 bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-slate-100">
         <div className="flex items-center justify-between mb-8">
            <h3 className="text-xl font-bold text-slate-800">
               Extrato de Faturas
            </h3>
         </div>

         <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[600px]">
               <thead>
                  <tr className="border-b border-slate-100 text-slate-400 text-sm">
                     <th className="pb-4 font-medium pl-2">
                        Detalhes da Compra
                     </th>
                     <th className="pb-4 font-medium">Parcela</th>
                     <th className="pb-4 font-medium">Vencimento</th>
                     <th className="pb-4 font-medium">Valor</th>
                     <th className="pb-4 font-medium text-right pr-2">
                        Ações / Status
                     </th>
                  </tr>
               </thead>
               <tbody>
                  {installments.map((inst) => (
                     <tr
                        key={inst.id}
                        className="border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors"
                     >
                        <td className="py-4 pl-2">
                           <div className="flex items-center gap-4">
                              <div className="w-10 h-10 rounded-full bg-purple-50 text-purple-600 flex items-center justify-center flex-shrink-0">
                                 <CardIcon size={18} />
                              </div>
                              <span
                                 className={`font-semibold ${inst.status === "cancelled" ? "text-slate-400 line-through" : "text-slate-700"}`}
                              >
                                 {inst.purchase_title || "Compra Desconhecida"}
                              </span>
                           </div>
                        </td>
                        {/* ... (células da tabela permanecem iguais) ... */}

                        {/* Ação de Cancelamento */}
                        <td className="py-4 text-right pr-2">
                           {inst.status === "paid" ? (
                              <span className="inline-flex items-center justify-end gap-1.5 px-3 py-1.5 rounded-full bg-emerald-50 text-emerald-600 text-xs font-bold uppercase tracking-wider">
                                 <CheckCircle2 size={14} /> Pago
                              </span>
                           ) : inst.status === "cancelled" ? (
                              <span className="inline-flex items-center justify-end gap-1.5 px-3 py-1.5 rounded-full bg-slate-100 text-slate-500 text-xs font-bold uppercase tracking-wider">
                                 <Ban size={14} /> Cancelado
                              </span>
                           ) : (
                              <div className="flex justify-end gap-2">
                                 <button
                                    onClick={() =>
                                       setCancellingPurchaseId(inst.purchase_id)
                                    }
                                    className="inline-flex items-center justify-center px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-bold uppercase tracking-wider transition-colors"
                                 >
                                    Cancelar
                                 </button>
                                 <button
                                    onClick={() =>
                                       setPayingInstallmentId(inst.id)
                                    }
                                    className="inline-flex items-center justify-center px-4 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold uppercase tracking-wider shadow-sm transition-colors"
                                 >
                                    Pagar
                                 </button>
                              </div>
                           )}
                        </td>
                     </tr>
                  ))}
               </tbody>
            </table>
         </div>

         <PayInstallmentModal
            isOpen={!!payingInstallmentId}
            onClose={() => setPayingInstallmentId(null)}
            installmentId={payingInstallmentId}
         />

         <CancelPurchaseModal
            isOpen={!!cancellingPurchaseId}
            onClose={() => setCancellingPurchaseId(null)}
            onConfirm={async () => {
               if (cancellingPurchaseId) {
                  await cancelPurchase(cancellingPurchaseId);
               }
            }}
         />
      </div>
   );
}
