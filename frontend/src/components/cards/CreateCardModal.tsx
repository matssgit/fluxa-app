import { useState } from "react";
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
import { useCreateCard } from "../../hooks/useCredit";

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

export function CreateCardModal({ isOpen, onClose }: CreateCardModalProps) {
  const { mutateAsync: createCard, isPending } = useCreateCard();

  // ✨ ESTADO DO ARCO-ÍRIS CENTRADO
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

  const selectedColor = useWatch({
    control,
    name: "color",
    defaultValue: "#10B981",
  });

  if (!isOpen) return null;

  async function onSubmit(data: CardForm) {
    try {
      await createCard({
        ...data,
        color: data.color || "#10B981",
      });
      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar cartão:", error);
      alert(
        "Não foi possível cadastrar o cartão. Verifique os dados e tente novamente.",
      );
    }
  }

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        {/* ✨ LAYOUT COMPACTO: max-h-[82vh] e flex-col garantem respiro na tela do celular */}
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
            onSubmit={handleSubmit(onSubmit)}
            className="p-4 sm:p-6 space-y-3.5 sm:space-y-4 overflow-y-auto flex-1"
          >
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

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  Bandeira *
                </label>
                <select
                  {...register("brand")}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer"
                >
                  <option
                    value="Mastercard"
                    className="bg-surface text-primary"
                  >
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
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 sm:gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Calendar size={13} className="text-muted" />
                  <span>Dia de Vencimento *</span>
                </label>
                <input
                  type="number"
                  min={1}
                  max={31}
                  placeholder="Ex: 10"
                  {...register("due_day", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs"
                />
              </div>

              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                  <Palette size={13} className="text-muted" />
                  <span>Cor do Cartão</span>
                </label>
                <div className="flex flex-wrap items-center gap-1.5 pt-1 px-0.5">
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
                        className={`w-7 h-7 sm:w-8 sm:h-8 rounded-xl transition-all flex items-center justify-center shadow-xs cursor-pointer ${
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

                  {/* ✨ BOTÃO DE PIPETA QUE ABRE O MODAL CENTRADO NO MEIO DA TELA */}
                  <button
                    type="button"
                    onClick={() => setIsColorPickerOpen(true)}
                    title="Personalizar Cor Exata (Hexadecimal)"
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
              type="button"
              onClick={handleSubmit(onSubmit)}
              disabled={isPending}
              className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold shadow-sm transition-all duration-200 cursor-pointer disabled:opacity-50"
            >
              {isPending ? "Criando..." : "Salvar Cartão"}
            </button>
          </div>
        </div>
      </div>

      {/* ✨ MODAL DO ARCO-ÍRIS CENTRADO: O seletor abre exatamente no centro do celular! */}
      {isColorPickerOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface p-6 rounded-3xl shadow-2xl border border-subtle/30 flex flex-col items-center space-y-4 max-w-xs w-full animate-in zoom-in-95 duration-200">
            <div className="text-center">
              <h3 className="font-bold text-primary text-base">
                Escolha a Cor Exata
              </h3>
              <p className="text-xs text-muted mt-0.5">
                Selecione qualquer tom no círculo abaixo
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

            <div className="flex items-center gap-2 bg-elevated px-3 py-1.5 rounded-xl border border-subtle/20 w-full justify-center">
              <div
                className="w-4 h-4 rounded-full border border-white/20"
                style={{ backgroundColor: selectedColor }}
              />
              <span className="font-mono text-xs font-extrabold text-secondary tracking-wider">
                {selectedColor?.toUpperCase()}
              </span>
            </div>

            <button
              type="button"
              onClick={() => setIsColorPickerOpen(false)}
              className="w-full py-3 bg-brand hover:bg-brand-light text-white font-bold text-xs sm:text-sm rounded-xl shadow-sm transition-all cursor-pointer mt-2"
            >
              Confirmar Esta Cor
            </button>
          </div>
        </div>
      )}
    </>
  );
}
