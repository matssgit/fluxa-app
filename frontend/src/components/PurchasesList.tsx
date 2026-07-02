import { ShoppingBag, CheckCircle2, AlertCircle } from "lucide-react";
import { type Purchase, useInstallments } from "../hooks/useCredit";

interface PurchasesListProps {
   purchases: Purchase[];
   onPurchaseClick: (purchase: Purchase) => void;
}

export function PurchasesList({
   purchases,
   onPurchaseClick,
}: PurchasesListProps) {
   // Puxamos as parcelas para calcular o status da compra via frontend
   const { data: allInstallments = [] } = useInstallments();

   if (purchases.length === 0) {
      return (
         <div className="text-center p-6 bg-slate-50 rounded-2xl border border-slate-100 mt-6">
            <div className="w-12 h-12 bg-slate-200 text-slate-400 rounded-full flex items-center justify-center mx-auto mb-3">
               <ShoppingBag size={20} />
            </div>
            <p className="text-sm text-slate-500">
               Nenhuma compra registrada neste cartão.
            </p>
         </div>
      );
   }

   return (
      <div className="mt-8">
         <h3 className="text-sm font-bold text-slate-400 mb-4 uppercase tracking-wider">
            Histórico de Compras
         </h3>
         <div className="space-y-3">
            {purchases.map((purchase) => {
               // Lógica de status baseada nos dados do backend
               const isCancelled = purchase.status === "cancelled";
               const purchaseInstallments = allInstallments.filter(
                  (i) => i.purchase_id === purchase.id,
               );
               const hasPending = purchaseInstallments.some(
                  (i) => i.status === "pending",
               );

               return (
                  <div
                     key={purchase.id}
                     onClick={() => onPurchaseClick(purchase)}
                     className="flex items-center justify-between p-4 bg-white border border-slate-100 rounded-2xl hover:border-purple-200 hover:shadow-md cursor-pointer transition-all group"
                  >
                     <div className="flex items-center gap-4">
                        {/* Ícone de Status Visual */}
                        <div
                           className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors 
                           ${
                              isCancelled
                                 ? "bg-slate-50 text-slate-400"
                                 : hasPending
                                   ? "bg-amber-50 text-amber-600"
                                   : "bg-emerald-50 text-emerald-600"
                           }`}
                        >
                           {isCancelled ? (
                              <ShoppingBag size={18} />
                           ) : hasPending ? (
                              <AlertCircle size={18} />
                           ) : (
                              <CheckCircle2 size={18} />
                           )}
                        </div>
                        <div>
                           <p
                              className={`font-semibold ${
                                 isCancelled
                                    ? "text-slate-400 line-through"
                                    : "text-slate-800"
                              } line-clamp-1`}
                           >
                              {purchase.title}
                           </p>
                           <p className="text-xs text-slate-500 font-medium">
                              {purchase.store} • {purchase.total_installments}x
                           </p>
                        </div>
                     </div>
                     <div className="text-right">
                        <p
                           className={`font-bold ${
                              isCancelled ? "text-slate-400" : "text-slate-800"
                           }`}
                        >
                           {new Intl.NumberFormat("pt-BR", {
                              style: "currency",
                              currency: "BRL",
                           }).format(Number(purchase.total_amount))}
                        </p>
                        {isCancelled && (
                           <p className="text-[10px] font-bold text-slate-400 uppercase mt-0.5 tracking-wider">
                              Cancelada
                           </p>
                        )}
                     </div>
                  </div>
               );
            })}
         </div>
      </div>
   );
}
