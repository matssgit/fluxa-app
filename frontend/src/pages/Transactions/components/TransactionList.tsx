import {
  CheckCircle2,
  Clock,
  Receipt,
  SearchX,
  FilterX,
  Repeat,
  CreditCard,
  Store,
} from "lucide-react";
import { PrivacyMask } from "../../../components/ui/PrivacyMask";
import type { FinancialEventDTO } from "../types";

interface TransactionListProps {
  transactions: FinancialEventDTO[];
  isLoading: boolean;
  isSearching?: boolean;
  isFiltering?: boolean;
  searchTerm?: string;
  onClearFilters?: () => void;
  onNewTransaction?: () => void;
  onEventClick: (event: FinancialEventDTO) => void;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  fetchNextPage?: () => unknown; // ✨ Correção: Permite que o React Query passe a sua Promise sem erro de tipagem
}

export function TransactionList({
  transactions,
  isLoading,
  isSearching,
  isFiltering,
  searchTerm,
  onClearFilters,
  onNewTransaction,
  onEventClick,
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
}: TransactionListProps) {
  if (isLoading) {
    return (
      <div className="w-full bg-surface rounded-3xl border border-subtle/30 shadow-sm overflow-hidden flex flex-col p-4 space-y-4">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 p-4 border border-subtle/10 rounded-2xl"
          >
            <div className="w-12 h-12 rounded-xl bg-subtle/20 animate-pulse shrink-0" />
            <div className="space-y-2 w-full">
              <div className="h-4 bg-subtle/20 rounded w-1/3 animate-pulse" />
              <div className="h-3 bg-subtle/20 rounded w-1/4 animate-pulse" />
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
          <button
            onClick={onClearFilters}
            className="text-sm font-bold text-brand hover:text-brand-light transition-colors cursor-pointer mt-4"
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
        <button
          onClick={onNewTransaction}
          className="flex items-center gap-2 bg-brand text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all cursor-pointer mt-6"
        >
          Criar primeiro lançamento
        </button>
      </div>
    );
  }

  // ✨ Correção: Tratamento ultra-seguro de parsing de data para evitar quebras do JS e queixas do Linter
  const groupedEvents = transactions.reduce(
    (acc, event) => {
      let dateKey = "Sem Data";
      try {
        if (event.date) {
          dateKey = new Date(event.date).toISOString().split("T")[0];
        }
      } catch {
        dateKey = "1970-01-01"; // Fallback de segurança silencioso
      }

      if (!acc[dateKey]) acc[dateKey] = [];
      acc[dateKey].push(event);
      return acc;
    },
    {} as Record<string, FinancialEventDTO[]>,
  );

  const sortedDates = Object.keys(groupedEvents).sort(
    (a, b) => new Date(b).getTime() - new Date(a).getTime(),
  );

  const getRelativeDayName = (dateStr: string) => {
    if (dateStr === "Sem Data" || dateStr === "1970-01-01") return "SEM DATA";

    const target = new Date(dateStr);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    const isSameDay = (d1: Date, d2: Date) =>
      d1.getFullYear() === d2.getFullYear() &&
      d1.getMonth() === d2.getMonth() &&
      d1.getDate() === d2.getDate();

    if (isSameDay(target, today)) return "HOJE";
    if (isSameDay(target, yesterday)) return "ONTEM";

    return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "long" })
      .format(target)
      .toUpperCase();
  };

  return (
    <div className="w-full flex flex-col gap-6">
      {sortedDates.map((dateKey) => (
        <div key={dateKey} className="flex flex-col gap-3">
          <h3 className="text-xs font-extrabold tracking-widest text-muted uppercase pl-2">
            {getRelativeDayName(dateKey)}
          </h3>

          <div className="bg-surface rounded-3xl border border-subtle/30 shadow-sm overflow-hidden flex flex-col divide-y divide-subtle/10">
            {groupedEvents[dateKey].map((event) => {
              let Icon = Store;
              if (event.type === "installment") Icon = CreditCard;
              if (event.type === "subscription") Icon = Repeat;

              return (
                <div
                  key={event.id}
                  onClick={() => onEventClick(event)}
                  className="group flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 hover:bg-elevated/40 transition-colors cursor-pointer"
                >
                  <div className="flex items-start sm:items-center gap-4 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-elevated flex items-center justify-center text-muted border border-subtle/20 shrink-0 group-hover:bg-surface transition-colors">
                      <Icon size={18} />
                    </div>

                    <div className="flex flex-col min-w-0 gap-0.5">
                      <span className="text-sm font-bold text-primary truncate">
                        {event.title}
                      </span>

                      <div className="flex items-center gap-2 text-[11px] font-semibold text-secondary">
                        {event.type === "transaction" && (
                          <span>
                            {event.account || "Geral"} •{" "}
                            {event.category || "Sem Categoria"}
                          </span>
                        )}

                        {event.type === "subscription" && (
                          <span className="flex items-center gap-1.5">
                            💳 {event.account || "Geral"} • Próxima:{" "}
                            {new Intl.DateTimeFormat("pt-BR", {
                              day: "2-digit",
                              month: "2-digit",
                            }).format(
                              new Date(
                                event.context?.nextBillingDate || event.date,
                              ),
                            )}
                          </span>
                        )}

                        {event.type === "installment" && (
                          <span className="flex items-center gap-1.5">
                            💳 {event.context?.cardName || event.account} •
                            Parcela {event.context?.installmentNumber}/
                            {event.context?.totalInstallments}
                          </span>
                        )}
                      </div>

                      {event.type === "installment" &&
                        event.context?.installmentNumber &&
                        event.context?.totalInstallments && (
                          <div className="mt-2 flex items-center gap-2">
                            <div className="w-24 h-1.5 bg-subtle/20 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-brand rounded-full transition-all"
                                style={{
                                  width: `${(event.context.installmentNumber / event.context.totalInstallments) * 100}%`,
                                }}
                              />
                            </div>
                            <span className="text-[10px] font-bold text-muted">
                              {Math.round(
                                (event.context.installmentNumber /
                                  event.context.totalInstallments) *
                                  100,
                              )}
                              % Pago
                            </span>
                          </div>
                        )}
                    </div>
                  </div>

                  <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center shrink-0 w-full sm:w-auto mt-2 sm:mt-0 pl-14 sm:pl-0">
                    <span
                      className={`text-sm font-bold ${event.flow === "income" ? "text-emerald-500" : "text-red-500"}`}
                    >
                      <PrivacyMask amount={event.amount} />
                    </span>

                    <div className="flex items-center gap-1 mt-0.5">
                      {event.status === "pending" ? (
                        <>
                          <Clock size={12} className="text-amber-500" />
                          <span className="text-[10px] font-bold text-amber-500 uppercase tracking-wider">
                            Pendente
                          </span>
                        </>
                      ) : (
                        <>
                          <CheckCircle2
                            size={12}
                            className="text-emerald-500"
                          />
                          <span className="text-[10px] font-bold text-emerald-500 uppercase tracking-wider">
                            Concluído
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ))}

      {hasNextPage && (
        <div className="flex justify-center pt-4 pb-8">
          <button
            onClick={() => fetchNextPage?.()}
            disabled={isFetchingNextPage}
            className="px-6 py-3 rounded-xl border border-subtle/30 bg-surface text-sm font-bold text-secondary hover:text-primary hover:bg-elevated transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 shadow-sm"
          >
            {isFetchingNextPage ? (
              <>
                <div className="w-4 h-4 border-2 border-brand border-t-transparent rounded-full animate-spin" />
                Carregando histórico...
              </>
            ) : (
              "Carregar mais lançamentos"
            )}
          </button>
        </div>
      )}
    </div>
  );
}
