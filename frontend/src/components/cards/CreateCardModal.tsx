import {
  X,
  CreditCard,
  Calendar,
  DollarSign,
  Palette,
  Check,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreateCard } from "../../hooks/useCredit";
import { CARD_COLORS } from "../../utils/cardColors";

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

// Array de fallback para garantir variedade de cores caso o mapa não encontre
const FALLBACK_VIBRANT_COLORS = [
  "#10B981",
  "#F59E0B",
  "#8B5CF6",
  "#3B82F6",
  "#F43F5E",
  "#06B6D4",
];

export function CreateCardModal({ isOpen, onClose }: CreateCardModalProps) {
  const { mutateAsync: createCard, isPending } = useCreateCard();

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<CardForm>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      brand: "Mastercard",
      color: "emerald",
      due_day: 10,
    },
  });

  const selectedColor = useWatch({
    control,
    name: "color",
    defaultValue: "emerald",
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

  // Resolvedor que agora garante variedade caso não encontre no mapa
  const getHexColor = (key: string, val: unknown, index: number) => {
    if (typeof val === "string" && val.startsWith("#")) return val;
    if (typeof val === "object" && val !== null) {
      const obj = val as Record<string, unknown>;
      if (typeof obj.hex === "string") return obj.hex;
      if (typeof obj.color === "string") return obj.color;
      if (typeof obj.bg === "string" && obj.bg.startsWith("#")) return obj.bg;
    }

    // Se não achar no mapa, usa o fallback vibrante baseado no index para garantir variedade
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-subtle/30 transition-all duration-300">
        <div className="flex justify-between items-center p-6 border-b border-subtle/20">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Novo Cartão de Crédito
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
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

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div>
            <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              <CreditCard size={13} className="text-muted" />
              <span>Nome Identificador *</span>
            </label>
            <input
              type="text"
              placeholder="Ex: Nubank Black, XP Infinite, Itaú..."
              {...register("name")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs"
            />
            {errors.name && (
              <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                {errors.name.message}
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Bandeira *
              </label>
              <select
                {...register("brand")}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
              >
                <option value="Mastercard" className="bg-surface text-primary">
                  Mastercard
                </option>
                <option value="Visa" className="bg-surface text-primary">
                  Visa
                </option>
                <option value="Elo" className="bg-surface text-primary">
                  Elo
                </option>
                <option
                  value="American Express"
                  className="bg-surface text-primary"
                >
                  American Express
                </option>
                <option value="Hipercard" className="bg-surface text-primary">
                  Hipercard
                </option>
                <option value="Outra" className="bg-surface text-primary">
                  Outra
                </option>
              </select>
              {errors.brand && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.brand.message}
                </span>
              )}
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <DollarSign size={13} className="text-muted" />
                <span>Limite Total (R$) *</span>
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0,00"
                {...register("limit_amount", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-extrabold shadow-2xs tracking-tight"
              />
              {errors.limit_amount && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.limit_amount.message}
                </span>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Calendar size={13} className="text-muted" />
                <span>Dia de Vencimento *</span>
              </label>
              <input
                type="number"
                min={1}
                max={31}
                placeholder="Ex: 10"
                {...register("due_day", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-semibold shadow-2xs"
              />
              {errors.due_day && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.due_day.message}
                </span>
              )}
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Palette size={13} className="text-muted" />
                <span>Cor do Cartão</span>
              </label>
              <div className="flex flex-wrap items-center gap-2 pt-1.5 px-0.5">
                {paletteEntries.map(([colorKey, colorVal], index) => {
                  const hexColor = getHexColor(colorKey, colorVal, index);
                  const isSelected = selectedColor === colorKey;

                  return (
                    <label
                      key={colorKey}
                      className="cursor-pointer group"
                      title={colorKey}
                    >
                      <input
                        type="radio"
                        value={colorKey}
                        {...register("color")}
                        className="sr-only"
                      />
                      <div
                        className={`w-8 h-8 rounded-xl transition-all flex items-center justify-center shadow-xs ${
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
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>
          </div>

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
              {isPending ? "Criando..." : "Salvar Cartão"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
