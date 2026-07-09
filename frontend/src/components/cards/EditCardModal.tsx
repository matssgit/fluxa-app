import { useEffect } from "react";
import {
  X,
  CreditCard,
  Calendar,
  DollarSign,
  Palette,
  Check,
  Pipette,
} from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEditCard, type Card } from "../../hooks/useCredit";

const editCardSchema = z.object({
  id: z.string(),
  name: z.string().min(2, "O nome do cartão é obrigatório"),
  brand: z.string().min(2, "Selecione ou digite a bandeira"),
  total_limit: z.number().min(1, "O limite deve ser maior que zero"),
  due_day: z.number().min(1, "Dia mínimo: 1").max(31, "Dia máximo: 31"),
  color: z.string().optional(),
});

type EditCardForm = z.infer<typeof editCardSchema>;

interface EditCardModalProps {
  isOpen: boolean;
  onClose: () => void;
  card: Card | null;
}

const PRESET_COLORS = [
  { name: "Esmeralda", hex: "#10B981" },
  { name: "Laranja Itaú", hex: "#F97316" },
  { name: "Roxo Nubank", hex: "#8B5CF6" },
  { name: "Black / Dark", hex: "#1E293B" },
  { name: "Ouro / Gold", hex: "#F59E0B" },
  { name: "Azul Infinite", hex: "#3B82F6" },
  { name: "Rose Gold", hex: "#F43F5E" },
  { name: "Ciano / Teal", hex: "#06B6D4" },
];

export function EditCardModal({ isOpen, onClose, card }: EditCardModalProps) {
  const { mutateAsync: editCard, isPending } = useEditCard();

  const {
    register,
    handleSubmit,
    reset,
    control,
    setValue,
    formState: { errors },
  } = useForm<EditCardForm>({
    resolver: zodResolver(editCardSchema),
  });

  const selectedColor = useWatch({
    control,
    name: "color",
    defaultValue: "#10B981",
  });

  // Reseta e preenche o formulário sempre que um novo cartão é focado
  useEffect(() => {
    if (card) {
      reset({
        id: card.id,
        name: card.name,
        brand: card.brand,
        total_limit: Number(card.total_limit || 0),
        due_day: card.due_day,
        color: card.color || "#10B981",
      });
    }
  }, [card, reset]);

  if (!isOpen || !card) return null;

  async function onSubmit(data: EditCardForm) {
    try {
      await editCard(data);
      onClose(); // Ao fechar, o modal de detalhes reabre e já reflete o novo estado reativo na hora!
    } catch (error) {
      console.error("Erro ao atualizar cartão:", error);
      alert("Não foi possível atualizar o cartão. Tente novamente.");
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-subtle/30 transition-all duration-300">
        <div className="flex justify-between items-center p-6 border-b border-subtle/20">
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
              Editar Cartão de Crédito
            </h2>
            <p className="text-xs font-medium text-muted mt-0.5">
              Atualize as configurações de limite, vencimento e cor
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
          <input type="hidden" {...register("id")} />

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
                {...register("total_limit", { valueAsNumber: true })}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-extrabold shadow-2xs tracking-tight"
              />
              {errors.total_limit && (
                <span className="text-red-500 text-xs font-semibold mt-1 pl-1 block">
                  {errors.total_limit.message}
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

            {/* ✨ PALETA NEUMÓRFICA + SELETOR DE COR HEXADECIMAL NATIVO */}
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                <Palette size={13} className="text-muted" />
                <span>Cor do Cartão</span>
              </label>
              <div className="flex flex-wrap items-center gap-2 pt-1.5 px-0.5">
                {PRESET_COLORS.map((preset) => {
                  const isSelected =
                    selectedColor?.toLowerCase() === preset.hex.toLowerCase();
                  return (
                    <button
                      key={preset.hex}
                      type="button"
                      onClick={() =>
                        setValue("color", preset.hex, { shouldDirty: true })
                      }
                      title={preset.name}
                      className={`w-8 h-8 rounded-xl transition-all flex items-center justify-center shadow-xs cursor-pointer ${
                        isSelected
                          ? "scale-110 ring-2 ring-brand ring-offset-2 ring-offset-surface"
                          : "opacity-70 hover:opacity-100 hover:scale-105"
                      }`}
                      style={{ backgroundColor: preset.hex }}
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

                {/* BOTÃO DA PIPETA DE COR CUSTOMIZADA (HEX LIVRE) */}
                <label
                  title="Personalizar Cor Exata (Hexadecimal)"
                  className="relative w-8 h-8 rounded-xl bg-linear-to-tr from-rose-500 via-amber-500 to-indigo-500 flex items-center justify-center cursor-pointer shadow-xs hover:scale-105 transition-transform active:scale-95"
                >
                  <input
                    type="color"
                    value={
                      selectedColor?.startsWith("#") ? selectedColor : "#10B981"
                    }
                    onChange={(e) =>
                      setValue("color", e.target.value, { shouldDirty: true })
                    }
                    className="sr-only"
                  />
                  <Pipette
                    size={14}
                    className="text-white drop-shadow-md stroke-2.5"
                  />
                </label>
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
              {isPending ? "Salvando..." : "Atualizar Cartão"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
