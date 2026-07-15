import { useState, useMemo } from "react";
import { TransactionToolbar } from "./components/TransactionToolbar";
import { TransactionList } from "./components/TransactionList";
import { NewTransactionModal } from "../../components/transactions/NewTransactionModal";
import { useTransactions } from "../../hooks/useTransactions";
import { useTransactionFilters } from "./hooks/useTransactionFilters";
import { mapToFinancialEvents } from "./utils/eventMapper"; // ✨ Tradutor de Dados

export function Transactions() {
  const { transactions, isLoading } = useTransactions();
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);

  // A URL comanda absolutamente tudo!
  const {
    searchQuery,
    searchInput,
    setSearchInput,
    activeFilters,
    toggleFilter,
    activeSort,
    setSort,
    clearAllFilters,
  } = useTransactionFilters();

  const isSearching = searchQuery.trim().length > 0;
  const isFiltering = activeFilters.length > 0;

  // SIMULAÇÃO LOCAL (Com o novo Polimorfismo)
  const filteredTransactions = useMemo(() => {
    if (!transactions) return [];

    // 1. Traduz os dados puros para Eventos Financeiros Avançados
    const financialEvents = mapToFinancialEvents(transactions);
    let filtered = [...financialEvents];

    // 2. Simulação da Pesquisa Global
    if (isSearching) {
      const term = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (ev) =>
          ev.title.toLowerCase().includes(term) ||
          (ev.category_name && ev.category_name.toLowerCase().includes(term)) ||
          (ev.account_name && ev.account_name.toLowerCase().includes(term)),
      );
    }

    // 3. Simulação dos Filtros Rápidos
    if (isFiltering) {
      if (
        activeFilters.includes("income") &&
        !activeFilters.includes("expense")
      ) {
        filtered = filtered.filter((ev) => ev.flow === "income");
      }
      if (
        activeFilters.includes("expense") &&
        !activeFilters.includes("income")
      ) {
        filtered = filtered.filter((ev) => ev.flow === "expense");
      }
      if (
        activeFilters.includes("pending") &&
        !activeFilters.includes("completed")
      ) {
        filtered = filtered.filter((ev) => ev.status === "pending");
      }
      if (
        activeFilters.includes("completed") &&
        !activeFilters.includes("pending")
      ) {
        filtered = filtered.filter((ev) => ev.status === "completed");
      }
    }

    // 4. Simulação da Ordenação
    filtered.sort((a, b) => {
      const dateA = new Date(a.date || 0).getTime();
      const dateB = new Date(b.date || 0).getTime();

      switch (activeSort) {
        case "date_asc":
          return dateA - dateB;
        case "amount_desc":
          return Math.abs(b.amount) - Math.abs(a.amount);
        case "amount_asc":
          return Math.abs(a.amount) - Math.abs(b.amount);
        case "name_asc":
          return a.title.localeCompare(b.title);
        case "date_desc":
        default:
          return dateB - dateA;
      }
    });

    return filtered;
  }, [
    transactions,
    searchQuery,
    isSearching,
    isFiltering,
    activeFilters,
    activeSort,
  ]);

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-10">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-6 sm:pt-0">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Caixa
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Acompanhe receitas, despesas e movimentações
            </p>
          </div>
        </div>

        <TransactionToolbar
          searchTerm={searchInput}
          activeFilters={activeFilters}
          activeSort={activeSort}
          onSearchChange={setSearchInput}
          onFilterToggle={toggleFilter}
          onSortChange={setSort}
          onNewTransaction={() => setIsNewModalOpen(true)}
        />

        <TransactionList
          transactions={filteredTransactions}
          isLoading={isLoading}
          isSearching={isSearching}
          isFiltering={isFiltering}
          searchTerm={searchQuery}
          onClearFilters={clearAllFilters}
          onNewTransaction={() => setIsNewModalOpen(true)}
        />
      </div>

      <NewTransactionModal
        isOpen={isNewModalOpen}
        onClose={() => setIsNewModalOpen(false)}
      />
    </>
  );
}
