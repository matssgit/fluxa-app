import { AlertTriangle } from "lucide-react";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "../../ui/Modal";
import { useUpdateSubscriptionStatus } from "../../../hooks/useSubscriptions";

interface CancelSubscriptionModalProps {
  isOpen: boolean;
  onClose: () => void;
  subscriptionId: string | null;
}

export function CancelSubscriptionModal({
  isOpen,
  onClose,
  subscriptionId,
}: CancelSubscriptionModalProps) {
  const { mutate: updateStatus, isPending } = useUpdateSubscriptionStatus();

  if (!isOpen || !subscriptionId) return null;

  function handleConfirm() {
    if (!subscriptionId) return;
    updateStatus(
      { id: subscriptionId, status: "cancelled" },
      { onSuccess: () => onClose() },
    );
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="sm">
      <ModalHeader title="Cancelar Assinatura" onClose={onClose} />

      <ModalBody className="p-6">
        <div className="flex flex-col items-center text-center gap-4">
          <div className="w-14 h-14 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mb-2 ring-4 ring-red-500/5">
            <AlertTriangle size={28} />
          </div>

          <h3 className="text-xl font-extrabold text-primary tracking-tight">
            Tem certeza que deseja cancelar?
          </h3>

          <p className="text-sm font-medium text-muted leading-relaxed">
            Ao cancelar, o serviço será encerrado e esta assinatura{" "}
            <strong className="text-secondary">não poderá ser reativada</strong>
            .
          </p>

          <div className="p-3 bg-elevated/50 rounded-xl border border-subtle/30 mt-2">
            <p className="text-[11px] font-semibold text-muted leading-relaxed">
              Os pagamentos já realizados continuarão registrados normalmente no
              seu histórico financeiro do Caixa.
            </p>
          </div>
        </div>
      </ModalBody>

      <ModalFooter>
        <div className="flex gap-3 w-full">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-sm font-bold transition-all cursor-pointer"
          >
            Voltar
          </button>
          <button
            onClick={handleConfirm}
            disabled={isPending}
            className="flex-1 px-4 py-3 rounded-xl bg-red-500 hover:bg-red-600 text-white text-sm font-bold transition-all disabled:opacity-50 cursor-pointer shadow-sm shadow-red-500/20 flex items-center justify-center gap-2"
          >
            {isPending ? (
              <span className="animate-pulse">A Cancelar...</span>
            ) : (
              "Cancelar Assinatura"
            )}
          </button>
        </div>
      </ModalFooter>
    </Modal>
  );
}
