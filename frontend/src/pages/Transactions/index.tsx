import { useState } from "react";
import { Plus, TrendingUp, TrendingDown, Wallet } from "lucide-react";
import { useTransactions } from "../../hooks/useTransactions";
import { TransactionTable } from "../../components/TransactionTable";
import { NewTransactionModal } from "../../components/NewTransactionModal";

export function Transactions() {
  const { transactions, summary, isLoading } = useTransactions();

  // Estados locais de controle visual
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [filterStatus, setFilterStatus] = useState<
    "all" | "pending" | "completed"
  >("all");

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  // Filtragem local instantânea em memória
  const filteredTransactions = transactions.filter((tx) => {
    if (filterStatus === "pending") return tx.status === "pending";
    if (filterStatus === "completed") return tx.status === "completed";
    return true;
  });

  return (
    <div className="space-y-8 animate-fade-in">
      {/* 1. CABEÇALHO DA PÁGINA */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
            Fluxo de Caixa
          </h2>
          <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
            Monitore suas entradas, saídas operacionais e conciliações pendentes
          </p>
        </div>

        <button
          onClick={() => setIsNewModalOpen(true)}
          className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
        >
          <Plus size={18} />
          <span>Novo Lançamento</span>
        </button>
      </div>

      {/* 2. RESUMO OPERACIONAL (Consumindo 100% da API V2 sem cálculos no front) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Entradas */}
        <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-emerald-500">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0 shadow-2xs">
            <TrendingUp size={24} />
          </div>
          <div>
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
              Receitas no Período
            </span>
            <span className="text-lg sm:text-xl font-extrabold text-primary tracking-tight">
              {formatCurrency(summary.income || 0)}
            </span>
          </div>
        </div>

        {/* Saídas */}
        <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-red-500">
          <div className="w-12 h-12 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center shrink-0 shadow-2xs">
            <TrendingDown size={24} />
          </div>
          <div>
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
              Despesas no Período
            </span>
            <span className="text-lg sm:text-xl font-extrabold text-primary tracking-tight">
              {formatCurrency(summary.expense || 0)}
            </span>
          </div>
        </div>

        {/* Saldo Líquido */}
        <div className="card-default p-5 flex items-center gap-4 border-l-4 border-l-brand">
          <div className="w-12 h-12 rounded-2xl bg-brand/10 text-brand flex items-center justify-center shrink-0 shadow-2xs">
            <Wallet size={24} />
          </div>
          <div>
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
              Saldo Operacional
            </span>
            <span
              className={`text-lg sm:text-xl font-extrabold tracking-tight ${
                (summary.amount || 0) >= 0 ? "text-primary" : "text-red-500"
              }`}
            >
              {formatCurrency(summary.amount || 0)}
            </span>
          </div>
        </div>
      </div>

      {/* 3. ABAS DE FILTRAGEM RÁPIDA */}
      <div className="flex items-center justify-between border-b border-subtle/20 pb-4">
        <div className="flex gap-2 p-1 bg-elevated/60 rounded-xl border border-subtle/20">
          {(
            [
              { id: "all", label: "Todos os Lançamentos" },
              { id: "pending", label: "⏳ Pendentes" },
              { id: "completed", label: "✔ Concluídos" },
            ] as const
          ).map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterStatus(tab.id)}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all duration-200 cursor-pointer ${
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

      {/* 4. TABELA E MODAL OFICIAIS */}
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
