import { useState } from "react";
import { useDeleteAccount } from "../../../hooks/useAccounts";
import { Modal, ModalBody, ModalFooter } from "../../ui/Modal";
import {
  AlertTriangle,
  Trash2,
  ArrowRightLeft,
  CreditCard,
  Repeat,
} from "lucide-react";

interface AccountData {
  id: string;
  name: string;
}

interface DeleteAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  account?: AccountData | null;
}

interface ConflictData {
  transactions: number;
  cards: number;
  subscriptions: number;
}

interface ApiError {
  response?: {
    status?: number;
    data?: {
      message?: string;
      conflicts?: ConflictData;
    };
  };
}

export function DeleteAccountModal({
  isOpen,
  onClose,
  account,
}: DeleteAccountModalProps) {
  const { mutateAsync: deleteAccount } = useDeleteAccount();
  const [confirmText, setConfirmText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [conflicts, setConflicts] = useState<ConflictData | null>(null);

  const isTypingCorrect = confirmText.trim().toUpperCase() === "EXCLUIR";

  async function handleConfirm() {
    if (!isTypingCorrect || !account) return;
    try {
      setIsSubmitting(true);
      setConflicts(null);
      await deleteAccount(account.id);
      handleClose();
    } catch (error: unknown) {
      const err = error as ApiError;

      if (err.response?.status === 409 && err.response.data?.conflicts) {
        setConflicts(err.response.data.conflicts);
      } else {
        alert(
          err.response?.data?.message || "Ocorreu um erro ao excluir a conta.",
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleClose() {
    setConfirmText("");
    setConflicts(null);
    onClose();
  }

  if (!isOpen || !account) return null;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="sm">
      <ModalBody className="p-6">
        <div className="flex flex-col items-center text-center">
          <div className="w-16 h-16 bg-danger/10 text-danger rounded-2xl flex items-center justify-center mb-4 shadow-sm">
            <AlertTriangle size={32} />
          </div>
          <h2 className="text-xl font-bold text-primary">Excluir Conta</h2>
          <p className="text-sm text-muted mt-2">
            Você está prestes a excluir a conta{" "}
            <strong className="text-primary">{account.name}</strong>.
          </p>
        </div>

        <div className="mt-6">
          {conflicts ? (
            <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-5 text-left animate-in fade-in zoom-in-95">
              <h3 className="text-amber-600 font-bold text-sm mb-3">
                Exclusão Bloqueada
              </h3>
              <p className="text-secondary text-xs font-medium mb-4">
                Para preservar o seu histórico, remova ou transfira os seguintes
                vínculos antes de excluir esta conta:
              </p>
              <ul className="space-y-2.5">
                {conflicts.transactions > 0 && (
                  <li className="flex items-center gap-2.5 text-xs text-amber-700 font-bold bg-amber-500/10 px-3 py-2 rounded-xl">
                    <ArrowRightLeft size={16} /> {conflicts.transactions}{" "}
                    lançamentos no caixa
                  </li>
                )}
                {conflicts.cards > 0 && (
                  <li className="flex items-center gap-2.5 text-xs text-amber-700 font-bold bg-amber-500/10 px-3 py-2 rounded-xl">
                    <CreditCard size={16} /> {conflicts.cards} cartões de
                    crédito associados
                  </li>
                )}
                {conflicts.subscriptions > 0 && (
                  <li className="flex items-center gap-2.5 text-xs text-amber-700 font-bold bg-amber-500/10 px-3 py-2 rounded-xl">
                    <Repeat size={16} /> {conflicts.subscriptions} assinaturas
                    vinculadas
                  </li>
                )}
              </ul>
            </div>
          ) : (
            <>
              <div className="bg-amber-500/10 text-amber-600 border border-amber-500/20 text-xs p-4 rounded-xl font-semibold mb-6 text-center">
                Esta ação é irreversível e só será permitida se não houverem
                históricos atrelados.
              </div>

              <label className="block text-sm font-medium text-primary mb-2 text-center">
                Para confirmar, digite{" "}
                <strong className="text-danger select-none">EXCLUIR</strong>{" "}
                abaixo:
              </label>
              <input
                type="text"
                placeholder="EXCLUIR"
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                className="w-full px-4 py-3 bg-surface border border-subtle/30 rounded-xl focus:ring-2 focus:ring-danger/20 focus:border-danger outline-none text-center font-bold uppercase tracking-widest transition-all text-primary shadow-2xs"
              />
            </>
          )}
        </div>
      </ModalBody>

      <ModalFooter className="flex gap-3">
        {conflicts ? (
          <button
            onClick={handleClose}
            className="w-full px-4 py-3 bg-elevated hover:bg-subtle/40 text-secondary border border-subtle/30 rounded-xl transition-all cursor-pointer font-bold text-sm"
          >
            Ok, entendi
          </button>
        ) : (
          <>
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
                <span className="animate-pulse">A Excluir...</span>
              ) : (
                <>
                  <Trash2 size={16} /> Confirmar
                </>
              )}
            </button>
          </>
        )}
      </ModalFooter>
    </Modal>
  );
}
