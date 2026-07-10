import { useState } from "react";
import {
  X,
  ShoppingBag,
  DollarSign,
  Calendar as CalendarIcon,
  CreditCard,
  Tag,
  Layers,
  ChevronDown,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreatePurchase, useCards, type Card } from "../hooks/useCredit";
import { useCategories } from "../hooks/useCategories";

// Infraestrutura UX Padrão (Pine & Sage)
import { PickerModal } from "./ui/PickerModal";
import { DatePickerModal } from "./ui/DatePickerModal";

// ✨ TIPAGEM ESTRITA PARA O LINTER
interface CategoryData {
  id: string;
  name: string;
  type: string;
}

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

  // ✨ ESTADOS DE CONTROLE DA INFRAESTRUTURA UX
  const [isCardModalOpen, setIsCardModalOpen] = useState(false);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  const [isInstallmentModalOpen, setIsInstallmentModalOpen] = useState(false);

  const today = new Date().toISOString().split("T")[0];

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
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

  // ✨ OBSERVADORES DE ESTADO (React Compiler Safe)
  const selectedCardId = useWatch({
    control,
    name: "card_id",
    defaultValue: defaultCardId,
  });
  const selectedCategoryId = useWatch({ control, name: "category_id" });
  const selectedDate = useWatch({
    control,
    name: "purchase_date",
    defaultValue: today,
  });
  const selectedInstallments = useWatch({
    control,
    name: "total_installments",
    defaultValue: 1,
  });

  const expenseCategories = categories.filter(
    (cat: CategoryData) => cat.type === "saida" || cat.type === "expense",
  );

  // Helpers de Interface
  const selectedCardData = cards.find((c: Card) => c.id === selectedCardId);
  const selectedCardName = selectedCardData
    ? `${selectedCardData.name} (${selectedCardData.brand})`
    : "Selecione o cartão...";

  const selectedCategoryName =
    expenseCategories.find((c: CategoryData) => c.id === selectedCategoryId)
      ?.name || "Sem categoria";

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
            id="create-purchase-form"
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-3.5 sm:space-y-4 overflow-y-auto flex-1"
          >
            {/* Cartão */}
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <CreditCard size={13} className="text-muted" />
                <span>Cartão Utilizado *</span>
              </label>
              {/* ✨ TRIGGER BUTTON: Cartão */}
              <button
                type="button"
                onClick={() => setIsCardModalOpen(true)}
                className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${!selectedCardId ? "text-muted" : "text-primary"}`}
              >
                <span className="truncate pr-2">{selectedCardName}</span>
                <ChevronDown size={16} className="text-muted shrink-0" />
              </button>
              {errors.card_id && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.card_id.message}
                </span>
              )}
            </div>

            {/* Descrição e Estabelecimento */}
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
                {errors.title && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.title.message}
                  </span>
                )}
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
                {errors.store && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.store.message}
                  </span>
                )}
              </div>
            </div>

            {/* Valor e Parcelas */}
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
                {errors.total_amount && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.total_amount.message}
                  </span>
                )}
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Layers size={13} className="text-muted" />
                  <span>Nº de Parcelas *</span>
                </label>
                {/* ✨ TRIGGER BUTTON: Parcelas */}
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
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>

            {/* Data e Categoria */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <CalendarIcon size={13} className="text-muted" />
                  <span>Data da Compra *</span>
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
                  <CalendarIcon size={16} className="text-muted shrink-0" />
                </button>
                {errors.purchase_date && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.purchase_date.message}
                  </span>
                )}
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
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

            {/* Observação */}
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

          {/* Rodapé Fixo */}
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
              form="create-purchase-form"
              disabled={isPending}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isPending ? "Processando..." : "Lançar Compra"}
            </button>
          </div>
        </div>
      </div>

      {/* ========================================= */}
      {/* 🚀 MODAIS DE INFRAESTRUTURA UX (OVERLAYS) */}
      {/* ========================================= */}

      <PickerModal
        isOpen={isCardModalOpen}
        onClose={() => setIsCardModalOpen(false)}
        title="Selecione o Cartão"
        selectedValue={selectedCardId}
        onSelect={(val) =>
          setValue("card_id", val as string, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={cards.map((card: Card) => ({
          label: `${card.name} (${card.brand})`,
          value: card.id,
          subtitle: `Limite disp: ${new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(card.available_limit))}`,
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
          ...expenseCategories.map((cat: CategoryData) => ({
            label: cat.name,
            value: cat.id,
          })),
        ]}
      />

      <PickerModal
        isOpen={isInstallmentModalOpen}
        onClose={() => setIsInstallmentModalOpen(false)}
        title="Selecione as Parcelas"
        selectedValue={selectedInstallments}
        onSelect={(val) =>
          setValue("total_installments", val as number, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={INSTALLMENT_OPTIONS.map((num) => ({
          label:
            num === 1 ? "1x (À vista no vencimento)" : `${num}x parceladas`,
          value: num,
        }))}
      />

      <DatePickerModal
        isOpen={isDateModalOpen}
        onClose={() => setIsDateModalOpen(false)}
        title="Data da Compra"
        selectedDate={selectedDate}
        onSelectDate={(date) =>
          setValue("purchase_date", date, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
      />
    </>
  );
}
