import { CheckCircle2, Clock, Receipt, SearchX, FilterX } from "lucide-react";
import { PrivacyMask } from "../../../components/ui/PrivacyMask";
import type { FinancialEvent } from "../types";

interface TransactionListProps {
  transactions: FinancialEvent[];
  isLoading: boolean;
  isSearching?: boolean;
  isFiltering?: boolean;
  searchTerm?: string;
  onClearFilters?: () => void;
  onNewTransaction?: () => void;
}

export function TransactionList({
  transactions,
  isLoading,
  isSearching,
  isFiltering,
  searchTerm,
  onClearFilters,
  onNewTransaction,
}: TransactionListProps) {
  const formatDate = (dateString?: string) => {
    if (!dateString) return "Sem data";
    const date = new Date(dateString);
    return new Intl.DateTimeFormat("pt-BR", {
      day: "2-digit",
      month: "short",
    }).format(date);
  };

  if (isLoading) {
    return (
      <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm overflow-hidden flex flex-col">
        <div className="hidden md:grid grid-cols-12 gap-4 px-6 py-4 border-b border-subtle/20 bg-elevated/20">
          <div className="col-span-5 h-4 bg-subtle/20 rounded animate-pulse" />
          <div className="col-span-2 h-4 bg-subtle/20 rounded animate-pulse" />
          <div className="col-span-2 h-4 bg-subtle/20 rounded animate-pulse" />
          <div className="col-span-3 h-4 bg-subtle/20 rounded animate-pulse" />
        </div>
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="flex flex-col md:grid md:grid-cols-12 gap-3 md:gap-4 px-6 py-4 border-b border-subtle/10 last:border-0"
          >
            <div className="col-span-5 flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-subtle/20 animate-pulse shrink-0 md:hidden" />
              <div className="space-y-2 w-full">
                <div className="h-4 bg-subtle/20 rounded w-2/3 animate-pulse" />
                <div className="h-3 bg-subtle/20 rounded w-1/3 animate-pulse md:hidden" />
              </div>
            </div>
            <div className="col-span-2 hidden md:flex items-center">
              <div className="h-4 bg-subtle/20 rounded w-20 animate-pulse" />
            </div>
            <div className="col-span-2 hidden md:flex items-center">
              <div className="h-4 bg-subtle/20 rounded w-16 animate-pulse" />
            </div>
            <div className="col-span-3 flex items-center md:justify-end">
              <div className="h-5 bg-subtle/20 rounded w-24 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    if (isSearching) {
      return (
        <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm p-12 flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 bg-elevated rounded-2xl flex items-center justify-center text-muted mb-4 shadow-sm border border-subtle/20">
            <SearchX size={28} />
          </div>
          <h3 className="text-base font-bold text-primary mb-1">
            Nenhum lançamento encontrado
          </h3>
          <p className="text-sm text-muted max-w-sm mb-6">
            Não encontrámos resultados para "
            <span className="font-semibold text-primary">{searchTerm}</span>".
            Tente usar termos diferentes.
          </p>
          <button
            onClick={onClearFilters}
            className="text-sm font-bold text-brand hover:text-brand-light transition-colors cursor-pointer"
          >
            Limpar busca
          </button>
        </div>
      );
    }

    if (isFiltering) {
      return (
        <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm p-12 flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 bg-elevated rounded-2xl flex items-center justify-center text-muted mb-4 shadow-sm border border-subtle/20">
            <FilterX size={28} />
          </div>
          <h3 className="text-base font-bold text-primary mb-1">
            Nenhum lançamento corresponde aos filtros
          </h3>
          <p className="text-sm text-muted max-w-sm mb-6">
            A sua combinação de filtros não retornou nenhum resultado.
          </p>
          <button
            onClick={onClearFilters}
            className="text-sm font-bold text-brand hover:text-brand-light transition-colors cursor-pointer"
          >
            Limpar filtros
          </button>
        </div>
      );
    }

    return (
      <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm p-12 flex flex-col items-center justify-center text-center">
        <div className="w-16 h-16 bg-elevated rounded-2xl flex items-center justify-center text-muted mb-4 shadow-sm border border-subtle/20">
          <Receipt size={28} />
        </div>
        <h3 className="text-base font-bold text-primary mb-1">
          Nenhum lançamento cadastrado
        </h3>
        <p className="text-sm text-muted max-w-sm mb-6">
          Comece a organizar a sua vida financeira registrando a sua primeira
          receita ou despesa.
        </p>
        <button
          onClick={onNewTransaction}
          className="flex items-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all cursor-pointer"
        >
          Criar primeiro lançamento
        </button>
      </div>
    );
  }

  return (
    <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm overflow-hidden flex flex-col">
      <div className="hidden md:grid grid-cols-12 gap-4 px-6 py-3 border-b border-subtle/20 bg-elevated/20 text-[10px] font-extrabold uppercase tracking-wider text-muted">
        <div className="col-span-5">Descrição</div>
        <div className="col-span-2">Categoria</div>
        <div className="col-span-2">Conta</div>
        <div className="col-span-3 text-right">Valor</div>
      </div>

      <div className="flex flex-col">
        {transactions.map((event) => (
          <div
            key={event.id}
            className="group flex flex-col md:grid md:grid-cols-12 gap-3 md:gap-4 px-5 md:px-6 py-4 border-b border-subtle/10 hover:bg-elevated/40 transition-colors items-start md:items-center last:border-0 cursor-pointer"
          >
            {/* Bloco 1: Mobile Top / Desktop Título */}
            <div className="w-full md:w-auto col-span-5 flex justify-between md:justify-start items-start md:items-center min-w-0 md:pr-4">
              <div className="flex flex-col min-w-0">
                <span className="text-sm font-bold text-primary truncate">
                  {event.title}
                </span>
                <span className="hidden md:block text-[11px] font-medium text-muted mt-0.5">
                  {formatDate(event.date)}
                </span>
              </div>
              <div className="md:hidden flex items-center gap-2 shrink-0 ml-4">
                <span
                  className={`text-sm font-bold ${event.flow === "income" ? "text-emerald-500" : "text-red-500"}`}
                >
                  <PrivacyMask amount={event.amount} />
                </span>
              </div>
            </div>

            {/* Bloco 2: Mobile Bottom Info */}
            <div className="w-full md:hidden flex justify-between items-end mt-1">
              <div className="flex flex-col min-w-0">
                <span className="text-[11px] font-semibold text-secondary truncate">
                  {event.category_name || "Sem categoria"}
                </span>
                <span className="text-[10px] font-medium text-muted truncate">
                  {event.account_name || "Sem conta"}
                </span>
              </div>
              <div className="flex items-center gap-1.5 shrink-0">
                <span className="text-[11px] font-medium text-muted">
                  {formatDate(event.date)}
                </span>
                {event.status === "pending" ? (
                  <div title="Pendente">
                    <Clock size={12} className="text-amber-500" />
                  </div>
                ) : (
                  <div title="Concluído">
                    <CheckCircle2 size={12} className="text-emerald-500" />
                  </div>
                )}
              </div>
            </div>

            {/* Bloco 3: Colunas Desktop */}
            <div className="hidden md:flex col-span-2 items-center min-w-0 pr-2">
              <span className="px-2.5 py-1 rounded-md bg-elevated text-[11px] font-semibold text-secondary truncate border border-subtle/20 group-hover:bg-surface transition-colors">
                {event.category_name || "Geral"}
              </span>
            </div>

            <div className="hidden md:flex col-span-2 items-center min-w-0 pr-2">
              <span className="text-xs font-medium text-secondary truncate">
                {event.account_name || "Geral"}
              </span>
            </div>

            <div className="hidden md:flex col-span-3 items-center justify-end gap-3">
              {event.status === "pending" ? (
                <div title="Pendente">
                  <Clock size={14} className="text-amber-500" />
                </div>
              ) : (
                <div title="Concluído">
                  <CheckCircle2 size={14} className="text-emerald-500" />
                </div>
              )}
              <span
                className={`text-sm font-bold ${event.flow === "income" ? "text-emerald-500" : "text-red-500"}`}
              >
                <PrivacyMask amount={event.amount} />
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
