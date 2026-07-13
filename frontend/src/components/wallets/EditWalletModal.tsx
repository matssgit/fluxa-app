import { useEffect, useState } from "react";
import {
  Target,
  DollarSign,
  Calendar as CalendarIcon,
  AlignLeft,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useUpdateWallet } from "../../hooks/useWallets";
import type { Wallet } from "../../types/wallet";

import { Modal, ModalHeader, ModalBody, ModalFooter } from "../ui/Modal";
import { DatePickerModal } from "../ui/DatePickerModal";

const editWalletSchema = z.object({
  title: z.string().min(1, "O nome da meta é obrigatório"),
  target_amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  deadline: z.string().optional().nullable(),
  description: z.string().optional(),
  status: z.enum(["active", "paused", "completed"]).optional(),
});

type EditWalletForm = z.infer<typeof editWalletSchema>;

interface EditWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
  wallet: Wallet | null;
}

export function EditWalletModal({
  isOpen,
  onClose,
  wallet,
}: EditWalletModalProps) {
  const { mutateAsync: updateWallet, isPending } = useUpdateWallet();
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);

  const { register, handleSubmit, setValue, control, reset } =
    useForm<EditWalletForm>({
      resolver: zodResolver(editWalletSchema),
    });

  // ✨ CORREÇÃO 1: Hooks sempre no topo, antes de qualquer "return" condicional
  const currentDeadline = useWatch({ control, name: "deadline" });
  const currentStatus = useWatch({ control, name: "status" });

  useEffect(() => {
    if (isOpen && wallet) {
      reset({
        title: wallet.title,
        description: wallet.description || "",
        target_amount: Number(wallet.target_amount),
        deadline: wallet.deadline
          ? new Date(wallet.deadline).toISOString()
          : null,
        status: wallet.status,
      });
    }
  }, [isOpen, wallet, reset]);

  // ✨ O "early return" vem APÓS os hooks
  if (!isOpen || !wallet) return null;

  async function onSubmit(data: EditWalletForm): Promise<void> {
    if (!wallet) return;

    try {
      await updateWallet({
        id: wallet.id,
        ...data,
      });
      onClose();
    } catch (error) {
      console.error("Erro ao atualizar meta:", error);
      alert("Não foi possível salvar as alterações.");
    }
  }

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} size="sm">
        <ModalHeader title="Editar Objetivo" onClose={onClose} />

        <form id="edit-wallet-form" onSubmit={handleSubmit(onSubmit)}>
          <ModalBody className="p-6 space-y-5">
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
                  {...register("title")}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary outline-none transition-all text-sm font-medium shadow-2xs"
                />
              </div>
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
                  {...register("target_amount", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary outline-none transition-all text-base font-extrabold shadow-2xs tracking-tight"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Previsão de Conclusão
              </label>
              <button
                type="button"
                onClick={() => setIsDateModalOpen(true)}
                className="w-full flex items-center gap-2.5 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer text-primary"
              >
                <CalendarIcon size={16} className="text-muted shrink-0" />
                <span>
                  {currentDeadline &&
                  !isNaN(new Date(currentDeadline).getTime())
                    ? new Intl.DateTimeFormat("pt-BR").format(
                        new Date(currentDeadline),
                      )
                    : "Sem data limite"}
                </span>
              </button>
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Descrição
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <AlignLeft size={16} className="text-muted" />
                </div>
                <input
                  type="text"
                  {...register("description")}
                  className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary outline-none transition-all text-sm font-medium shadow-2xs"
                />
              </div>
            </div>

            {/* Opção de pausar a meta */}
            <div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="rounded border-subtle text-brand focus:ring-brand"
                  onChange={(e) =>
                    setValue("status", e.target.checked ? "paused" : "active")
                  }
                  checked={currentStatus === "paused"}
                />
                <span className="text-sm font-medium text-secondary">
                  Pausar meta temporariamente
                </span>
              </label>
            </div>
          </ModalBody>

          <ModalFooter>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-sm font-bold transition-all cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={isPending}
                form="edit-wallet-form"
                className="flex-1 px-4 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer"
              >
                {isPending ? "Salvando..." : "Salvar Alterações"}
              </button>
            </div>
          </ModalFooter>
        </form>
      </Modal>

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
