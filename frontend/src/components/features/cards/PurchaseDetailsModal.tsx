import { useState } from "react";
import { useAccounts } from "../../../hooks/useAccounts";
import { Calendar, CheckCircle2, AlertCircle, Trash2 } from "lucide-react";
import { Modal, ModalBody, ModalHeader } from "../../ui/Modal";
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
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <ModalHeader
        title={purchase.title}
        description={purchase.store}
        onClose={onClose}
      />
      <ModalBody>
        <div className="space-y-6">
          {isPurchaseCancelled && (
            <div className="flex items-center gap-2">
              <span className="badge badge-neutral uppercase tracking-wider">
                Compra cancelada
              </span>
            </div>
          )}
          {/* Seletor de Conta (Oculto se a compra estiver cancelada) */}
          {!isPurchaseCancelled && (
            <div className="p-4 bg-surface-muted rounded-2xl border border-border">
              <label className="block text-xs font-extrabold uppercase tracking-wider text-secondary mb-2">
                Conta para débito das parcelas
              </label>
              <select
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className="w-full p-3 bg-surface border border-border rounded-xl text-sm text-primary"
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
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 bg-surface rounded-2xl border border-border shadow-xs"
                >
                  <div>
                    <p
                      className={`font-semibold ${isInstCancelled ? "text-muted line-through" : "text-primary"}`}
                    >
                      Parcela {inst.installment_number} /{" "}
                      {inst.total_installments}
                    </p>
                    <div className="flex items-center gap-2 text-sm text-muted mt-0.5">
                      <Calendar size={14} />
                      {new Date(
                        inst.expected_date + "T12:00:00",
                      ).toLocaleDateString("pt-BR")}
                    </div>
                  </div>

                  <div className="text-left sm:text-right">
                    <p
                      className={`font-bold ${isInstCancelled ? "text-muted" : "text-primary"}`}
                    >
                      {new Intl.NumberFormat("pt-BR", {
                        style: "currency",
                        currency: "BRL",
                      }).format(Number(inst.amount))}
                    </p>

                    {inst.status === "paid" ? (
                      <span className="text-xs text-income font-bold flex items-center sm:justify-end gap-1 mt-1">
                        <CheckCircle2 size={12} /> Pago
                      </span>
                    ) : isInstCancelled ? (
                      <span className="text-xs text-muted font-bold flex items-center sm:justify-end gap-1 mt-1 uppercase">
                        Estornada
                      </span>
                    ) : (
                      <button
                        disabled={isPaying || !selectedAccountId}
                        onClick={() => handlePay(inst.id)}
                        className="btn btn-primary min-h-9 px-4 mt-2 text-xs"
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
            <div className="pt-6 border-t border-subtle">
              <button
                onClick={handleCancel}
                disabled={isCancelling}
                className="btn btn-danger w-full px-4"
              >
                <Trash2 size={18} />
                {isCancelling ? "Cancelando..." : "Cancelar Compra"}
              </button>
              <p className="text-center text-xs text-muted mt-2">
                O limite das parcelas em aberto será restaurado.
              </p>
            </div>
          )}
        </div>
      </ModalBody>
    </Modal>
  );
}
