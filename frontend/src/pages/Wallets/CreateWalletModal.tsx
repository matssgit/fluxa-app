import { useState } from "react";
import {
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Input,
} from "../../components/ui";
import { useCreateWallet } from "../../hooks/useWallets";

interface CreateWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
}

// Classe padrão do nosso Design System para inputs metalizados/acetinados
const INPUT_CLASS =
  "w-full h-11 px-3.5 rounded-xl bg-elevated/80 border border-subtle/30 text-primary text-sm font-medium focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand/30 transition-all placeholder:text-muted/60";

export function CreateWalletModal({ isOpen, onClose }: CreateWalletModalProps) {
  const { mutate: createWallet, isPending } = useCreateWallet();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [deadline, setDeadline] = useState("");

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    if (!title || !targetAmount) return;

    const numericTarget = Number(targetAmount.replace(",", "."));
    if (isNaN(numericTarget) || numericTarget <= 0) return;

    createWallet(
      {
        title,
        description: description || undefined,
        target_amount: numericTarget,
        current_amount: 0,
        deadline: deadline || null,
        color: "brand",
      },
      {
        onSuccess: () => {
          handleClose();
        },
      },
    );
  }

  function handleClose(): void {
    setTitle("");
    setDescription("");
    setTargetAmount("");
    setDeadline("");
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <ModalHeader title="Criar Novo Objetivo" onClose={handleClose} />

      <form onSubmit={handleSubmit}>
        <ModalBody className="space-y-4">
          <p className="text-xs font-medium text-muted -mt-2 mb-2">
            Defina um valor e organize suas reservas para acompanhar sua
            evolução patrimonial.
          </p>

          {/* Título da Meta */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Nome do Objetivo
            </label>
            <Input
              type="text"
              placeholder="ex: Reserva de Emergência, Viagem, Novo Setup..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={INPUT_CLASS}
              required
            />
          </div>

          {/* Valor da Meta */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Valor da Meta (R$)
            </label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted font-bold text-sm select-none">
                R$
              </span>
              <Input
                type="number"
                step="0.01"
                placeholder="10000.00"
                value={targetAmount}
                onChange={(e) => setTargetAmount(e.target.value)}
                className={`${INPUT_CLASS} pl-10`}
                required
              />
            </div>
          </div>

          {/* Data Limite */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Previsão de Conclusão (Opcional)
            </label>
            <div className="relative">
              <Input
                type="date"
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
                className={INPUT_CLASS}
              />
            </div>
          </div>

          {/* Descrição */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Motivação / Descrição (Opcional)
            </label>
            <Input
              type="text"
              placeholder="ex: 6 meses de custo fixo para segurança financeira"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={INPUT_CLASS}
            />
          </div>
        </ModalBody>

        <ModalFooter>
          <div className="pt-3 flex gap-3 border-t border-subtle/20 mt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={handleClose}
              disabled={isPending}
              className="flex-1 px-6 py-2.5 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all duration-200 cursor-pointer shadow-2xs"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isPending}
              className="flex-2 px-5 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              Salvar Meta
            </Button>
          </div>
        </ModalFooter>
      </form>
    </Modal>
  );
}
