import { useState } from "react";
import {
  X,
  CreditCard,
  Calendar,
  DollarSign,
  Palette,
  Check,
  Pipette,
  ChevronDown,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreateCard } from "../../hooks/useCredit";
import { CARD_COLORS } from "../../utils/cardColors";

// Infraestrutura UX Padrão (Pine & Sage)
import { PickerModal } from "../ui/PickerModal";

const cardSchema = z.object({
  name: z.string().min(2, "O nome do cartão é obrigatório"),
  brand: z.string().min(2, "Selecione ou digite a bandeira"),
  limit_amount: z.number().min(1, "O limite deve ser maior que zero"),
  due_day: z.number().min(1, "Dia mínimo: 1").max(31, "Dia máximo: 31"),
  color: z.string().optional(),
});

type CardForm = z.infer<typeof cardSchema>;

interface CreateCardModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const COLOR_HEX_MAP: Record<string, string> = {
  emerald: "#10B981",
  verde: "#10B981",
  green: "#10B981",
  dark: "#1E293B",
  preto: "#1E293B",
  black: "#1E293B",
  slate: "#334155",
  gold: "#F59E0B",
  ouro: "#F59E0B",
  amber: "#F59E0B",
  yellow: "#EAB308",
  purple: "#8B5CF6",
  roxo: "#8B5CF6",
  indigo: "#6366F1",
  blue: "#3B82F6",
  azul: "#3B82F6",
  rose: "#F43F5E",
  pink: "#EC4899",
  red: "#EF4444",
  vermelho: "#EF4444",
  cyan: "#06B6D4",
  teal: "#14B8A6",
};

const FALLBACK_VIBRANT_COLORS = [
  "#10B981",
  "#F59E0B",
  "#8B5CF6",
  "#3B82F6",
  "#F43F5E",
  "#06B6D4",
];

// Gerador de Dias de Vencimento para o PickerModal (1 a 31)
const DUE_DAY_OPTIONS = Array.from({ length: 31 }, (_, i) => ({
  label: `Dia ${i + 1}`,
  value: i + 1,
}));

