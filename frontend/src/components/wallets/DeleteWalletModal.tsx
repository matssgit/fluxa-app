import { AlertTriangle } from "lucide-react";
import { useDeleteWallet } from "../../hooks/useWallets";
import type { Wallet } from "../../types/wallet";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "../ui/Modal";

interface DeleteWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
  wallet: Wallet | null;
}

export function DeleteWalletModal({
  isOpen,
  onClose,
  wallet,
}: DeleteWalletModalProps) {
  const { mutateAsync: deleteWallet, isPending } = useDeleteWallet();

  if (!isOpen || !wallet) return null;

  async function handleDelete() {
    try {
      await deleteWallet(wallet!.id);
      onClose();
    } catch (error) {
      console.error("Erro ao excluir meta:", error);
      alert("Não foi possível excluir a meta.");
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="sm">
      <ModalHeader title="Excluir Objetivo" onClose={onClose} />

      <ModalBody className="p-6 text-center">
        <div className="w-16 h-16 rounded-full bg-danger/10 text-danger flex items-center justify-center mx-auto mb-4">
          <AlertTriangle size={32} />
        </div>
        <h3 className="text-lg font-bold text-primary mb-2">
          Deseja excluir "{wallet.title}"?
        </h3>
        <p className="text-sm text-secondary font-medium">
          Isso apagará a meta e todo o histórico de progresso. Lembre-se:{" "}
          <strong className="text-primary">
            isto não afeta o saldo das suas contas
          </strong>
          , pois metas registam apenas evolução.
        </p>
      </ModalBody>

      <ModalFooter>
        <div className="flex gap-3 w-full">
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-sm font-bold transition-all cursor-pointer"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleDelete}
            disabled={isPending}
            className="flex-1 px-4 py-3 rounded-xl bg-danger hover:bg-red-500 text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer"
          >
            {isPending ? "Excluindo..." : "Sim, Excluir"}
          </button>
        </div>
      </ModalFooter>
    </Modal>
  );
}
