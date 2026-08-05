import { useState } from "react";
import { useAccounts } from "../../../hooks/useAccounts";
import { X, Calendar, CheckCircle2, AlertCircle, Trash2 } from "lucide-react";
import {
  useInstallments,
  usePayInstallment,
  useCancelPurchase,
  type Purchase,
} from "../../../hooks/useCredit";

interface PurchaseDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  purchase: Purchase | null;
}

export function PurchaseDetailsModal({
  isOpen,
  onClose,
  purchase,
}: PurchaseDetailsModalProps) {
  const [selectedAccountId, setSelectedAccountId] = useState("");

  const { accounts = [] } = useAccounts();
  const { data: allInstallments = [] } = useInstallments();

  const { mutate: payInstallment, isPending: isPaying } = usePayInstallment();
  const { mutate: cancelPurchase, isPending: isCancelling } =
    useCancelPurchase();

  if (!isOpen || !purchase) return null;

  const installments = allInstallments.filter(
    (i) => i.purchase_id === purchase.id,
  );
  const isPurchaseCancelled = purchase.status === "cancelled";

  const handlePay = (installmentId: string) => {
    if (!selectedAccountId) return;
    payInstallment({ installmentId, accountId: selectedAccountId });
  };

  const handleCancel = () => {
    if (
      confirm(
        "Tem certeza que deseja cancelar esta compra? O limite das parcelas pendentes será estornado e o histórico mantido.",
      )
    ) {
      cancelPurchase(purchase.id, {
        onSuccess: () => onClose(),
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex justify-between items-center">
          <div>
            <div className="flex items-center gap-2">
              <h2
                className={`text-xl font-bold ${isPurchaseCancelled ? "text-slate-400 line-through" : "text-slate-800"}`}
              >
                {purchase.title}
              </h2>
              {isPurchaseCancelled && (
                <span className="bg-slate-100 text-slate-500 text-xs px-2 py-1 rounded-md font-medium uppercase tracking-wider">
                  Cancelada
                </span>
              )}
            </div>
            <p className="text-sm text-slate-500">{purchase.store}</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-100 rounded-full transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto max-h-[65vh]">
          {/* Seletor de Conta (Oculto se a compra estiver cancelada) */}
          {!isPurchaseCancelled && (
            <div className="mb-6 p-4 bg-slate-50 rounded-xl border border-slate-200">
              <label className="block text-sm font-semibold text-slate-700 mb-2">
                Conta para débito das parcelas
              </label>
              <select
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className="w-full p-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value="" disabled>
                  Selecione uma conta bancária...
                </option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.name}
                  </option>
                ))}
              </select>
              {!selectedAccountId && (
                <p className="text-xs text-amber-600 mt-2 flex items-center gap-1">
                  <AlertCircle size={12} />
                  Selecione uma conta para habilitar o pagamento.
                </p>
              )}
            </div>
          )}

          {/* Lista de Parcelas */}
          <div className="space-y-3">
            {installments.map((inst) => {
              // Se a compra está cancelada e a parcela estava pendente, ela aparece como cancelada também
              const isInstCancelled =
                isPurchaseCancelled && inst.status !== "paid";

              return (
                <div
                  key={inst.id}
                  className="flex items-center justify-between p-4 bg-white rounded-xl border border-slate-100 shadow-sm"
                >
                  <div>
                    <p
                      className={`font-semibold ${isInstCancelled ? "text-slate-400" : "text-slate-800"}`}
                    >
                      Parcela {inst.installment_number} /{" "}
                      {inst.total_installments}
                    </p>
                    <div className="flex items-center gap-2 text-sm text-slate-500 mt-0.5">
                      <Calendar size={14} />
                      {new Date(
                        inst.expected_date + "T12:00:00",
                      ).toLocaleDateString("pt-BR")}
                    </div>
                  </div>

                  <div className="text-right">
                    <p
                      className={`font-bold ${isInstCancelled ? "text-slate-400" : "text-slate-800"}`}
                    >
                      {new Intl.NumberFormat("pt-BR", {
                        style: "currency",
                        currency: "BRL",
                      }).format(Number(inst.amount))}
                    </p>

                    {inst.status === "paid" ? (
                      <span className="text-xs text-emerald-600 font-bold flex items-center justify-end gap-1 mt-1">
                        <CheckCircle2 size={12} /> Pago
                      </span>
                    ) : isInstCancelled ? (
                      <span className="text-xs text-slate-400 font-bold flex items-center justify-end gap-1 mt-1 uppercase">
                        Estornada
                      </span>
                    ) : (
                      <button
                        disabled={isPaying || !selectedAccountId}
                        onClick={() => handlePay(inst.id)}
                        className="text-xs bg-purple-600 text-white px-4 py-1.5 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors mt-1 font-medium"
                      >
                        {isPaying ? "Processando..." : "Pagar Parcela"}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Botão de Cancelamento (Visível apenas se a compra não estiver cancelada) */}
          {!isPurchaseCancelled && (
            <div className="mt-8 pt-6 border-t border-slate-100">
              <button
                onClick={handleCancel}
                disabled={isCancelling}
                className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl font-medium transition-colors disabled:opacity-50"
              >
                <Trash2 size={18} />
                {isCancelling ? "Cancelando..." : "Cancelar Compra"}
              </button>
              <p className="text-center text-xs text-slate-400 mt-2">
                O limite das parcelas em aberto será restaurado.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
