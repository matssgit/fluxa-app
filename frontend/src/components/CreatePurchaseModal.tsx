import {
  X,
  ShoppingBag,
  DollarSign,
  Calendar,
  CreditCard,
  Tag,
  Layers,
} from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreatePurchase, useCards } from "../hooks/useCredit";
import { useCategories } from "../hooks/useCategories";

const purchaseSchema = z.object({
  card_id: z.string().min(1, "Selecione o cartão de crédito"),
  category_id: z.string().optional(),
  title: z.string().min(2, "A descrição da compra é obrigatória"),
  store: z.string().min(2, "O estabelecimento / loja é obrigatório"),
  total_amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  total_installments: z
    .number()
    .min(1, "Mínimo de 1 parcela")
    .max(72, "Máximo de 72 parcelas"),
  purchase_date: z.string().min(1, "A data da compra é obrigatória"),
  observation: z.string().optional(),
});

type PurchaseForm = z.infer<typeof purchaseSchema>;

interface CreatePurchaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultCardId?: string;
}

export function CreatePurchaseModal({
  isOpen,
  onClose,
  defaultCardId = "",
}: CreatePurchaseModalProps) {
  const { mutateAsync: createPurchase, isPending } = useCreatePurchase();
  const { data: cards = [] } = useCards();
  const { categories = [] } = useCategories();

  const today = new Date().toISOString().split("T")[0];

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PurchaseForm>({
    resolver: zodResolver(purchaseSchema),
    defaultValues: {
      card_id: defaultCardId,
      total_installments: 1,
      purchase_date: today,
      store: "Geral",
    },
  });

  // Filtra categorias apenas para despesas
  const expenseCategories = categories.filter(
    (cat: { type: string; id: string; name: string }) =>
      cat.type === "saida" || cat.type === "expense",
  );

  if (!isOpen) return null;

  async function onSubmit(data: PurchaseForm) {
    try {
      await createPurchase({
        ...data,
        category_id: data.category_id || "",
      });
      reset({
        card_id: defaultCardId,
        total_installments: 1,
        purchase_date: today,
        store: "Geral",
      });
      onClose();
    } catch (error) {
      console.error("Erro ao registrar compra:", error);
      alert(
        "Não foi possível registrar a compra. Verifique o limite do cartão e tente novamente.",
      );
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden border border-subtle/30 transition-all duration-300">
        {/* Cabeçalho */}
        <div className="flex justify-between items-center p-6 border-b border-subtle/20">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Nova Compra no Cartão
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
              Lance uma despesa à vista ou parcelada para acompanhamento
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
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="p-6 space-y-4 overflow-y-auto max-h-[75vh]"
        >
          {/* Cartão de Crédito */}
          <div>
            <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              <CreditCard size={13} className="text-muted" />
              <span>Cartão Utilizado *</span>
            </label>
            <select
              {...register("card_id")}
              className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
            >
              <option value="" className="bg-surface text-muted">
                Selecione o cartão...
              </option>
              {cards.map((card) => (
                <option
                  key={card.id}
                  value={card.id}
                  className="bg-surface text-primary"
                >
                  {card.name} ({card.brand}) — Limite disp: R${" "}
                  {Number(card.available_limit).toFixed(2)}
                </option>
              ))}
            </select>
            {errors.card_id && (
              <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                {errors.card_id.message}
              </span>
            )}
          </div>

          {/* Descrição e Estabelecimento */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <ShoppingBag size={13} className="text-muted" />
                <span>Descrição da Compra *</span>
              </label>
              <input
                type="text"
                placeholder="Ex: MacBook Air, Jantar, Uber..."
                {...register("title")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs"
              />
              {errors.title && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.title.message}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Estabelecimento / Loja *
              </label>
              <input
                type="text"
                placeholder="Ex: Apple Store, iFood, Amazon..."
                {...register("store")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs"
              />
              {errors.store && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.store.message}
                </span>
              )}
            </div>
          </div>

          {/* Valor Total e Parcelas */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <DollarSign size={13} className="text-muted" />
                <span>Valor Total (R$) *</span>
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0,00"
                {...register("total_amount", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base sm:text-lg font-extrabold shadow-2xs tracking-tight"
              />
              {errors.total_amount && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.total_amount.message}
                </span>
              )}
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Layers size={13} className="text-muted" />
                <span>Nº de Parcelas *</span>
              </label>
              <select
                {...register("total_installments", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value={1} className="bg-surface text-primary">
                  1x (À vista no vencimento)
                </option>
                {[2, 3, 4, 5, 6, 8, 10, 12, 18, 24, 36].map((num) => (
                  <option
                    key={num}
                    value={num}
                    className="bg-surface text-primary"
                  >
                    {num}x parceladas
                  </option>
                ))}
              </select>
              {errors.total_installments && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.total_installments.message}
                </span>
              )}
            </div>
          </div>

          {/* Data e Categoria */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Calendar size={13} className="text-muted" />
                <span>Data da Compra *</span>
              </label>
              <input
                type="date"
                {...register("purchase_date")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
              />
              {errors.purchase_date && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.purchase_date.message}
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
                {expenseCategories.map((cat: { id: string; name: string }) => (
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

          {/* Observação (Opcional) */}
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Observação (Opcional)
            </label>
            <input
              type="text"
              placeholder="Ex: Presente de aniversário, compra corporativa..."
              {...register("observation")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-2.5 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-medium shadow-2xs"
            />
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
              {isPending ? "Processando..." : "Lançar Compra"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
