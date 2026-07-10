import { useState } from "react";
import {
  X,
  ArrowUpCircle,
  ArrowDownCircle,
  Calendar as CalendarIcon,
  Building,
  Tag,
  ChevronDown,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQueryClient } from "@tanstack/react-query";
import { useAccounts } from "../../hooks/useAccounts";
import { useCategories } from "../../hooks/useCategories";
import { api } from "../../api/client";
// Importando a nossa nova infraestrutura de UX
import { PickerModal } from "../ui/PickerModal";
import { DatePickerModal } from "../ui/DatePickerModal";

// ✨ TIPAGEM ESTRITA PARA O LINTER
interface AccountData {
  id: string;
  name: string;
}

interface CategoryData {
  id: string;
  name: string;
  type: string;
}

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

  // ✨ ESTADOS DE CONTROLE DOS MODAIS DE UX
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    control, // <-- Adicione o control e remova o watch
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<TransactionForm>({
    resolver: zodResolver(transactionSchema),
    defaultValues: {
      type: "expense",
      status: "completed",
      date: today,
    },
  });

  // ✨ OBSERVADORES DE ESTADO (React Compiler Safe)
  const selectedType = useWatch({ control, name: "type" });
  const selectedStatus = useWatch({ control, name: "status" });
  const selectedDate = useWatch({ control, name: "date" });
  const selectedAccountId = useWatch({ control, name: "account_id" });
  const selectedCategoryId = useWatch({ control, name: "category_id" });
  // Filtros 100% tipados (Zero 'any')
  const filteredCategories = categories.filter(
    (cat: CategoryData) =>
      cat.type === (selectedType === "income" ? "entrada" : "saida") ||
      cat.type === selectedType,
  );

  // Helpers para exibir os nomes corretos nos botões (Zero 'any')
  const selectedAccountName =
    accounts.find((a: AccountData) => a.id === selectedAccountId)?.name ||
    "Selecione onde...";

  const selectedCategoryName =
    filteredCategories.find((c: CategoryData) => c.id === selectedCategoryId)
      ?.name || "Sem categoria";

  if (!isOpen) return null;

  async function onSubmit(data: TransactionForm) {
    try {
      const finalAmount =
        data.type === "expense"
          ? -Math.abs(data.amount)
          : Math.abs(data.amount);

      const payload = {
        title: data.title,
        amount: finalAmount,
        type: data.type === "income" ? "entrada" : "saida",
        account_id: data.account_id,
        category_id: data.category_id || undefined,
        status: data.status,
        expected_date: data.status === "pending" ? data.date : undefined,
        completed_date: data.status === "completed" ? data.date : undefined,
      };

      await api.post("/transactions", payload);

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
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          {/* Cabeçalho */}
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
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

          {/* Formulário com Scroll Interno */}
          <form
            id="new-transaction-form"
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-4 overflow-y-auto flex-1"
          >
            {/* Seletor Tipo (Receita/Despesa) Neumórfico */}
            <div className="flex gap-2 p-1.5 bg-elevated/60 rounded-2xl border border-subtle/20 shrink-0">
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
                  <CalendarIcon size={13} className="text-muted" />
                  <span>
                    {selectedStatus === "completed"
                      ? "Data do Fato"
                      : "Vencimento"}
                  </span>
                </label>
                {/* ✨ TRIGGER BUTTON: Data */}
                <button
                  type="button"
                  onClick={() => setIsDateModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span>
                    {selectedDate && !isNaN(new Date(selectedDate).getTime())
                      ? new Intl.DateTimeFormat("pt-BR").format(
                          new Date(selectedDate),
                        )
                      : "Selecione..."}
                  </span>
                  <CalendarIcon size={16} className="text-muted" />
                </button>
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
                {/* ✨ TRIGGER BUTTON: Situação */}
                <button
                  type="button"
                  onClick={() => setIsStatusModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span className="truncate pr-2">
                    {selectedStatus === "completed"
                      ? "✔ Concluído (Pago)"
                      : "⏳ Pendente (A Pagar)"}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>

            {/* Conta e Categoria */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <Building size={13} className="text-muted" />
                  <span>Conta *</span>
                </label>
                {/* ✨ TRIGGER BUTTON: Conta */}
                <button
                  type="button"
                  onClick={() => setIsAccountModalOpen(true)}
                  className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${!selectedAccountId ? "text-muted" : "text-primary"}`}
                >
                  <span className="truncate pr-2">{selectedAccountName}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
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
                {/* ✨ TRIGGER BUTTON: Categoria */}
                <button
                  type="button"
                  onClick={() => setIsCategoryModalOpen(true)}
                  className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${!selectedCategoryId ? "text-muted" : "text-primary"}`}
                >
                  <span className="truncate pr-2">{selectedCategoryName}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>
          </form>

          {/* Botões de Ação (Rodapé Fixo) */}
          <div className="p-4 sm:p-6 bg-surface border-t border-subtle/20 flex gap-3 shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all duration-200 cursor-pointer shadow-2xs"
            >
              Cancelar
            </button>
            <button
              type="submit"
              form="new-transaction-form"
              disabled={isSubmitting}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? "Salvando..." : "Lançar"}
            </button>
          </div>
        </div>
      </div>

      {/* ========================================= */}
      {/* 🚀 MODAIS DE INFRAESTRUTURA UX (OVERLAYS) */}
      {/* ========================================= */}

      <DatePickerModal
        isOpen={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        title={selectedStatus === "completed" ? "Data do Fato" : "Vencimento"}
        selectedDate={selectedDate}
        onSelectDate={(date) =>
          setValue("date", date, { shouldValidate: true, shouldDirty: true })
        }
      />

      <PickerModal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Situação do Lançamento"
        selectedValue={selectedStatus}
        onSelect={(val) =>
          setValue("status", val as "pending" | "completed", {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={[
          {
            label: "Concluído (Pago)",
            value: "completed",
            subtitle: "Já afetou o saldo da conta",
          },
          {
            label: "Pendente (A Pagar)",
            value: "pending",
            subtitle: "Irá afetar o saldo futuramente",
          },
        ]}
      />

      <PickerModal
        isOpen={isAccountModalOpen}
        onClose={() => setIsAccountModalOpen(false)}
        title="Selecione a Conta"
        selectedValue={selectedAccountId}
        onSelect={(val) =>
          setValue("account_id", val as string, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={accounts.map((acc: AccountData) => ({
          label: acc.name,
          value: acc.id,
        }))}
      />

      <PickerModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        title="Selecione a Categoria"
        selectedValue={selectedCategoryId || ""}
        onSelect={(val) =>
          setValue("category_id", val as string, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={[
          { label: "Sem categoria", value: "" },
          ...filteredCategories.map((cat: CategoryData) => ({
            label: cat.name,
            value: cat.id,
          })),
        ]}
      />
    </>
  );
}
