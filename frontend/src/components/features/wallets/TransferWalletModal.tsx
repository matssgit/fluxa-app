import { z } from "zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { Wallet } from "../../../types/wallet";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import { useUpdateWalletProgress } from "../../../hooks/useWallets";
import { TrendingUp, TrendingDown, AlignLeft } from "lucide-react";

import { Modal, ModalHeader, ModalBody, ModalFooter } from "../../ui/Modal";

const progressSchema = z.object({
  amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  observation: z.string().optional(),
});

type ProgressForm = z.infer<typeof progressSchema>;

interface UpdateProgressModalProps {
  isOpen: boolean;
  onClose: () => void;
  wallet: Wallet | null;
  type: "deposit" | "withdraw" | null;
}

// Pode renomear a função exportada para UpdateProgressModal se atualizar no index.tsx
export function TransferWalletModal({
  isOpen,
  onClose,
  wallet,
  type,
}: UpdateProgressModalProps) {
  const { mutateAsync: updateProgress, isPending } = useUpdateWalletProgress();
  const queryClient = useQueryClient();

  const isDeposit = type === "deposit";
  // O máximo que o utilizador pode reduzir é o que já tem guardado
  const maxWithdraw = wallet ? Number(wallet.current_amount) || 0 : 0;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProgressForm>({
    resolver: zodResolver(progressSchema),
    defaultValues: {
      observation: "",
    },
  });

  useEffect(() => {
    if (!isOpen) reset();
  }, [isOpen, reset]);

  if (!isOpen || !wallet || !type) return null;

  const title = isDeposit ? "Adicionar Progresso" : "Reduzir Progresso";
  const subtitle = isDeposit
    ? `Aumentando o progresso de: ${wallet.title}`
    : `Reduzindo o progresso de: ${wallet.title}`;

  async function onSubmit(data: ProgressForm): Promise<void> {
    if (!wallet || !type) return;

    try {
      if (!isDeposit && data.amount > maxWithdraw) {
        alert("O valor de redução não pode ser maior que o progresso atual.");
        return;
      }

      await updateProgress({
        wallet_id: wallet.id,
        amount: data.amount,
        observation: data.observation,
        type: type as "deposit" | "withdraw",
      });

      await queryClient.invalidateQueries({ queryKey: ["wallets"] });
      onClose();
    } catch (error) {
      console.error("Erro ao atualizar progresso:", error);
      alert("Não foi possível atualizar o progresso da meta. Tente novamente.");
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="sm">
      <ModalHeader title={title} onClose={onClose} />

      <form id="progress-wallet-form" onSubmit={handleSubmit(onSubmit)}>
        <ModalBody className="p-6 space-y-5">
          <p className="text-xs font-medium text-muted -mt-2">{subtitle}</p>

          <div>
            <div className="flex justify-between items-baseline mb-1.5">
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                Valor (R$) *
              </label>
              {!isDeposit && (
                <span className="text-[10px] font-extrabold text-amber-500 uppercase tracking-wider bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20">
                  Progresso Atual: R$ {maxWithdraw.toFixed(2)}
                </span>
              )}
            </div>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                {isDeposit ? (
                  <TrendingUp size={16} className="text-brand" />
                ) : (
                  <TrendingDown size={16} className="text-amber-500" />
                )}
              </div>
              <input
                type="number"
                step="0.01"
                max={!isDeposit ? maxWithdraw : undefined}
                placeholder="0,00"
                {...register("amount", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base font-extrabold shadow-2xs tracking-tight"
              />
            </div>
            {errors.amount && (
              <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                {errors.amount.message}
              </span>
            )}
          </div>

          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Observação (Opcional)
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <AlignLeft size={16} className="text-muted" />
              </div>
              <input
                type="text"
                placeholder={
                  isDeposit
                    ? "Ex: Guardei parte do salário"
                    : "Ex: Usei para emergência"
                }
                {...register("observation")}
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
              form="progress-wallet-form"
              className={`flex-1 px-4 py-3 rounded-xl text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer flex items-center justify-center ${
                isDeposit
                  ? "bg-brand hover:bg-brand-light"
                  : "bg-amber-500 hover:bg-amber-400"
              }`}
            >
              {isPending ? (
                <span className="animate-pulse">Guardando...</span>
              ) : (
                <div className="flex items-center gap-2">
                  {isDeposit ? (
                    <TrendingUp size={16} />
                  ) : (
                    <TrendingDown size={16} />
                  )}
                  <span>
                    {isDeposit ? "Confirmar Aporte" : "Confirmar Redução"}
                  </span>
                </div>
              )}
            </button>
          </div>
        </ModalFooter>
      </form>
    </Modal>
  );
}
