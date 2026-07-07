import {
  X,
  Calendar,
  DollarSign,
  Tag,
  Building,
  CreditCard,
} from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCreateSubscription } from "../../hooks/useSubscriptions";
import { useAccounts } from "../../hooks/useAccounts";
import { useCategories } from "../../hooks/useCategories";
import { useCards } from "../../hooks/useCredit";
import { useQueryClient } from "@tanstack/react-query";

// Interfaces seguras para blindar a tipagem sem 'any'
interface Category {
  id: string;
  name: string;
  type: string;
}

interface Account {
  id: string;
  name: string;
}

interface Card {
  id: string;
  name: string;
}

const subscriptionSchema = z.object({
  title: z.string().min(1, "O nome do serviço é obrigatório"),
  amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  due_day: z.number().min(1, "Dia inválido").max(31, "Dia inválido"),
  frequency: z.enum(["monthly", "yearly"]),
  category_id: z.string().min(1, "Selecione uma categoria"),
  payment_method: z.enum(["account", "card"]),
  account_id: z.string().optional(),
  card_id: z.string().optional(),
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
  const { mutateAsync: createSub, isPending } = useCreateSubscription();
  const { accounts = [] } = useAccounts();
  const { categories = [] } = useCategories();
  const { data: cards = [] } = useCards();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<SubscriptionForm>({
    resolver: zodResolver(subscriptionSchema),
    defaultValues: { frequency: "monthly", payment_method: "account" },
  });

  const paymentMethod = watch("payment_method");

  // Suporta tanto o padrão inglês ("expense") como o português ("saida") vindos da API
  const filteredCategories = (categories as Category[]).filter(
    (c) => c.type === "expense" || c.type === "saida",
  );

  if (!isOpen) return null;

  async function onSubmit(data: SubscriptionForm): Promise<void> {
    try {
      await createSub({
        title: data.title,
        amount: data.amount,
        due_day: data.due_day,
        frequency: data.frequency,
        category_id: data.category_id,
        account_id:
          data.payment_method === "account" ? data.account_id : undefined,
        card_id: data.payment_method === "card" ? data.card_id : undefined,
      });

      // Invalida em cascata para atualizar a lista de assinaturas e o cockpit do Dashboard
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["subscriptions"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);

      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar assinatura", error);
      alert("Não foi possível guardar a assinatura. Tente novamente.");
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden border border-subtle/30 transition-all duration-300">
        {/* Cabeçalho */}
        <div className="p-6 border-b border-subtle/20 flex justify-between items-center">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Nova Assinatura Recorrente
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
              Monitorize os seus serviços fixos e cobranças automáticas
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
              Nome do Serviço *
            </label>
            <input
              type="text"
              placeholder="Ex: Netflix, Spotify, Internet Fibre..."
              {...register("title")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
            />
            {errors.title && (
              <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                {errors.title.message}
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <DollarSign size={13} className="text-muted" />
                <span>Valor (R$) *</span>
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0,00"
                {...register("amount", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base font-extrabold shadow-2xs tracking-tight"
              />
              {errors.amount && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.amount.message}
                </span>
              )}
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Calendar size={13} className="text-muted" />
                <span>Dia de Vencimento *</span>
              </label>
              <input
                type="number"
                placeholder="Ex: 10"
                {...register("due_day", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs"
              />
              {errors.due_day && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.due_day.message}
                </span>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Tag size={13} className="text-muted" />
                <span>Categoria *</span>
              </label>
              <select
                {...register("category_id")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="" className="bg-surface text-muted">
                  Selecione...
                </option>
                {filteredCategories.map((c: Category) => (
                  <option
                    key={c.id}
                    value={c.id}
                    className="bg-surface text-primary"
                  >
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.category_id && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.category_id.message}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Periodicidade
              </label>
              <select
                {...register("frequency")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="monthly" className="bg-surface text-primary">
                  Mensal
                </option>
                <option value="yearly" className="bg-surface text-primary">
                  Anual
                </option>
              </select>
            </div>
          </div>

          {/* Seletor de Método de Cobrança */}
          <div className="pt-2 border-t border-subtle/20 space-y-3">
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
              Fonte de Cobrança Padrão
            </label>
            <div className="grid grid-cols-2 gap-3 p-1 bg-elevated/60 rounded-2xl border border-subtle/20">
              <label className="flex-1 cursor-pointer">
                <input
                  type="radio"
                  value="account"
                  {...register("payment_method")}
                  className="peer sr-only"
                />
                <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-primary peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all">
                  <Building size={16} className="text-brand shrink-0" />
                  <span>Conta / Débito</span>
                </div>
              </label>
              <label className="flex-1 cursor-pointer">
                <input
                  type="radio"
                  value="card"
                  {...register("payment_method")}
                  className="peer sr-only"
                />
                <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-primary peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all">
                  <CreditCard size={16} className="text-brand shrink-0" />
                  <span>Cartão de Crédito</span>
                </div>
              </label>
            </div>

            <div className="pt-1">
              {paymentMethod === "account" ? (
                <select
                  {...register("account_id")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <option value="" className="bg-surface text-muted">
                    Selecione a conta de débito...
                  </option>
                  {(accounts as Account[]).map((acc) => (
                    <option
                      key={acc.id}
                      value={acc.id}
                      className="bg-surface text-primary"
                    >
                      {acc.name}
                    </option>
                  ))}
                </select>
              ) : (
                <select
                  {...register("card_id")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <option value="" className="bg-surface text-muted">
                    Selecione o cartão de crédito...
                  </option>
                  {(cards as Card[]).map((card) => (
                    <option
                      key={card.id}
                      value={card.id}
                      className="bg-surface text-primary"
                    >
                      {card.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>

          {/* Botões */}
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
              disabled={isPending}
              className="flex-1 px-4 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isPending ? "A guardar..." : "Guardar Assinatura"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
