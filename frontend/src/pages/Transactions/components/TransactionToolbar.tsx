import { useState, useRef, useEffect } from "react";
import { Search, ChevronDown, Plus, Check } from "lucide-react";
import { QUICK_FILTERS, SORT_OPTIONS } from "../constants";

interface TransactionToolbarProps {
  searchTerm: string;
  activeFilters: string[];
  activeSort: string;
  onSearchChange: (value: string) => void;
  onFilterToggle: (filterId: string) => void;
  onSortChange: (sortId: string) => void;
  onNewTransaction: () => void;
}

export function TransactionToolbar({
  searchTerm,
  activeFilters,
  activeSort,
  onSearchChange,
  onFilterToggle,
  onSortChange,
  onNewTransaction,
}: TransactionToolbarProps) {
  // Estado puramente visual (abrir/fechar menu). O estado do valor selecionado vem das props!
  const [isSortOpen, setIsSortOpen] = useState(false);
  const sortRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (sortRef.current && !sortRef.current.contains(event.target as Node)) {
        setIsSortOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const currentSortLabel =
    SORT_OPTIONS.find((opt) => opt.id === activeSort)?.label || "Ordenar";

  return (
    <div className="flex flex-col gap-4 mb-6">
      {/* 
        LINHA 1 (Desktop) / BLOCO 1 (Mobile): 
        Pesquisa | Ordenação | Novo Lançamento 
      */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
        {/* Input de Pesquisa (Ocupa o espaço máximo possível) */}
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
            <Search size={18} className="text-muted" />
          </div>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Pesquisar lançamentos..."
            className="w-full pl-11 pr-4 py-2.5 rounded-xl bg-surface border border-subtle/30 text-primary placeholder:text-muted/60 focus:outline-none focus:border-brand/50 focus:ring-1 focus:ring-brand/50 transition-all shadow-sm text-sm"
          />
        </div>

        {/* Grupo de Ações */}
        <div className="flex items-center gap-3">
          {/* Dropdown de Ordenação */}
          <div className="relative flex-1 md:flex-none" ref={sortRef}>
            <button
              onClick={() => setIsSortOpen(!isSortOpen)}
              className="w-full flex items-center justify-between gap-2 px-4 py-2.5 rounded-xl bg-surface border border-subtle/30 text-secondary hover:text-primary transition-colors shadow-sm text-sm font-semibold cursor-pointer"
            >
              <span className="truncate">{currentSortLabel}</span>
              <ChevronDown
                size={16}
                className={`text-muted transition-transform duration-200 ${
                  isSortOpen ? "rotate-180" : ""
                }`}
              />
            </button>

            {isSortOpen && (
              <div className="absolute right-0 top-full mt-2 w-56 bg-surface/95 backdrop-blur-xl border border-subtle/30 rounded-2xl shadow-xl z-20 py-2 animate-fade-in divide-y divide-subtle/10">
                {SORT_OPTIONS.map((option) => (
                  <button
                    key={option.id}
                    onClick={() => {
                      onSortChange(option.id);
                      setIsSortOpen(false);
                    }}
                    className="w-full flex items-center justify-between px-4 py-2.5 text-sm font-semibold text-secondary hover:bg-elevated hover:text-primary transition-colors text-left cursor-pointer"
                  >
                    <span>{option.label}</span>
                    {activeSort === option.id && (
                      <Check size={16} className="text-brand" />
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Botão Principal */}
          <button
            onClick={onNewTransaction}
            className="flex-1 md:flex-none flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all whitespace-nowrap cursor-pointer"
          >
            <Plus size={18} />
            <span className="hidden sm:inline">Novo Lançamento</span>
            <span className="sm:hidden">Novo</span>
          </button>
        </div>
      </div>

      {/* 
        LINHA 2: Chips Dinâmicos Combináveis 
      */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 sm:pb-0 snap-x scrollbar-none">
        {QUICK_FILTERS.map((filter) => {
          const isActive = activeFilters.includes(filter.id);
          return (
            <button
              key={filter.id}
              onClick={() => onFilterToggle(filter.id)}
              className={`snap-start shrink-0 px-4 py-1.5 rounded-full text-xs font-bold transition-all border cursor-pointer ${
                isActive
                  ? "bg-brand/10 border-brand text-brand shadow-xs"
                  : "bg-surface border-subtle/30 text-secondary hover:bg-elevated hover:border-subtle/50"
              }`}
            >
              {filter.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
