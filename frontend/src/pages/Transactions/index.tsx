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

export function Transactions() {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<FinancialEventDTO | null>(
    null,
  );

  const queryClient = useQueryClient(); // ✨ Instanciamos o gestor de cache do React Query

  const { filters, setFilters, searchInput, setSearchInput, clearAllFilters } =
    useTransactionFilters();

  const { data, isLoading, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useFinancialEvents(filters);

  const events: FinancialEventDTO[] = useMemo(() => {
    if (!data?.pages) return [];
    return data.pages.flatMap((page) => page.items || []);
  }, [data]);

  // ✨ FUNÇÃO REAL DE "DAR BAIXA"
  const handleMarkAsPaid = async (eventId: string) => {
    try {
      // 1. Chama a nova rota do backend que acabámos de criar
      await api.patch(`/financial-events/${eventId}/pay`);

      // 2. Invalida o cache para que a Timeline atualize instantaneamente com o novo status
      await queryClient.invalidateQueries({ queryKey: ["financial-events"] });

      // 3. Fecha o painel da Matrioska
      setSelectedEvent(null);
    } catch (error) {
      console.error("Erro ao dar baixa no lançamento:", error);
      alert("Ocorreu um erro ao dar baixa no pagamento. Tente novamente.");
    }
  };

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-10">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pt-6 sm:pt-0">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Central Financeira
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Consulte e pesquise toda a sua vida financeira
            </p>
          </div>

          <button
            onClick={() => setIsNewModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-3 rounded-2xl text-sm font-bold shadow-sm transition-all cursor-pointer"
          >
            <Plus size={18} /> Novo Lançamento
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
        onMarkAsPaid={handleMarkAsPaid} // ✨ Injetado com sucesso!
      />

      <NewTransactionModal
        isOpen={isNewModalOpen}
        onClose={() => setIsNewModalOpen(false)}
      />
    </>
  );
}
