import { useState, useMemo } from "react";
import {
  Plus,
  TrendingUp,
  TrendingDown,
  Wallet,
  Search,
  SlidersHorizontal,
  X,
  ChevronDown,
  FilterX,
} from "lucide-react";
import { useTransactions } from "../../hooks/useTransactions";
import { TransactionTable } from "../../components/transactions/TransactionTable";
import { NewTransactionModal } from "../../components/transactions/NewTransactionModal";

// Infraestrutura UX Padrão (Pine & Sage)
import { PickerModal } from "../../components/ui/PickerModal";

interface SummaryData {
  income: number;
  expense: number;
  amount: number;
}

type FilterStatus = "all" | "pending" | "completed";

interface Tab {
  id: FilterStatus;
  label: string;
}

const TABS: Tab[] = [
  { id: "all", label: "Todos" },
  { id: "pending", label: "Pendentes" },
  { id: "completed", label: "Concluídos" },
];

export function Transactions() {
  const { transactions, summary, isLoading } = useTransactions();
  const [isNewModalOpen, setIsNewModalOpen] = useState<boolean>(false);
  const [filterStatus, setFilterStatus] = useState<FilterStatus>("all");

  const [searchTerm, setSearchTerm] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);

  const [selectedAccount, setSelectedAccount] = useState("all");
  const [selectedCategory, setSelectedCategory] = useState("all");

  const [isAccountPickerOpen, setIsAccountPickerOpen] = useState(false);
  const [isCategoryPickerOpen, setIsCategoryPickerOpen] = useState(false);

  const typedSummary = summary as SummaryData;

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  const uniqueAccounts = useMemo(() => {
    const accounts = new Set(
      transactions.map((t) => t.account_name).filter(Boolean),
    );
    return Array.from(accounts) as string[];
  }, [transactions]);

  const uniqueCategories = useMemo(() => {
    const categories = new Set(
      transactions.map((t) => t.category_name).filter(Boolean),
    );
    return Array.from(categories) as string[];
  }, [transactions]);

  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      if (filterStatus === "pending" && tx.status !== "pending") return false;
      if (filterStatus === "completed" && tx.status !== "completed")
        return false;
      if (selectedAccount !== "all" && tx.account_name !== selectedAccount)
        return false;
      if (selectedCategory !== "all" && tx.category_name !== selectedCategory)
        return false;

      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        const matchTitle = tx.title.toLowerCase().includes(term);
        const matchAcc = tx.account_name?.toLowerCase().includes(term);
        const matchCat = tx.category_name?.toLowerCase().includes(term);

        if (!matchTitle && !matchAcc && !matchCat) return false;
      }
      return true;
    });
  }, [
    transactions,
    filterStatus,
    selectedAccount,
    selectedCategory,
    searchTerm,
  ]);

  const isFiltering =
    searchTerm !== "" ||
    selectedAccount !== "all" ||
    selectedCategory !== "all" ||
    filterStatus !== "all";

  function handleResetFilters() {
    setSearchTerm("");
    setSelectedAccount("all");
    setSelectedCategory("all");
    setFilterStatus("all");
    setShowAdvanced(false);
  }

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
              Fluxo de Caixa
            </h2>
            <p className="text-muted text-sm font-medium mt-0.5">
              Operações diárias, pesquisas e conciliação
            </p>
          </div>
          <button
            onClick={() => setIsNewModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm transition-all cursor-pointer"
          >
            <Plus size={18} /> Novo Lançamento
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-emerald-500">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0 shadow-2xs">
              <TrendingUp size={24} />
            </div>
            <div>
              <span className="text-[10px] font-extrabold uppercase text-muted block">
                Receitas
              </span>
              <span className="text-lg font-extrabold text-primary">
                {formatCurrency(typedSummary.income ?? 0)}
              </span>
            </div>
          </div>
          <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-red-500">
            <div className="w-12 h-12 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center shrink-0 shadow-2xs">
              <TrendingDown size={24} />
            </div>
            <div>
              <span className="text-[10px] font-extrabold uppercase text-muted block">
                Despesas
              </span>
              <span className="text-lg font-extrabold text-primary">
                {formatCurrency(typedSummary.expense ?? 0)}
              </span>
            </div>
          </div>
          <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-brand">
            <div className="w-12 h-12 rounded-2xl bg-brand/10 text-brand flex items-center justify-center shrink-0 shadow-2xs">
              <Wallet size={24} />
            </div>
            <div>
              <span className="text-[10px] font-extrabold uppercase text-muted block">
                Saldo Operacional
              </span>
              <span
                className={`text-lg font-extrabold ${typedSummary.amount >= 0 ? "text-primary" : "text-red-500"}`}
              >
                {formatCurrency(typedSummary.amount ?? 0)}
              </span>
            </div>
          </div>
        </div>

        <div className="bg-surface border border-subtle/30 rounded-2xl p-4 shadow-2xs space-y-4">
          <div className="flex flex-col xl:flex-row gap-3">
            <div className="relative flex-1 group">
              <Search
                size={16}
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted group-focus-within:text-brand transition-colors"
              />
              <input
                type="text"
                placeholder="Buscar por descrição, conta ou categoria..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-subtle/30 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none text-xs sm:text-sm font-semibold transition-all shadow-3xs"
              />
              {searchTerm && (
                <button
                  onClick={() => setSearchTerm("")}
                  className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-muted hover:text-primary rounded-full hover:bg-elevated cursor-pointer transition-colors"
                  title="Limpar busca"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            {/* ✨ MAGIA AQUI: flex-wrap e distribuição inteligente garante que NENHUM ícone seja cortado no celular */}
            <div className="flex flex-wrap gap-2 items-center justify-between sm:justify-start w-full xl:w-auto">
              <div className="flex gap-1 p-1 bg-elevated/80 rounded-xl border border-subtle/20 flex-1 sm:flex-none justify-center">
                {TABS.map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setFilterStatus(tab.id)}
                    className={`px-3 sm:px-4 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer truncate ${
                      filterStatus === tab.id
                        ? "bg-surface text-primary shadow-2xs border border-subtle/30"
                        : "text-muted hover:text-secondary"
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => setShowAdvanced(!showAdvanced)}
                  className={`p-2 rounded-xl border transition-all cursor-pointer flex items-center justify-center h-8.5 w-8.5 ${
                    showAdvanced ||
                    selectedAccount !== "all" ||
                    selectedCategory !== "all"
                      ? "bg-brand/10 border-brand/30 text-brand font-bold shadow-xs"
                      : "bg-elevated border-subtle/30 text-secondary hover:bg-subtle/20"
                  }`}
                  title="Filtros Avançados"
                >
                  <SlidersHorizontal size={16} />
                </button>

                {isFiltering && (
                  <button
                    onClick={handleResetFilters}
                    className="p-2 rounded-xl border border-dashed border-red-500/30 bg-red-500/5 text-red-500 hover:bg-red-500/10 transition-all cursor-pointer flex items-center justify-center h-8.5 w-8.5"
                    title="Remover todos os filtros"
                  >
                    <FilterX size={16} />
                  </button>
                )}
              </div>
            </div>
          </div>

          {showAdvanced && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-subtle/20 animate-fade-in">
              <div>
                <label className="block text-[10px] font-extrabold uppercase tracking-wider text-muted mb-1.5 pl-1">
                  Filtrar por Conta
                </label>
                <button
                  onClick={() => setIsAccountPickerOpen(true)}
                  className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${selectedAccount === "all" ? "text-muted" : "text-primary"}`}
                >
                  <span className="truncate pr-2">
                    {selectedAccount === "all"
                      ? "Todas as contas"
                      : selectedAccount}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>

              <div>
                <label className="block text-[10px] font-extrabold uppercase tracking-wider text-muted mb-1.5 pl-1">
                  Filtrar por Categoria
                </label>
                <button
                  onClick={() => setIsCategoryPickerOpen(true)}
                  className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${selectedCategory === "all" ? "text-muted" : "text-primary"}`}
                >
                  <span className="truncate pr-2">
                    {selectedCategory === "all"
                      ? "Todas as categorias"
                      : selectedCategory}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>
          )}
        </div>

        <TransactionTable
          transactions={filteredTransactions}
          isLoading={isLoading}
        />

        <NewTransactionModal
          isOpen={isNewModalOpen}
          onClose={() => setIsNewModalOpen(false)}
        />
      </div>

      <PickerModal
        isOpen={isAccountPickerOpen}
        onClose={() => setIsAccountPickerOpen(false)}
        title="Filtrar por Conta"
        selectedValue={selectedAccount}
        onSelect={(val) => setSelectedAccount(val as string)}
        options={[
          { label: "Todas as contas", value: "all" },
          ...uniqueAccounts.map((acc) => ({ label: acc, value: acc })),
        ]}
      />

      <PickerModal
        isOpen={isCategoryPickerOpen}
        onClose={() => setIsCategoryPickerOpen(false)}
        title="Filtrar por Categoria"
        selectedValue={selectedCategory}
        onSelect={(val) => setSelectedCategory(val as string)}
        options={[
          { label: "Todas as categorias", value: "all" },
          ...uniqueCategories.map((cat) => ({ label: cat, value: cat })),
        ]}
      />
    </>
  );
}
