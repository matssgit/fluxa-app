import { useState } from "react";
import {
  X,
  Repeat,
  DollarSign,
  Calendar as CalendarIcon,
  Building,
  Tag,
  ChevronDown,
  Layers,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQueryClient } from "@tanstack/react-query";
import { useAccounts } from "../../hooks/useAccounts";
import { useCategories } from "../../hooks/useCategories";
import { api } from "../../api/client";

// Infraestrutura UX Padrão
import { PickerModal } from "../../components/ui/PickerModal";
import { DatePickerModal } from "../../components/ui/DatePickerModal";

// ✨ TIPAGEM ESTRITA
interface AccountData {
  id: string;
  name: string;
}

interface CategoryData {
  id: string;
  name: string;
  type: string;
}

const subscriptionSchema = z.object({
  title: z.string().min(2, "A descrição é obrigatória"),
  amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  account_id: z.string().min(1, "Selecione uma conta"),
  category_id: z.string().optional(),
  frequency: z.enum(["monthly", "yearly", "weekly"]),
  next_billing_date: z.string().min(1, "A data do próximo ciclo é obrigatória"),
});

type SubscriptionForm = z.infer<typeof subscriptionSchema>;

interface CreateSubscriptionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateSubscriptionModal({
  isOpen,
  onClose,
}: CreateSubscriptionModalProps) {
  const queryClient = useQueryClient();
  const { accounts = [] } = useAccounts();
  const { categories = [] } = useCategories();

  const today = new Date().toISOString().split("T")[0];

  const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [isFrequencyModalOpen, setIsFrequencyModalOpen] = useState(false);
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors },
  } = useForm<SubscriptionForm>({
    resolver: zodResolver(subscriptionSchema),
    defaultValues: {
      frequency: "monthly",
      next_billing_date: today,
    },
  });

  const selectedAccountId = useWatch({ control, name: "account_id" });
  const selectedCategoryId = useWatch({ control, name: "category_id" });
  const selectedFrequency = useWatch({
    control,
    name: "frequency",
    defaultValue: "monthly",
  });
  const selectedDate = useWatch({
    control,
    name: "next_billing_date",
    defaultValue: today,
  });

  const expenseCategories = categories.filter(
    (cat: CategoryData) => cat.type === "saida" || cat.type === "expense",
  );

  const selectedAccountName =
    accounts.find((a: AccountData) => a.id === selectedAccountId)?.name ||
    "Selecione onde debita...";
  const selectedCategoryName =
    expenseCategories.find((c: CategoryData) => c.id === selectedCategoryId)
      ?.name || "Sem categoria";

  const frequencyLabels: Record<string, string> = {
    weekly: "Semanal",
    monthly: "Mensal",
    yearly: "Anual",
  };

  if (!isOpen) return null;

  async function onSubmit(data: SubscriptionForm) {
    try {
      setIsSubmitting(true);

      const payload = {
        title: data.title,
        amount: Math.abs(data.amount),
        account_id: data.account_id,
        category_id: data.category_id || undefined,
        frequency: data.frequency,
        next_billing_date: data.next_billing_date,
        status: "active",
      };

      await api.post("/subscriptions", payload);
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });

      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar assinatura:", error);
      alert("Não foi possível salvar a assinatura. Tente novamente.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      {/* 🚀 ESTRUTURA GLOBAL IDÊNTICA AO NEW TRANSACTION MODAL */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          {/* HEADER PADRÃO */}
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
                Nova Assinatura
              </h2>
              <p className="text-xs font-medium text-muted mt-0.5">
                Registre serviços com cobrança recorrente
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>

          {/* ⚡ A MÁGICA: Formulário com min-h-0 (protege o scroll flex) e space-y-4 */}
          <form
            id="create-subscription-form"
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-4 overflow-y-auto flex-1 min-h-0"
          >
            {/* LINHA 1: NOME DO SERVIÇO (Largura Total) */}
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Repeat size={13} className="text-muted" />
                <span>Nome do Serviço *</span>
              </label>
              <input
                type="text"
                placeholder="Ex: Netflix, Spotify..."
                {...register("title")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
              />
              {errors.title && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.title.message}
                </span>
              )}
            </div>

            {/* LINHA 2: VALOR E FREQUÊNCIA (Lado a Lado no Mobile!) */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <DollarSign size={13} className="text-muted" />
                  <span>Valor *</span>
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

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <Layers size={13} className="text-muted shrink-0" />
                  <span className="truncate">Frequência *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsFrequencyModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span className="truncate pr-2">
                    {frequencyLabels[selectedFrequency]}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>

            {/* LINHA 3: DATA E CONTA (Lado a Lado no Mobile!) */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <CalendarIcon size={13} className="text-muted shrink-0" />
                  <span className="truncate">Vencimento *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsDateModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span className="truncate">
                    {selectedDate
                      ? new Intl.DateTimeFormat("pt-BR").format(
                          new Date(selectedDate),
                        )
                      : "Selecione..."}
                  </span>
                  <CalendarIcon size={16} className="text-muted shrink-0" />
                </button>
                {errors.next_billing_date && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.next_billing_date.message}
                  </span>
                )}
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <Building size={13} className="text-muted shrink-0" />
                  <span className="truncate">Conta *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsAccountModalOpen(true)}
                  className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${!selectedAccountId ? "text-muted" : "text-primary"}`}
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
            </div>

            {/* LINHA 4: CATEGORIA (Largura Total) */}
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Tag size={13} className="text-muted" />
                <span>Categoria</span>
              </label>
              <button
                type="button"
                onClick={() => setIsCategoryModalOpen(true)}
                className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-sm font-semibold shadow-2xs cursor-pointer ${!selectedCategoryId ? "text-muted" : "text-primary"}`}
              >
                <span className="truncate pr-2">{selectedCategoryName}</span>
                <ChevronDown size={16} className="text-muted shrink-0" />
              </button>
            </div>
          </form>

          {/* FOOTER PADRÃO */}
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
              form="create-subscription-form"
              disabled={isSubmitting}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? "Salvando..." : "Criar Assinatura"}
            </button>
          </div>
        </div>
      </div>

      {/* MODAIS UX PINE & SAGE */}
      <PickerModal
        isOpen={isFrequencyModalOpen}
        onClose={() => setIsFrequencyModalOpen(false)}
        title="Frequência de Cobrança"
        selectedValue={selectedFrequency}
        onSelect={(val) =>
          setValue("frequency", val as "monthly" | "yearly" | "weekly", {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={[
          {
            label: "Mensal",
            value: "monthly",
            subtitle: "Cobra 1 vez a cada mês",
          },
          { label: "Anual", value: "yearly", subtitle: "Cobra 1 vez por ano" },
          {
            label: "Semanal",
            value: "weekly",
            subtitle: "Cobra a cada 7 dias",
          },
        ]}
      />

      <PickerModal
        isOpen={isAccountModalOpen}
        onClose={() => setIsAccountModalOpen(false)}
        title="Onde será debitado?"
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
        title="Categoria da Assinatura"
        selectedValue={selectedCategoryId || ""}
        onSelect={(val) =>
          setValue("category_id", val as string, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={[
          { label: "Sem categoria", value: "" },
          ...expenseCategories.map((cat: CategoryData) => ({
            label: cat.name,
            value: cat.id,
          })),
        ]}
      />

      <DatePickerModal
        isOpen={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        title="Próximo Vencimento"
        selectedDate={selectedDate}
        onSelectDate={(date) =>
          setValue("next_billing_date", date, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
      />
    </>
  );
}