export function CreateCardModal({ isOpen, onClose }: CreateCardModalProps) {
  const { mutateAsync: createCard, isPending } = useCreateCard();

  // ✨ ESTADOS DOS MODAIS UX
  const [isBrandModalOpen, setIsBrandModalOpen] = useState(false);
  const [isDueDayModalOpen, setIsDueDayModalOpen] = useState(false);
  const [isColorPickerOpen, setIsColorPickerOpen] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors },
  } = useForm<CardForm>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      brand: "Mastercard",
      color: "#10B981",
      due_day: 10,
    },
  });

  // ✨ OBSERVADORES (React Compiler Safe)
  const selectedBrand = useWatch({
    control,
    name: "brand",
    defaultValue: "Mastercard",
  });
  const selectedDueDay = useWatch({
    control,
    name: "due_day",
    defaultValue: 10,
  });
  const selectedColor = useWatch({
    control,
    name: "color",
    defaultValue: "#10B981",
  });

  if (!isOpen) return null;

  async function onSubmit(data: CardForm) {
    try {
      await createCard(data);
      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar cartão:", error);
      alert(
        "Não foi possível cadastrar o cartão. Verifique os dados e tente novamente.",
      );
    }
  }

  const getHexColor = (key: string, val: unknown, index: number) => {
    if (typeof val === "string" && val.startsWith("#")) return val;
    if (typeof val === "object" && val !== null) {
      const obj = val as Record<string, unknown>;
      if (typeof obj.hex === "string") return obj.hex;
      if (typeof obj.color === "string") return obj.color;
      if (typeof obj.bg === "string" && obj.bg.startsWith("#")) return obj.bg;
    }
    return (
      COLOR_HEX_MAP[key.toLowerCase()] ||
      FALLBACK_VIBRANT_COLORS[index % FALLBACK_VIBRANT_COLORS.length]
    );
  };

  const paletteEntries =
    CARD_COLORS && Object.keys(CARD_COLORS).length > 0
      ? Object.entries(CARD_COLORS)
      : Object.entries({
          emerald: "Esmeralda",
          dark: "Black",
          gold: "Ouro",
          purple: "Roxo",
          blue: "Azul",
          rose: "Rose",
        });

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
            <div>
              <h2 className="text-base sm:text-xl font-bold text-primary tracking-tight">
                Novo Cartão de Crédito
              </h2>
              <p className="text-xs font-medium text-muted mt-0.5 hidden sm:block">
                Adicione um novo cartão para gerenciar suas faturas e limites
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
            id="create-card-form"
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-3.5 sm:space-y-4 overflow-y-auto flex-1"
          >
            {/* Nome do Cartão */}
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <CreditCard size={13} className="text-muted" />
                <span>Nome Identificador *</span>
              </label>
              <input
                type="text"
                placeholder="Ex: Nubank Black, XP Infinite, Itaú..."
                {...register("name")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
              />
              {errors.name && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.name.message}
                </span>
              )}
            </div>

            {/* Bandeira e Limite */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  Bandeira *
                </label>
                {/* ✨ TRIGGER BUTTON: Bandeira */}
                <button
                  type="button"
                  onClick={() => setIsBrandModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span>{selectedBrand}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
                {errors.brand && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.brand.message}
                  </span>
                )}
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <DollarSign size={13} className="text-muted" />
                  <span>Limite Total (R$) *</span>
                </label>
                <input
                  type="number"
                  step="0.01"
                  placeholder="0,00"
                  {...register("limit_amount", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-extrabold shadow-2xs tracking-tight"
                />
                {errors.limit_amount && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.limit_amount.message}
                  </span>
                )}
              </div>
            </div>

            {/* Dia de Vencimento e Cor */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Calendar size={13} className="text-muted" />
                  <span>Dia de Vencimento *</span>
                </label>
                {/* ✨ TRIGGER BUTTON: Dia de Vencimento */}
                <button
                  type="button"
                  onClick={() => setIsDueDayModalOpen(true)}
                  className="w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface text-primary outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <span>Dia {selectedDueDay}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
                {errors.due_day && (
                  <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                    {errors.due_day.message}
                  </span>
                )}
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Palette size={13} className="text-muted" />
                  <span>Cor do Cartão</span>
                </label>
                <div className="flex flex-wrap items-center gap-1.5 pt-1 px-0.5">
                  {paletteEntries.map(([colorKey, colorVal], index) => {
                    const hexColor = getHexColor(colorKey, colorVal, index);
                    const isSelected =
                      selectedColor?.toLowerCase() === hexColor.toLowerCase() ||
                      selectedColor === colorKey;

                    return (
                      <button
                        key={colorKey}
                        type="button"
                        onClick={() =>
                          setValue("color", hexColor, { shouldDirty: true })
                        }
                        title={colorKey}
                        className={`w-7 h-7 sm:w-8 sm:h-8 rounded-xl transition-all flex items-center justify-center shadow-xs cursor-pointer ${
                          isSelected
                            ? "scale-110 ring-2 ring-brand ring-offset-2 ring-offset-surface"
                            : "opacity-70 hover:opacity-100 hover:scale-105"
                        }`}
                        style={{ backgroundColor: hexColor }}
                      >
                        {isSelected && (
                          <Check
                            size={14}
                            className="text-white drop-shadow-sm stroke-3"
                          />
                        )}
                      </button>
                    );
                  })}

                  <button
                    type="button"
                    onClick={() => setIsColorPickerOpen(true)}
                    title="Personalizar Cor Exata"
                    className="w-7 h-7 sm:w-8 sm:h-8 rounded-xl bg-linear-to-tr from-rose-500 via-amber-500 to-indigo-500 flex items-center justify-center cursor-pointer shadow-xs hover:scale-105 transition-transform active:scale-95"
                  >
                    <Pipette
                      size={14}
                      className="text-white drop-shadow-md stroke-2.5"
                    />
                  </button>
                </div>
              </div>
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
              type="submit"
              form="create-card-form"
              disabled={isPending}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isPending ? "Criando..." : "Salvar Cartão"}
            </button>
          </div>
        </div>
      </div>

      {/* ========================================= */}
      {/* 🚀 MODAIS DE INFRAESTRUTURA UX (OVERLAYS) */}
      {/* ========================================= */}

      <PickerModal
        isOpen={isBrandModalOpen}
        onClose={() => setIsBrandModalOpen(false)}
        title="Bandeira do Cartão"
        selectedValue={selectedBrand}
        onSelect={(val) =>
          setValue("brand", val as string, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={[
          { label: "Mastercard", value: "Mastercard" },
          { label: "Visa", value: "Visa" },
          { label: "Elo", value: "Elo" },
          { label: "American Express", value: "American Express" },
          { label: "Hipercard", value: "Hipercard" },
          { label: "Outra", value: "Outra" },
        ]}
      />

      <PickerModal
        isOpen={isDueDayModalOpen}
        onClose={() => setIsDueDayModalOpen(false)}
        title="Dia de Vencimento"
        selectedValue={selectedDueDay}
        onSelect={(val) =>
          setValue("due_day", val as number, {
            shouldValidate: true,
            shouldDirty: true,
          })
        }
        options={DUE_DAY_OPTIONS}
      />

      {isColorPickerOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface p-6 rounded-3xl shadow-2xl border border-subtle/30 flex flex-col items-center space-y-4 max-w-xs w-full animate-in zoom-in-95 duration-200">
            <div className="text-center">
              <h3 className="font-bold text-primary text-base">
                Escolha a Cor Exata
              </h3>
              <p className="text-xs text-muted mt-0.5">
                Selecione qualquer tom no círculo
              </p>
            </div>
            <div className="p-2 bg-elevated/60 rounded-full border border-subtle/20 shadow-inner">
              <input
                type="color"
                value={
                  selectedColor?.startsWith("#") ? selectedColor : "#10B981"
                }
                onChange={(e) =>
                  setValue("color", e.target.value, { shouldDirty: true })
                }
                className="w-24 h-24 rounded-full cursor-pointer border-0 bg-transparent overflow-hidden block"
              />
            </div>
            <button
              type="button"
              onClick={() => setIsColorPickerOpen(false)}
              className="w-full py-3 bg-brand hover:bg-brand-light text-white font-bold text-xs sm:text-sm rounded-xl shadow-sm transition-all cursor-pointer mt-2"
            >
              Confirmar
            </button>
          </div>
        </div>
      )}
    </>
  );
}
