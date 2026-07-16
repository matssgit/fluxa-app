import { Search, SlidersHorizontal, X } from "lucide-react";
import type {
  FinancialEventFilters,
  FinancialEventFlow,
  FinancialEventStatus,
} from "../types";

interface TransactionToolbarProps {
  searchTerm: string | undefined;
  filters: FinancialEventFilters;
  onSearchChange: (value: string) => void;
  onFilterChange: (newFilters: FinancialEventFilters) => void;
  onClearFilters: () => void;
  onOpenAdvancedFilters: () => void;
}

export function TransactionToolbar({
  searchTerm,
  filters,
  onSearchChange,
  onFilterChange,
  onClearFilters,
  onOpenAdvancedFilters,
}: TransactionToolbarProps) {
  // Conta quantos filtros estão ativos no Drawer para mostrar no badge e controlar o botão Limpar
  const activeCount = Object.entries(filters).reduce((acc, [key, value]) => {
    if (["query", "page", "pageSize", "sort"].includes(key)) return acc;
    if (Array.isArray(value)) return acc + value.length;
    if (value !== undefined) return acc + 1;
    return acc;
  }, 0);

  const toggleFlow = (flow: FinancialEventFlow) => {
    const current = filters.flow || [];
    const newFlow = current.includes(flow)
      ? current.filter((f) => f !== flow)
      : [...current, flow];
    onFilterChange({ ...filters, flow: newFlow.length ? newFlow : undefined });
  };

  const toggleStatus = (status: FinancialEventStatus) => {
    const current = filters.status || [];
    const newStatus = current.includes(status)
      ? current.filter((s) => s !== status)
      : [...current, status];
    onFilterChange({
      ...filters,
      status: newStatus.length ? newStatus : undefined,
    });
  };

  return (
    <div className="flex flex-col sm:flex-row gap-4">
      <div className="relative flex-1">
        <Search
          size={18}
          className="absolute left-4 top-1/2 -translate-y-1/2 text-muted"
        />
        <input
          type="text"
          placeholder="Pesquisar lançamentos, lojas, categorias..."
          value={searchTerm || ""}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-full pl-11 pr-4 py-3 bg-surface border border-subtle/30 rounded-2xl text-sm font-semibold text-primary focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand transition-all shadow-sm"
        />
        {searchTerm && (
          <button
            onClick={() => onSearchChange("")}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-muted hover:text-primary cursor-pointer"
          >
            <X size={16} />
          </button>
        )}
      </div>

      <div className="flex items-center gap-2 overflow-x-auto scrollbar-none pb-2 sm:pb-0">
        <button
          onClick={() => toggleFlow("income")}
          className={`px-4 py-3 rounded-2xl text-sm font-bold border whitespace-nowrap transition-all cursor-pointer ${filters.flow?.includes("income") ? "bg-emerald-500/10 text-emerald-600 border-emerald-500/20" : "bg-surface border-subtle/30 text-secondary hover:bg-elevated"}`}
        >
          Receitas
        </button>
        <button
          onClick={() => toggleFlow("expense")}
          className={`px-4 py-3 rounded-2xl text-sm font-bold border whitespace-nowrap transition-all cursor-pointer ${filters.flow?.includes("expense") ? "bg-red-500/10 text-red-600 border-red-500/20" : "bg-surface border-subtle/30 text-secondary hover:bg-elevated"}`}
        >
          Despesas
        </button>
        <button
          onClick={() => toggleStatus("pending")}
          className={`px-4 py-3 rounded-2xl text-sm font-bold border whitespace-nowrap transition-all cursor-pointer ${filters.status?.includes("pending") ? "bg-amber-500/10 text-amber-600 border-amber-500/20" : "bg-surface border-subtle/30 text-secondary hover:bg-elevated"}`}
        >
          Pendentes
        </button>

        <div className="flex items-center ml-auto gap-1">
          {/* ✨ Botão "Limpar" aparece dinamicamente se houver filtros ativos */}
          {activeCount > 0 && (
            <button
              onClick={onClearFilters}
              className="px-3 py-3 text-sm font-bold text-muted hover:text-primary transition-all whitespace-nowrap cursor-pointer"
            >
              Limpar
            </button>
          )}

          <button
            onClick={onOpenAdvancedFilters}
            className="px-4 py-3 rounded-2xl text-sm font-bold bg-surface border border-subtle/30 text-secondary hover:bg-elevated hover:text-primary transition-all flex items-center gap-2 whitespace-nowrap shadow-sm cursor-pointer"
          >
            <SlidersHorizontal size={16} />
            Filtros
            {activeCount > 0 && (
              <span className="w-5 h-5 rounded-full bg-brand text-white flex items-center justify-center text-[10px] ml-1">
                {activeCount}
              </span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
