import { ShoppingBag, CheckCircle2, AlertCircle } from "lucide-react";
import { type Purchase, useInstallments } from "../hooks/useCredit";
import { EmptyState } from "./ui/EmptyState";

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
      <div className="card-default py-8 mt-6">
        <EmptyState
          icon={ShoppingBag}
          title="Nenhuma compra registrada"
          description="As compras realizadas neste cartão aparecerão aqui no histórico."
        />
      </div>
    );
  }

  return (
    <div className="mt-8 space-y-4">
      <h3 className="text-xs font-extrabold text-muted uppercase tracking-[0.18em]">
        Histórico de Compras
      </h3>

      <div className="space-y-3">
        {purchases.map((purchase) => {
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
              className="card-interactive group flex items-center justify-between p-4 min-h-20"
            >
              <div className="flex items-center gap-4">
                {/* Ícone de Status Visual com opacidade do Design System */}
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center transition-colors shadow-2xs border ${
                    isCancelled
                      ? "bg-elevated/80 text-muted border-subtle/30"
                      : hasPending
                        ? "bg-amber-500/10 text-amber-500 border-amber-500/20"
                        : "bg-emerald-500/10 text-emerald-500 border-emerald-500/20"
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
                    className={`font-bold tracking-tight text-sm sm:text-base ${
                      isCancelled ? "text-muted line-through" : "text-primary"
                    } line-clamp-1`}
                  >
                    {purchase.title}
                  </p>
                  <p className="text-xs text-muted font-medium mt-0.5">
                    {purchase.store} • {purchase.total_installments}x
                  </p>
                </div>
              </div>

              <div className="text-right">
                <p
                  className={`font-extrabold tracking-tight text-sm sm:text-base ${
                    isCancelled ? "text-muted" : "text-primary"
                  }`}
                >
                  {new Intl.NumberFormat("pt-BR", {
                    style: "currency",
                    currency: "BRL",
                  }).format(Number(purchase.total_amount))}
                </p>
                {isCancelled && (
                  <p className="text-[10px] font-extrabold text-muted uppercase mt-0.5 tracking-[0.15em]">
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
