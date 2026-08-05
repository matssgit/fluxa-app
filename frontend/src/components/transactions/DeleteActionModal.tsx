import { useState } from "react";
import { AlertTriangle, Trash2 } from "lucide-react";
import { Modal, ModalBody, ModalFooter } from "../ui/Modal";

interface DeleteActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  title: string;
  description: string;
  warningText?: string;
}

export function DeleteActionModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  warningText,
}: DeleteActionModalProps) {
  const [confirmText, setConfirmText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isTypingCorrect = confirmText.trim().toUpperCase() === "EXCLUIR";

  async function handleConfirm() {
    if (!isTypingCorrect) return;
    try {
      setIsSubmitting(true);
      await onConfirm();
      setConfirmText("");
      onClose();
    } catch (error) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || "Ocorreu um erro ao excluir.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleClose() {
    setConfirmText("");
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="sm">
      <ModalBody className="p-6">
        <div className="flex flex-col items-center text-center">
          <div className="w-16 h-16 bg-danger/10 text-danger rounded-2xl flex items-center justify-center mb-4">
            <AlertTriangle size={32} />
          </div>
          <h2 className="text-xl font-bold text-primary">{title}</h2>
          <p className="text-sm text-muted mt-2">{description}</p>
        </div>

        <div className="mt-6">
          {/* Texto de aviso extra (se houver) */}
          {warningText && (
            <div className="bg-amber-500/10 text-amber-500 text-xs p-4 rounded-xl font-medium mb-6">
              {warningText}
            </div>
          )}

          {/* Campo de confirmação */}
          <label className="block text-sm font-medium text-primary mb-2 text-center">
            Para confirmar, digite{" "}
            <strong className="text-danger select-none">EXCLUIR</strong> abaixo:
          </label>
          <input
            type="text"
            placeholder="EXCLUIR"
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            className="w-full px-4 py-3 bg-surface border border-subtle/30 rounded-xl focus:ring-2 focus:ring-danger/20 focus:border-danger outline-none text-center font-bold uppercase tracking-widest transition-all text-primary shadow-2xs"
          />
        </div>
      </ModalBody>

      <ModalFooter className="flex gap-3">
        <button
          onClick={handleClose}
          className="flex-1 px-4 py-3 bg-elevated hover:bg-subtle/40 text-secondary border border-subtle/30 rounded-xl transition-all cursor-pointer font-bold text-sm"
        >
          Cancelar
        </button>
        <button
          onClick={handleConfirm}
          disabled={!isTypingCorrect || isSubmitting}
          className="flex-1 bg-danger hover:bg-danger/90 text-white px-4 py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all disabled:opacity-40 disabled:cursor-not-allowed shadow-sm cursor-pointer"
        >
          {isSubmitting ? (
            <span className="animate-pulse">Excluindo...</span>
          ) : (
            <>
              <Trash2 size={16} /> Confirmar
            </>
          )}
        </button>
      </ModalFooter>
    </Modal>
  );
}
