import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAccounts } from "../hooks/useAccounts";
import { useQueryClient } from "@tanstack/react-query";

const accountSchema = z.object({
  name: z.string().min(2, "O nome da conta é obrigatório"),
  type: z.enum(["checking", "wallet", "savings"]),
});

type AccountForm = z.infer<typeof accountSchema>;

interface AccountModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AccountModal({ isOpen, onClose }: AccountModalProps) {
  const { createAccount, isCreating } = useAccounts();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AccountForm>({
    resolver: zodResolver(accountSchema),
    defaultValues: {
      type: "checking",
    },
  });

  if (!isOpen) return null;

  async function onSubmit(data: AccountForm): Promise<void> {
    try {
      await createAccount(data);

      // Invalida os caches em cascata para atualizar o ecossistema financeiro
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["accounts"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);

      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar conta:", error);
      alert("Não foi possível salvar a conta. Tente novamente.");
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-subtle/30 transition-all duration-300">
        {/* Cabeçalho */}
        <div className="flex justify-between items-center p-6 border-b border-subtle/20">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Nova Conta ou Carteira
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
              Adicione uma fonte de liquidez ao seu ecossistema
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Formulário */}
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Nome da Conta *
            </label>
            <input
              type="text"
              placeholder="Ex: Nubank, Itaú, Carteira Física..."
              {...register("name")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
            />
            {errors.name && (
              <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                {errors.name.message}
              </span>
            )}
          </div>

          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Tipo de Conta *
            </label>
            <div className="relative">
              <select
                {...register("type")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs cursor-pointer appearance-none"
              >
                <option value="checking" className="bg-surface text-primary">
                  🏛️ Conta Corrente (Bancos)
                </option>
                <option value="wallet" className="bg-surface text-primary">
                  💵 Carteira (Dinheiro Físico)
                </option>
                <option value="savings" className="bg-surface text-primary">
                  📈 Poupança / Investimentos
                </option>
              </select>
            </div>
          </div>

          {/* Botões de Ação */}
          <div className="pt-3 flex gap-3 border-t border-subtle/20 mt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all duration-200 cursor-pointer shadow-2xs"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isCreating}
              className="flex-1 px-4 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isCreating ? "Salvando..." : "Salvar Conta"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
