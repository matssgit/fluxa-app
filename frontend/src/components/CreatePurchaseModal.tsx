import { useState } from "react";
import {
  X,
  ShoppingBag,
  DollarSign,
  Calendar,
  CreditCard,
  Tag,
  Layers,
  ChevronDown,
  Check,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
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
  total_installments: z.number().min(1).max(72),
  purchase_date: z.string().min(1, "A data da compra é obrigatória"),
  observation: z.string().optional(),
});

type PurchaseForm = z.infer<typeof purchaseSchema>;

interface CreatePurchaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultCardId?: string;
}

const INSTALLMENT_OPTIONS = [1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 18, 24, 36];

export function CreatePurchaseModal({
  isOpen,
  onClose,
  defaultCardId = "",
}: CreatePurchaseModalProps) {
  const { mutateAsync: createPurchase, isPending } = useCreatePurchase();
  const { data: cards = [] } = useCards();
  const { categories = [] } = useCategories();

  // ✨ ESTADO DO MODAL CENTRADO DE PARCELAS
  const [isInstallmentModalOpen, setIsInstallmentModalOpen] = useState(false);

  const today = new Date().toISOString().split("T")[0];

  const { register, handleSubmit, reset, control, setValue } =
    useForm<PurchaseForm>({
      resolver: zodResolver(purchaseSchema),
      defaultValues: {
        card_id: defaultCardId,
        total_installments: 1,
        purchase_date: today,
        store: "Geral",
      },
    });

  const selectedInstallments = useWatch({
    control,
    name: "total_installments",
    defaultValue: 1,
  });

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
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        {/* ✨ LAYOUT COMPACTO: max-h-[82vh] garante que nunca invada o cabeçalho ou o rodapé da tela */}
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-lg max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
            <div>
              <h2 className="text-base sm:text-xl font-bold text-primary tracking-tight">
                Nova Compra no Cartão
              </h2>
              <p className="text-xs font-medium text-muted mt-0.5 hidden sm:block">
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

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-3.5 sm:space-y-4 overflow-y-auto flex-1"
          >
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <CreditCard size={13} className="text-muted" />
                <span>Cartão Utilizado *</span>
              </label>
              <select
                {...register("card_id")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
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
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <ShoppingBag size={13} className="text-muted" />
                  <span>Descrição da Compra *</span>
                </label>
                <input
                  type="text"
                  placeholder="Ex: MacBook Air, Jantar..."
                  {...register("title")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
                />
              </div>

              <div>
                <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  Estabelecimento / Loja *
                </label>
                <input
                  type="text"
                  placeholder="Ex: Apple Store, iFood..."
                  {...register("store")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <DollarSign size={13} className="text-muted" />
                  <span>Valor Total (R$) *</span>
                </label>
                <input
                  type="number"
                  step="0.01"
                  placeholder="0,00"
                  {...register("total_amount", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm sm:text-base font-extrabold shadow-2xs tracking-tight"
                />
              </div>

              {/* ✨ BOTÃO QUE ABRE O MODAL CENTRADO DE PARCELAS EM VEZ DE QUEBRAR O LAYOUT */}
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Layers size={13} className="text-muted" />
                  <span>Nº de Parcelas *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsInstallmentModalOpen(true)}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs flex items-center justify-between cursor-pointer"
                >
                  <span>
                    {selectedInstallments === 1
                      ? "1x (À vista no vencimento)"
                      : `${selectedInstallments}x parceladas`}
                  </span>
                  <ChevronDown size={16} className="text-muted" />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Calendar size={13} className="text-muted" />
                  <span>Data da Compra *</span>
                </label>
                <input
                  type="date"
                  {...register("purchase_date")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
                />
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
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
                  {expenseCategories.map(
                    (cat: { id: string; name: string }) => (
                      <option
                        key={cat.id}
                        value={cat.id}
                        className="bg-surface text-primary"
                      >
                        {cat.name}
                      </option>
                    ),
                  )}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                Observação (Opcional)
              </label>
              <input
                type="text"
                placeholder="Ex: Presente de aniversário..."
                {...register("observation")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-medium shadow-2xs"
              />
            </div>
          </form>

          <div className="p-4 sm:p-6 bg-surface border-t border-subtle/20 flex gap-3 shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all duration-200 cursor-pointer shadow-2xs"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={handleSubmit(onSubmit)}
              disabled={isPending}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isPending ? "Processando..." : "Lançar Compra"}
            </button>
          </div>
        </div>
      </div>

      {/* ✨ MODAL CENTRADO DE SELEÇÃO DE PARCELAS: Abre limpo no meio da tela com scroll suave! */}
      {isInstallmentModalOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-xs max-h-[60vh] flex flex-col overflow-hidden border border-subtle/30 animate-in zoom-in-95 duration-200">
            <div className="p-4 border-b border-subtle/20 flex justify-between items-center shrink-0">
              <h3 className="font-bold text-primary text-sm">
                Selecione as Parcelas
              </h3>
              <button
                type="button"
                onClick={() => setIsInstallmentModalOpen(false)}
                className="p-1 text-muted hover:text-primary rounded-full cursor-pointer"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-2 overflow-y-auto divide-y divide-subtle/10 flex-1">
              {INSTALLMENT_OPTIONS.map((num) => {
                const isSelected = selectedInstallments === num;
                return (
                  <button
                    key={num}
                    type="button"
                    onClick={() => {
                      setValue("total_installments", num, {
                        shouldDirty: true,
                      });
                      setIsInstallmentModalOpen(false);
                    }}
                    className={`w-full p-3.5 rounded-xl text-left text-xs sm:text-sm font-bold flex items-center justify-between transition-colors cursor-pointer ${
                      isSelected
                        ? "bg-brand/10 text-brand"
                        : "text-primary hover:bg-elevated/60"
                    }`}
                  >
                    <span>
                      {num === 1
                        ? "1x (À vista no vencimento)"
                        : `${num}x parceladas`}
                    </span>
                    {isSelected && (
                      <Check size={16} className="text-brand stroke-3" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
