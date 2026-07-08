import { useState } from "react";
import { CheckCircle2, Clock, Ban, CreditCard as CardIcon } from "lucide-react";
import { useInstallments, useCancelPurchase } from "../../hooks/useCredit";
import { PayInstallmentModal } from "./PayInstallmentModal";
import { CancelPurchaseModal } from "../cards/CancelPurchaseModal";
import { Skeleton } from "../ui/Skeleton";
import { EmptyState } from "../ui/EmptyState";

export function InstallmentsList() {
  const { data: installments = [], isLoading: isLoadingInstallments } =
    useInstallments();
  const { mutateAsync: cancelPurchase } = useCancelPurchase();

  const [payingInstallmentId, setPayingInstallmentId] = useState<string | null>(
    null,
  );
  const [cancellingPurchaseId, setCancellingPurchaseId] = useState<
    string | null
  >(null);

  // 1. ESTADO DE LOADING (Skeletons Neumórficos)
  if (isLoadingInstallments) {
    return (
      <div className="mt-12 card-default space-y-4">
        <Skeleton className="h-6 w-48 mb-6" />
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="flex justify-between items-center py-3 border-b border-subtle/20 last:border-0"
          >
            <div className="flex items-center gap-3">
              <Skeleton className="h-10 w-10 rounded-xl" />
              <div className="space-y-1">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-3 w-20" />
              </div>
            </div>
            <Skeleton className="h-8 w-24 rounded-xl" />
          </div>
        ))}
      </div>
    );
  }

  // 2. EMPTY STATE (Sem faturas)
  if (installments.length === 0) {
    return (
      <div className="mt-12 card-default py-12">
        <EmptyState
          icon={Clock}
          title="Nenhuma fatura gerada"
          description="Suas compras parceladas e vencimentos mensais aparecerão aqui no extrato."
        />
      </div>
    );
  }

  // 3. SUCESSO (Tabela Neumórfica Limpa)
  return (
    <div className="mt-12 card-default p-6 sm:p-8">
      <div className="flex items-center justify-between mb-8">
        <h3 className="text-lg lg:text-xl font-bold text-primary tracking-tight">
          Extrato de Faturas
        </h3>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse min-w-150">
          <thead>
            <tr className="border-b border-subtle/20 text-muted text-xs uppercase tracking-[0.15em]">
              <th className="pb-4 font-extrabold pl-2">Detalhes da Compra</th>
              <th className="pb-4 font-extrabold">Parcela</th>
              <th className="pb-4 font-extrabold">Vencimento</th>
              <th className="pb-4 font-extrabold">Valor</th>
              <th className="pb-4 font-extrabold text-right pr-2">
                Ações / Status
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-subtle/15">
            {installments.map((inst) => {
              // ✅ Correção cirúrgica utilizando o campo expected_date oficial da interface
              const formattedDate = inst.expected_date
                ? new Intl.DateTimeFormat("pt-BR", {
                    timeZone: "UTC",
                  }).format(new Date(inst.expected_date))
                : "A definir";

              const formattedValue = new Intl.NumberFormat("pt-BR", {
                style: "currency",
                currency: "BRL",
              }).format(Number(inst.amount));

              return (
                <tr
                  key={inst.id}
                  className="group hover:bg-elevated/40 transition-colors"
                >
                  <td className="py-4 pl-2">
                    <div className="flex items-center gap-3.5">
                      <div className="w-9 h-9 rounded-xl bg-brand/10 border border-brand/15 text-brand flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform duration-200">
                        <CardIcon size={16} />
                      </div>
                      <span
                        className={`font-bold text-sm tracking-tight ${
                          inst.status === "cancelled"
                            ? "text-muted line-through"
                            : "text-primary"
                        }`}
                      >
                        {inst.purchase_title || "Compra Desconhecida"}
                      </span>
                    </div>
                  </td>

                  <td className="py-4 font-semibold text-secondary text-sm">
                    {inst.installment_number} / {inst.total_installments}
                  </td>

                  {/* Renderização da data formatada corretamente */}
                  <td className="py-4 font-medium text-muted text-sm">
                    {formattedDate}
                  </td>

                  <td className="py-4 font-extrabold text-primary text-sm tracking-tight">
                    {formattedValue}
                  </td>

                  <td className="py-4 text-right pr-2">
                    {inst.status === "paid" ? (
                      <span className="inline-flex items-center justify-end gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-[10px] font-extrabold uppercase tracking-[0.15em]">
                        <CheckCircle2 size={13} /> Pago
                      </span>
                    ) : inst.status === "cancelled" ? (
                      <span className="inline-flex items-center justify-end gap-1.5 px-3 py-1 rounded-full bg-elevated text-muted border border-subtle/30 text-[10px] font-extrabold uppercase tracking-[0.15em]">
                        <Ban size={13} /> Cancelado
                      </span>
                    ) : (
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() =>
                            setCancellingPurchaseId(inst.purchase_id)
                          }
                          className="inline-flex items-center justify-center px-3 py-1.5 rounded-xl bg-elevated hover:bg-red-500/10 hover:text-red-500 text-secondary text-xs font-bold transition-all duration-200 cursor-pointer border border-subtle/30"
                        >
                          Cancelar
                        </button>
                        <button
                          onClick={() => setPayingInstallmentId(inst.id)}
                          className="inline-flex items-center justify-center px-4 py-1.5 rounded-xl bg-brand hover:bg-brand-light text-white text-xs font-bold shadow-xs transition-all duration-200 cursor-pointer"
                        >
                          Pagar
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Modais Atômicos da Secção */}
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
