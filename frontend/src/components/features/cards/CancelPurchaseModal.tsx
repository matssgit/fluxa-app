import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui/Modal";

interface CancelPurchaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

export function CancelPurchaseModal({
  isOpen,
  onClose,
  onConfirm,
}: CancelPurchaseModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  async function handleCancel() {
    try {
      setIsSubmitting(true);
      await onConfirm();
      onClose();
    } catch (error) {
      console.error(error);
      alert("Erro ao processar o cancelamento da compra.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="md">
      <ModalHeader
        title="Cancelar compra"
        description="Revise o impacto antes de confirmar"
        onClose={onClose}
      />
      <ModalBody className="space-y-5">
        <div className="w-14 h-14 bg-warning/10 text-warning rounded-2xl flex items-center justify-center mx-auto border border-warning/20">
          <AlertTriangle size={24} />
        </div>
          <p className="text-sm text-secondary text-center leading-relaxed">
            Você está cancelando esta compra. <br />
            As parcelas futuras serão canceladas e o limite correspondente será
            liberado. <br />
            <strong className="text-expense">
              Esta ação não poderá ser desfeita.
            </strong>
          </p>
      </ModalBody>
      <ModalFooter className="grid grid-cols-2">
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="btn btn-secondary px-4"
            >
              Voltar
            </button>
            <button
              onClick={handleCancel}
              disabled={isSubmitting}
              className="btn btn-danger px-4"
            >
              {isSubmitting ? "Cancelando..." : "Confirmar Cancelamento"}
            </button>
      </ModalFooter>
    </Modal>
  );
}
