import {
  X,
  ArrowUpCircle,
  ArrowDownCircle,
  Calendar,
  Building,
  Tag,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQueryClient } from "@tanstack/react-query";
import { useAccounts } from "../hooks/useAccounts";
import { useCategories } from "../hooks/useCategories";
import { api } from "../api/client";

const transactionSchema = z.object({
  type: z.enum(["income", "expense"]),
  title: z.string().min(2, "O título é obrigatório"),
  amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  account_id: z.string().min(1, "Selecione uma conta"),
  category_id: z.string().optional(),
  status: z.enum(["pending", "completed"]),
  date: z.string().min(1, "A data é obrigatória"),
});

type TransactionForm = z.infer<typeof transactionSchema>;

interface NewTransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NewTransactionModal({
  isOpen,
  onClose,
}: NewTransactionModalProps) {
  const queryClient = useQueryClient();
  const { accounts = [] } = useAccounts();
  const { categories = [] } = useCategories();

  const today = new Date().toISOString().split("T")[0];

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<TransactionForm>({
    resolver: zodResolver(transactionSchema),
    defaultValues: {
      type: "expense",
      status: "completed",
      date: today,
    },
  });

  const selectedType = useWatch({
    control,
    name: "type",
    defaultValue: "expense",
  });

  const selectedStatus = useWatch({
    control,
    name: "status",
    defaultValue: "completed",
  });

  const filteredCategories = categories.filter(
    (cat: { type: string; id: string; name: string }) =>
      cat.type === (selectedType === "income" ? "entrada" : "saida") ||
      cat.type === selectedType,
  );

  if (!isOpen) return null;

  async function onSubmit(data: TransactionForm) {
    try {
      // Ajuste matemático de sinal para o Back-end
      const finalAmount =
        data.type === "expense"
          ? -Math.abs(data.amount)
          : Math.abs(data.amount);

      const payload = {
        title: data.title,
        amount: finalAmount,
        account_id: data.account_id,
        category_id: data.category_id || undefined,
        status: data.status,
        expected_date: data.status === "pending" ? data.date : undefined,
        completed_date: data.status === "completed" ? data.date : undefined,
      };

      await api.post("/transactions", payload);

      // Invalida em cascata os caches para atualizar o ecossistema financeiro
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });

      reset({ type: "expense", status: "completed", date: today });
      onClose();
    } catch (error) {
      console.error("Erro ao criar transação", error);
      alert("Não foi possível salvar o lançamento. Tente novamente.");
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-subtle/30 transition-all duration-300">
        {/* Cabeçalho */}
        <div className="flex justify-between items-center p-6 border-b border-subtle/20">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Novo Lançamento
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
              Registre uma movimentação no seu fluxo diário
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
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          {/* Seletor Tipo (Receita/Despesa) Neumórfico */}
          <div className="flex gap-2 p-1.5 bg-elevated/60 rounded-2xl border border-subtle/20">
            <label className="flex-1 cursor-pointer">
              <input
                type="radio"
                value="income"
                {...register("type")}
                className="peer sr-only"
              />
              <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-emerald-500 peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all duration-200">
                <ArrowUpCircle size={16} className="shrink-0" />
                <span>Receita</span>
              </div>
            </label>

            <label className="flex-1 cursor-pointer">
              <input
                type="radio"
                value="expense"
                {...register("type")}
                className="peer sr-only"
              />
              <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-red-500 peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all duration-200">
                <ArrowDownCircle size={16} className="shrink-0" />
                <span>Despesa</span>
              </div>
            </label>
          </div>

          {/* Descrição e Valor */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Descrição *
              </label>
              <input
                type="text"
                placeholder="Ex: Salário, Almoço, Internet..."
                {...register("title")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
              />
              {errors.title && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.title.message}
                </span>
              )}
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Valor (R$) *
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0,00"
                {...register("amount", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base sm:text-lg font-extrabold shadow-2xs tracking-tight"
              />
              {errors.amount && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.amount.message}
                </span>
              )}
            </div>
          </div>

          {/* Data e Situação */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Calendar size={13} className="text-muted" />
                <span>
                  {selectedStatus === "completed"
                    ? "Data do Fato"
                    : "Vencimento"}
                </span>
              </label>
              <input
                type="date"
                {...register("date")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
              />
              {errors.date && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.date.message}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Situação
              </label>
              <select
                {...register("status")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="completed" className="bg-surface text-primary">
                  ✔ Concluído (Pago)
                </option>
                <option value="pending" className="bg-surface text-primary">
                  ⏳ Pendente (A Pagar)
                </option>
              </select>
            </div>
          </div>

          {/* Conta e Categoria */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Building size={13} className="text-muted" />
                <span>Conta *</span>
              </label>
              <select
                {...register("account_id")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="" className="bg-surface text-muted">
                  Selecione onde...
                </option>
                {accounts.map((acc: { id: string; name: string }) => (
                  <option
                    key={acc.id}
                    value={acc.id}
                    className="bg-surface text-primary"
                  >
                    {acc.name}
                  </option>
                ))}
              </select>
              {errors.account_id && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.account_id.message}
                </span>
              )}
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Tag size={13} className="text-muted" />
                <span>Categoria</span>
              </label>
              <select
                {...register("category_id")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="" className="bg-surface text-muted">
                  Sem categoria
                </option>
                {filteredCategories.map((cat: { id: string; name: string }) => (
                  <option
                    key={cat.id}
                    value={cat.id}
                    className="bg-surface text-primary"
                  >
                    {cat.name}
                  </option>
                ))}
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
              disabled={isSubmitting}
              className="flex-1 px-4 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? "Salvando..." : "Lançar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
