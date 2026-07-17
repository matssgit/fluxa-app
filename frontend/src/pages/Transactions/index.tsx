import { useState, useMemo } from "react";
import { Plus } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "../../api/client";
import { TransactionToolbar } from "./components/TransactionToolbar";
import { TransactionList } from "./components/TransactionList";
import { NewTransactionModal } from "../../components/transactions/NewTransactionModal";
import { AdvancedFiltersDrawer } from "./components/AdvancedFiltersDrawer";
import { FinancialEventPanel } from "./components/FinancialEventPanel";
import { useTransactionFilters } from "./hooks/useTransactionFilters";
import { useFinancialEvents } from "./hooks/useFinancialEvents";
import type { FinancialEventDTO } from "./types";

// ✨ NOVO: Importamos o modal específico de assinaturas já existente
import { PaySubscriptionModal } from "../../components/subscriptions/PaySubscriptionModal";

export function Transactions() {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<FinancialEventDTO | null>(
    null,
  );

  // ✨ NOVO: Estado para controlar a abertura do modal de pagamento de assinatura
  const [isPaySubModalOpen, setIsPaySubModalOpen] = useState(false);

  const queryClient = useQueryClient();

  const { filters, setFilters, searchInput, setSearchInput, clearAllFilters } =
    useTransactionFilters();

  const { data, isLoading, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useFinancialEvents(filters);

  const events: FinancialEventDTO[] = useMemo(() => {
    if (!data?.pages) return [];
    return data.pages.flatMap((page) => page.items || []);
  }, [data]);

  const handleMarkAsPaid = async (eventId: string) => {
    // ✨ NOVO: INTERCEPTAÇÃO DA REGRA DE NEGÓCIO
    // Se for uma assinatura, abrimos o modal que exige a conta (account_id) e paramos a execução aqui.
    if (selectedEvent?.type === "subscription") {
      setIsPaySubModalOpen(true);
      return;
    }

    // Fluxo genérico (original) mantido intacto para Transações e Parcelas
    try {
      await api.patch(`/financial-events/${eventId}/pay`);
      await queryClient.invalidateQueries({ queryKey: ["financial-events"] });
      setSelectedEvent(null);
    } catch (error) {
      console.error("Erro ao dar baixa no lançamento:", error);
      alert("Ocorreu um erro ao dar baixa no pagamento. Tente novamente.");
    }
  };

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-10">
        {/* 🚀 REGRA UX #04: HIERARQUIA DE CONTEXTO */}
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
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all cursor-pointer active:scale-95"
          >
            <Plus size={18} />
            <span>Novo Lançamento</span>
          </button>
        </div>

        <TransactionToolbar
          searchTerm={searchInput}
          filters={filters}
          onSearchChange={setSearchInput}
          onFilterChange={setFilters}
          onClearFilters={clearAllFilters}
          onOpenAdvancedFilters={() => setIsDrawerOpen(true)}
        />

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

      {/* ✨ NOVO: O Modal de Pagamento de Assinaturas plugado na página */}
      <PaySubscriptionModal
        isOpen={isPaySubModalOpen}
        onClose={() => {
          setIsPaySubModalOpen(false);
          setSelectedEvent(null); // Fecha o painel lateral simultaneamente
          // Como o hook da assinatura atualizou o saldo, garantimos que a query geral do Caixa recarregue para espelhar a baixa:
          queryClient.invalidateQueries({ queryKey: ["financial-events"] });
        }}
        subscriptionId={selectedEvent?.id || null}
      />
    </>
  );
}
