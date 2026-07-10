import { useState } from "react";
import {
  X,
  Target,
  DollarSign,
  Calendar as CalendarIcon,
  AlignLeft,
} from "lucide-react";
import { useCreateWallet } from "../../hooks/useWallets";
import { DatePickerModal } from "../../components/ui/DatePickerModal";

interface CreateWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateWalletModal({ isOpen, onClose }: CreateWalletModalProps) {
  const { mutate: createWallet, isPending } = useCreateWallet();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [deadline, setDeadline] = useState("");

  const [isDateModalOpen, setIsDateModalOpen] = useState(false);

  if (!isOpen) return null;

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
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
            <div>
              <h2 className="text-base sm:text-xl font-bold text-primary tracking-tight">
                Novo Objetivo
              </h2>
              <p className="text-xs font-medium text-muted mt-0.5 hidden sm:block">
                Defina um valor e organize suas reservas financeiras
              </p>
            </div>
            <button
              onClick={handleClose}
              className="p-2 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>

          <form
            id="create-wallet-form"
            onSubmit={handleSubmit}
            className="p-4 sm:p-6 space-y-3.5 sm:space-y-4 overflow-y-auto flex-1"
          >
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <Target size={13} className="text-muted" />
                <span>Nome da Meta *</span>
              </label>
              <input
                type="text"
                placeholder="Ex: Viagem, Novo Setup, Reserva..."
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
                required
              />
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <DollarSign size={13} className="text-muted" />
                <span>Valor do Objetivo (R$) *</span>
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0.00"
                value={targetAmount}
                onChange={(e) => setTargetAmount(e.target.value)}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm sm:text-base font-extrabold shadow-2xs tracking-tight"
                required
              />
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <CalendarIcon size={13} className="text-muted" />
                <span>Previsão de Conclusão (Opcional)</span>
              </label>
              {/* ✨ TRIGGER BUTTON: Data com proteção White Screen */}
              <button
                type="button"
                onClick={() => setIsDateModalOpen(true)}
                className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <span>
                  {deadline && !isNaN(new Date(deadline).getTime())
                    ? new Intl.DateTimeFormat("pt-BR").format(
                        new Date(deadline),
                      )
                    : "Sem data limite..."}
                </span>
                <CalendarIcon size={16} className="text-muted shrink-0" />
              </button>
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <AlignLeft size={13} className="text-muted" />
                <span>Descrição (Opcional)</span>
              </label>
              <input
                type="text"
                placeholder="Ex: 6 meses de custo fixo garantidos"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-medium shadow-2xs"
              />
            </div>
          </form>

          <div className="p-4 sm:p-6 bg-surface border-t border-subtle/20 flex gap-3 shrink-0">
            <button
              type="button"
              onClick={handleClose}
              disabled={isPending}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all shadow-2xs cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              form="create-wallet-form"
              disabled={isPending || !title || !targetAmount}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer"
            >
              {isPending ? "Criando..." : "Salvar Meta"}
            </button>
          </div>
        </div>
      </div>

      <DatePickerModal
        isOpen={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        title="Previsão de Conclusão"
        selectedDate={deadline}
        onSelectDate={(date) => setDeadline(date)}
      />
    </>
  );
}
