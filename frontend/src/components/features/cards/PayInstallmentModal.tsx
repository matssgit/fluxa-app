import { useState } from "react";
import { CheckCircle2, Wallet } from "lucide-react";
import { useAccounts } from "../../../hooks/useAccounts";
import { usePayInstallment } from "../../../hooks/useCredit";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "../../ui/Modal";

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
  const { mutateAsync: payInstallment } = usePayInstallment();
  const { accounts = [], isLoading: isLoadingAccounts } = useAccounts();

  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handlePay(e: React.FormEvent) {
    e.preventDefault();

    if (!installmentId) return;
    if (!selectedAccountId)
      return alert("Selecione uma conta para o pagamento.");

    try {
      setIsSubmitting(true);
      await payInstallment({
        installmentId,
        accountId: selectedAccountId,
      });
      onClose();
      setSelectedAccountId("");
    } catch (error) {
      console.error("Erro ao pagar fatura:", error);
      alert("Ocorreu um erro ao registrar o pagamento.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal isOpen={isOpen && !!installmentId} onClose={onClose} size="md">
      <ModalHeader
        title="Pagar Parcela"
        description="O valor será debitado do seu saldo no Dashboard."
        onClose={onClose}
      />

      <form onSubmit={handlePay}>
        <ModalBody>
          <div>
            <label className="block text-sm font-semibold text-primary mb-2">
              De onde o dinheiro vai sair?
            </label>
            <div className="relative">
              <Wallet
                className="absolute left-4 top-1/2 -translate-y-1/2 text-muted"
                size={18}
              />
              <select
                required
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className="input pl-11 appearance-none cursor-pointer"
              >
                <option value="" disabled>
                  Selecione uma conta...
                </option>
                {isLoadingAccounts ? (
                  <option disabled>Carregando contas...</option>
                ) : (
                  accounts.map((account: { id: string; name: string }) => (
                    <option key={account.id} value={account.id}>
                      {account.name}
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>
        </ModalBody>

        <ModalFooter>
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting || !selectedAccountId}
            className="btn-primary"
          >
            {isSubmitting ? (
              <span className="animate-pulse">Processando...</span>
            ) : (
              <>
                <CheckCircle2 size={18} /> Confirmar Pagamento
              </>
            )}
          </button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
