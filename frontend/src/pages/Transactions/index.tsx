import { useState } from "react";
import { Plus, TrendingUp, TrendingDown, Wallet } from "lucide-react";
import { useTransactions } from "../../hooks/useTransactions";
import { TransactionTable } from "../../components/transactions/TransactionTable";
import { NewTransactionModal } from "../../components/transactions/NewTransactionModal";

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

  const typedSummary = summary as SummaryData;

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  const filteredTransactions = transactions.filter((tx) => {
    if (filterStatus === "pending") return tx.status === "pending";
    if (filterStatus === "completed") return tx.status === "completed";
    return true;
  });

  return (
    /* ✨ PADRONIZAÇÃO DE LARGURA: max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 garante alinhamento com a tela de Cartões! */
    <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 pb-28 sm:pb-16 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
            Fluxo de Caixa
          </h2>
          <p className="text-muted text-sm font-medium mt-0.5">
            Operações diárias e conciliação
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

      {/* ✨ FILTROS CENTRALIZADOS NO MOBILE: justify-center no celular, justify-start no desktop! */}
      <div className="flex justify-center sm:justify-start border-b border-subtle/20 pb-4">
        <div className="flex gap-1.5 p-1 bg-elevated/60 rounded-2xl border border-subtle/20 w-full sm:w-auto justify-center">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterStatus(tab.id)}
              className={`flex-1 sm:flex-none px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                filterStatus === tab.id
                  ? "bg-surface text-primary shadow-2xs border border-subtle/30"
                  : "text-muted hover:text-secondary"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
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
  );
}
