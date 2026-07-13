import { useState, useEffect } from "react";
import {
  Target,
  DollarSign,
  Calendar as CalendarIcon,
  AlignLeft,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreateWallet } from "../../hooks/useWallets";
import { useQueryClient } from "@tanstack/react-query";

import { Modal, ModalHeader, ModalBody, ModalFooter } from "../ui/Modal";
import { DatePickerModal } from "../ui/DatePickerModal";

const walletSchema = z.object({
  title: z.string().min(1, "O nome da meta é obrigatório"),
  target_amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  deadline: z.string().optional().nullable(),
  description: z.string().optional(),
});

type WalletForm = z.infer<typeof walletSchema>;

interface CreateWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateWalletModal({ isOpen, onClose }: CreateWalletModalProps) {
  const { mutateAsync: createWallet, isPending } = useCreateWallet();
  const queryClient = useQueryClient();

  const [isDateModalOpen, setIsDateModalOpen] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    control,
    reset,
    formState: { errors },
  } = useForm<WalletForm>({
    resolver: zodResolver(walletSchema),
    defaultValues: {
      title: "",
      description: "",
      deadline: null,
    },
  });

  // Limpa o formulário sempre que o modal fechar/abrir
  useEffect(() => {
    if (!isOpen) reset();
  }, [isOpen, reset]);

  // React Compiler Safe: Observa a data sem causar stale UI
  const currentDeadline = useWatch({
    control,
    name: "deadline",
  });

  if (!isOpen) return null;

  async function onSubmit(data: WalletForm): Promise<void> {
    try {
      await createWallet({
        title: data.title,
        description: data.description || undefined,
        target_amount: data.target_amount,
        current_amount: 0,
        deadline: data.deadline || null,
        color: "brand", // Mantendo a cor fixa conforme o payload original
      });

      // Invalidação em cascata para atualizar o cockpit
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["wallets"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);

      onClose();
    } catch (error) {
      console.error("Erro ao criar carteira:", error);
      alert("Não foi possível salvar a meta. Tente novamente.");
    }
  }

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} size="sm">
        <ModalHeader title="Novo Objetivo" onClose={onClose} />

        <form id="create-wallet-form" onSubmit={handleSubmit(onSubmit)}>
          <ModalBody className="p-6 space-y-5">
            <p className="text-xs font-medium text-muted -mt-2">
              Defina um valor e organize suas reservas financeiras
            </p>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Nome da Meta *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Target size={16} className="text-muted" />
                </div>
                <input
                  type="text"
                  placeholder="Ex: Viagem, Novo Setup, Reserva..."
                  {...register("title")}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
                />
              </div>
              {errors.title && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.title.message}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Valor do Objetivo (R$) *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <DollarSign size={16} className="text-muted" />
                </div>
                <input
                  type="number"
                  step="0.01"
                  placeholder="0,00"
                  {...register("target_amount", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base font-extrabold shadow-2xs tracking-tight"
                />
              </div>
              {errors.target_amount && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.target_amount.message}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Previsão de Conclusão (Opcional)
              </label>
              {/* ✨ TRIGGER BUTTON: Chama o nosso DatePickerModal com proteção White Screen */}
              <button
                type="button"
                onClick={() => setIsDateModalOpen(true)}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer ${
                  currentDeadline ? "text-primary" : "text-muted"
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <CalendarIcon size={16} className="text-muted shrink-0" />
                  <span>
                    {currentDeadline &&
                    !isNaN(new Date(currentDeadline).getTime())
                      ? new Intl.DateTimeFormat("pt-BR").format(
                          new Date(currentDeadline),
                        )
                      : "Sem data limite..."}
                  </span>
                </div>
              </button>
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Descrição (Opcional)
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <AlignLeft size={16} className="text-muted" />
                </div>
                <input
                  type="text"
                  placeholder="Ex: 6 meses de custo fixo garantidos"
                  {...register("description")}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
                />
              </div>
            </div>
          </ModalBody>

          <ModalFooter>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-sm font-bold transition-all shadow-2xs cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={isPending}
                form="create-wallet-form"
                className="flex-1 px-4 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer flex items-center justify-center"
              >
                {isPending ? (
                  <span className="animate-pulse">A Guardar...</span>
                ) : (
                  "Guardar Meta"
                )}
              </button>
            </div>
          </ModalFooter>
        </form>
      </Modal>

      {/* --- INFRAESTRUTURA DE DATA --- */}
      <DatePickerModal
        isOpen={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        title="Previsão de Conclusão"
        selectedDate={currentDeadline || ""}
        onSelectDate={(date) => {
          setValue("deadline", date, { shouldValidate: true });
          setIsDateModalOpen(false);
        }}
      />
    </>
  );
}
