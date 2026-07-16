import { useState } from "react";
import {
  X,
  SlidersHorizontal,
  ChevronDown,
  Check,
  DollarSign,
} from "lucide-react";
import type { FinancialEventFilters } from "../types";

interface AdvancedFiltersDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  filters: FinancialEventFilters;
  onChange: (newFilters: FinancialEventFilters) => void;
  onClear: () => void;
}

const MOCK_CATEGORIES = [
  { id: "cat-1", name: "Alimentação" },
  { id: "cat-2", name: "Moradia" },
  { id: "cat-3", name: "Transporte" },
  { id: "cat-4", name: "Lazer" },
];
const MOCK_ACCOUNTS = [
  { id: "acc-1", name: "Itaú" },
  { id: "acc-2", name: "Nubank" },
  { id: "acc-3", name: "Bradesco" },
];
const MOCK_CARDS = [
  { id: "card-1", name: "Visa Infinite" },
  { id: "card-2", name: "Mastercard Black" },
];

export function AdvancedFiltersDrawer({
  isOpen,
  onClose,
  filters,
  onChange,
  onClear,
}: AdvancedFiltersDrawerProps) {
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({
    nature: true,
    flow: true,
    status: false,
    values: true,
    entities: false,
  });

  if (!isOpen) return null;

  const toggleGroup = (group: string) =>
    setOpenGroups((prev) => ({ ...prev, [group]: !prev[group] }));

  const toggleArrayFilter = (
    key: keyof FinancialEventFilters,
    value: string,
  ) => {
    const currentArray = (filters[key] as string[]) || [];
    const newArray = currentArray.includes(value)
      ? currentArray.filter((item) => item !== value)
      : [...currentArray, value];

    onChange({ ...filters, [key]: newArray.length > 0 ? newArray : undefined });
  };

  const handleAmountChange = (
    key: "minAmount" | "maxAmount",
    value: string,
  ) => {
    const numericValue = value ? Number(value) : undefined;
    onChange({ ...filters, [key]: numericValue });
  };

  const renderCheckboxOption = (
    key: keyof FinancialEventFilters,
    value: string,
    label: string,
  ) => {
    const isActive = (filters[key] as string[])?.includes(value);
    return (
      <label
        className={`flex items-center gap-3 p-3.5 rounded-2xl border transition-all cursor-pointer select-none ${isActive ? "bg-brand/10 border-brand/40 text-brand shadow-sm" : "bg-surface border-subtle/20 text-secondary hover:bg-elevated/60 hover:border-subtle/30"}`}
      >
        <div
          className={`w-5 h-5 rounded-full flex items-center justify-center transition-all border-2 shrink-0 ${isActive ? "bg-brand border-brand text-white" : "border-subtle/40 bg-transparent"}`}
        >
          {isActive && <Check size={12} strokeWidth={3} />}
        </div>
        <span
          className={`text-sm font-semibold flex-1 ${isActive ? "text-primary" : "text-secondary"}`}
        >
          {label}
        </span>
      </label>
    );
  };

  const renderPillOption = (
    key: keyof FinancialEventFilters,
    value: string,
    label: string,
  ) => {
    const isActive = (filters[key] as string[])?.includes(value);
    return (
      <button
        onClick={() => toggleArrayFilter(key, value)}
        className={`px-3 py-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${isActive ? "bg-brand text-white border-brand shadow-sm" : "bg-elevated border-subtle/20 text-secondary hover:text-primary hover:border-subtle/40"}`}
      >
        {label}
      </button>
    );
  };

  return (
    // ✨ WRAPPER RESPONSIVO: P-4 cria as margens (Safe Area) no mobile, impedindo que o modal toque nas bordas
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-0 sm:justify-end">
      {/* Overlay Escuro */}
      <div
        className="absolute inset-0 bg-surface/80 backdrop-blur-sm animate-fade-in"
        onClick={onClose}
      />

      {/* ✨ O PAINEL: max-h-[80dvh] impede que o modal estoure a tela. 
          flex e flex-col garantem que o cabeçalho e rodapé fiquem fixos enquanto o miolo rola. */}
      <div className="relative w-full max-w-md sm:max-w-none sm:w-100 max-h-[80dvh] sm:max-h-none sm:h-dvh bg-surface rounded-3xl sm:rounded-none border border-subtle/20 sm:border-0 sm:border-l shadow-2xl flex flex-col animate-scale-in sm:animate-slide-in-right overflow-hidden">
        {/* HEADER (Fixo - shrink-0) */}
        <div className="flex items-center justify-between px-5 py-4 sm:px-6 sm:py-5 border-b border-subtle/20 bg-elevated/30 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-brand/10 text-brand flex items-center justify-center">
              <SlidersHorizontal size={16} />
            </div>
            <h2 className="text-base font-extrabold text-primary">
              Filtros Avançados
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-muted hover:text-primary hover:bg-surface rounded-full transition-colors cursor-pointer border border-transparent hover:border-subtle/30"
          >
            <X size={20} />
          </button>
        </div>

        {/* BODY (Com Scroll Interno - flex-1 overflow-y-auto) */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-5 sm:space-y-6 scrollbar-thin">
          <div className="space-y-4">
            <button
              onClick={() => toggleGroup("values")}
              className="w-full flex items-center justify-between py-2 text-primary hover:text-brand transition-colors cursor-pointer"
            >
              <h3 className="text-[11px] sm:text-xs font-extrabold tracking-widest uppercase">
                Valores
              </h3>
              <ChevronDown
                size={16}
                className={`transition-transform ${openGroups.values ? "rotate-180" : ""}`}
              />
            </button>
            {openGroups.values && (
              <div className="flex items-center gap-3 animate-fade-in">
                <div className="relative flex-1">
                  <DollarSign
                    size={14}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                  />
                  <input
                    type="number"
                    placeholder="Mínimo"
                    value={filters.minAmount || ""}
                    onChange={(e) =>
                      handleAmountChange("minAmount", e.target.value)
                    }
                    className="w-full pl-8 pr-4 py-2.5 bg-elevated border border-subtle/20 rounded-xl text-xs sm:text-sm font-semibold text-primary focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand transition-all"
                  />
                </div>
                <span className="text-muted font-bold">-</span>
                <div className="relative flex-1">
                  <DollarSign
                    size={14}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                  />
                  <input
                    type="number"
                    placeholder="Máximo"
                    value={filters.maxAmount || ""}
                    onChange={(e) =>
                      handleAmountChange("maxAmount", e.target.value)
                    }
                    className="w-full pl-8 pr-4 py-2.5 bg-elevated border border-subtle/20 rounded-xl text-xs sm:text-sm font-semibold text-primary focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand transition-all"
                  />
                </div>
              </div>
            )}
          </div>

          <div className="w-full h-px bg-subtle/10" />

          <div className="space-y-5">
            <h3 className="text-[11px] sm:text-xs font-extrabold tracking-widest uppercase text-primary">
              Classificação
            </h3>

            <div className="space-y-2">
              <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                Natureza do Lançamento
              </p>
              <div className="flex flex-col gap-2">
                <div onClick={() => toggleArrayFilter("type", "transaction")}>
                  {renderCheckboxOption(
                    "type",
                    "transaction",
                    "À vista (Lançamento Único)",
                  )}
                </div>
                <div onClick={() => toggleArrayFilter("type", "installment")}>
                  {renderCheckboxOption(
                    "type",
                    "installment",
                    "Parcelamentos (Crédito)",
                  )}
                </div>
                <div onClick={() => toggleArrayFilter("type", "subscription")}>
                  {renderCheckboxOption(
                    "type",
                    "subscription",
                    "Assinaturas (Recorrentes)",
                  )}
                </div>
              </div>
            </div>

            <div className="flex gap-3 sm:gap-4">
              <div className="flex-1 space-y-2">
                <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                  Fluxo
                </p>
                <div className="flex flex-col gap-2">
                  <div onClick={() => toggleArrayFilter("flow", "income")}>
                    {renderCheckboxOption("flow", "income", "Entradas")}
                  </div>
                  <div onClick={() => toggleArrayFilter("flow", "expense")}>
                    {renderCheckboxOption("flow", "expense", "Saídas")}
                  </div>
                </div>
              </div>
              <div className="flex-1 space-y-2">
                <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                  Status
                </p>
                <div className="flex flex-col gap-2">
                  <div onClick={() => toggleArrayFilter("status", "completed")}>
                    {renderCheckboxOption("status", "completed", "Pagos")}
                  </div>
                  <div onClick={() => toggleArrayFilter("status", "pending")}>
                    {renderCheckboxOption("status", "pending", "Pendentes")}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="w-full h-px bg-subtle/10" />

          <div className="space-y-4 pb-2">
            <button
              onClick={() => toggleGroup("entities")}
              className="w-full flex items-center justify-between py-2 text-primary hover:text-brand transition-colors cursor-pointer"
            >
              <h3 className="text-[11px] sm:text-xs font-extrabold tracking-widest uppercase">
                Filtros de Origem
              </h3>
              <ChevronDown
                size={16}
                className={`transition-transform ${openGroups.entities ? "rotate-180" : ""}`}
              />
            </button>

            {openGroups.entities && (
              <div className="space-y-4 animate-fade-in">
                <div className="space-y-2">
                  <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                    Categorias
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {MOCK_CATEGORIES.map((cat) =>
                      renderPillOption("categoryIds", cat.id, cat.name),
                    )}
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                    Contas Bancárias
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {MOCK_ACCOUNTS.map((acc) =>
                      renderPillOption("accountIds", acc.id, acc.name),
                    )}
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-[10px] font-bold text-muted uppercase tracking-wider pl-1">
                    Cartões de Crédito
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {MOCK_CARDS.map((card) =>
                      renderPillOption("cardIds", card.id, card.name),
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* FOOTER (Fixo - shrink-0) - Os botões nunca mais sumirão! */}
        <div className="p-5 sm:p-6 border-t border-subtle/20 bg-elevated/30 flex gap-3 shrink-0">
          <button
            onClick={onClear}
            className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 text-xs sm:text-sm font-bold text-secondary hover:bg-surface hover:text-primary transition-colors cursor-pointer shadow-sm"
          >
            Limpar Filtros
          </button>
          <button
            onClick={onClose}
            className="flex-1 px-4 py-3 rounded-xl bg-brand text-white text-xs sm:text-sm font-bold shadow-sm hover:bg-brand-light transition-colors cursor-pointer"
          >
            Ver Resultados
          </button>
        </div>
      </div>
    </div>
  );
}
