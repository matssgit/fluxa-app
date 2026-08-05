import { Plus } from "lucide-react";
import { api } from "../../api/client";
import { useState, useMemo } from "react";
import type { FinancialEventDTO } from "./types";
import { TransactionsTour } from "./TransactionsTour";
import { useQueryClient } from "@tanstack/react-query";
import { useDashboard } from "../../hooks/useDashboard";
import { TransactionList } from "./components/TransactionList";
import { useFinancialEvents } from "./hooks/useFinancialEvents";
import { TransactionToolbar } from "./components/TransactionToolbar";
import { useTransactionFilters } from "./hooks/useTransactionFilters";
import { FinancialEventPanel } from "./components/FinancialEventPanel";
import { TransactionsSummary } from "./components/TransactionsSummary";
import { AdvancedFiltersDrawer } from "./components/AdvancedFiltersDrawer";
import { NewTransactionModal } from "../../components/transactions/NewTransactionModal";
import { PaySubscriptionModal } from "../../components/features/subscriptions/PaySubscriptionModal";

export function Transactions() {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<FinancialEventDTO | null>(
    null,
  );
  const [isPaySubModalOpen, setIsPaySubModalOpen] = useState(false);

  const queryClient = useQueryClient();

  const { filters, setFilters, searchInput, setSearchInput, clearAllFilters } =
    useTransactionFilters();

  const { data, isLoading, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useFinancialEvents(filters);

  const { data: dashboardData } = useDashboard();

  const events: FinancialEventDTO[] = useMemo(() => {
    if (!data?.pages) return [];
    return data.pages.flatMap((page) => page.items || []);
  }, [data]);

  const handleMarkAsPaid = async (eventId: string) => {
    if (selectedEvent?.type === "subscription") {
      setIsPaySubModalOpen(true);
      return;
    }

    try {
      await api.patch(`/financial-events/${eventId}/pay`);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["financial-events"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
      setSelectedEvent(null);
    } catch (error) {
      console.error("Erro ao dar baixa no lançamento:", error);
      alert("Ocorreu um erro ao dar baixa no pagamento. Tente novamente.");
    }
  };

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-10">
        {!isLoading && <TransactionsTour />}

        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Caixa Central
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Visualize todas as entradas e saídas em ordem cronológica.
            </p>
          </div>

          <button
            onClick={() => setIsNewModalOpen(true)}
            className="tour-transactions-add w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all cursor-pointer active:scale-95"
          >
            <Plus size={18} />
            <span>Novo Lançamento</span>
          </button>
        </div>

        {dashboardData && dashboardData.summary && dashboardData.projection && (
          <TransactionsSummary
            summary={{
              income: dashboardData.summary.totalIncome,
              expense: dashboardData.summary.totalExpenses,
              amount: dashboardData.summary.currentBalance,
              projection: dashboardData.projection.projectedBalance,
            }}
          />
        )}

        <div className="tour-transactions-filters">
          <TransactionToolbar
            searchTerm={searchInput}
            filters={filters}
            onSearchChange={setSearchInput}
            onFilterChange={setFilters}
            onClearFilters={clearAllFilters}
            onOpenAdvancedFilters={() => setIsDrawerOpen(true)}
          />
        </div>

        <div className="tour-transactions-list">
          <TransactionList
            transactions={events}
            isLoading={isLoading}
            isSearching={!!filters.query}
            isFiltering={Object.keys(filters).length > (filters.query ? 1 : 0)}
            searchTerm={filters.query}
            onClearFilters={clearAllFilters}
            onNewTransaction={() => setIsNewModalOpen(true)}
            onEventClick={setSelectedEvent}
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            fetchNextPage={() => fetchNextPage()}
          />
        </div>
      </div>

      <AdvancedFiltersDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        filters={filters}
        onChange={setFilters}
        onClear={clearAllFilters}
      />

      <FinancialEventPanel
        event={selectedEvent}
        isOpen={!!selectedEvent}
        onClose={() => setSelectedEvent(null)}
        onEdit={() => setIsNewModalOpen(true)}
        onMarkAsPaid={handleMarkAsPaid}
      />

      <NewTransactionModal
        isOpen={isNewModalOpen}
        onClose={() => setIsNewModalOpen(false)}
      />

      <PaySubscriptionModal
        isOpen={isPaySubModalOpen}
        onClose={() => {
          setIsPaySubModalOpen(false);
          setSelectedEvent(null);
          Promise.all([
            queryClient.invalidateQueries({ queryKey: ["financial-events"] }),
            queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
          ]);
        }}
        subscriptionId={selectedEvent?.id || null}
      />
    </>
  );
}
