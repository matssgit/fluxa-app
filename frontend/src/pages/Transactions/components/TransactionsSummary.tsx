import { TrendingUp, TrendingDown, Wallet, Target } from "lucide-react";

export interface SummaryData {
  income: number;
  expense: number;
  amount: number;
  projection?: number;
}

interface TransactionsSummaryProps {
  summary: SummaryData;
}

export function TransactionsSummary({ summary }: TransactionsSummaryProps) {
  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-8">
      {/* Entradas */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-income">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-income-soft text-income flex items-center justify-center shrink-0">
          <TrendingUp size={20} className="sm:w-6 sm:h-6" />
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase text-muted block">
            Entradas
          </span>
          <span className="text-base sm:text-lg font-extrabold text-primary">
            {formatCurrency(summary.income ?? 0)}
          </span>
        </div>
      </div>

      {/* Saídas */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-expense">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-expense-soft text-expense flex items-center justify-center shrink-0">
          <TrendingDown size={20} className="sm:w-6 sm:h-6" />
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase text-muted block">
            Saídas
          </span>
          <span className="text-base sm:text-lg font-extrabold text-primary">
            {formatCurrency(summary.expense ?? 0)}
          </span>
        </div>
      </div>

      {/* Saldo Atual */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-brand">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-brand/10 text-brand flex items-center justify-center shrink-0 shadow-2xs">
          <Wallet size={20} className="sm:w-6 sm:h-6" />
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase text-muted block">
            Saldo Atual
          </span>
          <span
            className={`text-base sm:text-lg font-extrabold ${summary.amount >= 0 ? "text-primary" : "text-expense"}`}
          >
            {formatCurrency(summary.amount ?? 0)}
          </span>
        </div>
      </div>

      {/* Projeção */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-info bg-linear-to-br from-surface to-elevated/40">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-elevated text-info flex items-center justify-center shrink-0">
          <Target size={20} className="sm:w-6 sm:h-6" />
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase text-muted block">
            Projeção
          </span>
          <span
            className={`text-base sm:text-lg font-extrabold ${
              (summary.projection ?? 0) >= 0 ? "text-primary" : "text-expense"
            }`}
          >
            {formatCurrency(summary.projection ?? 0)}
          </span>
        </div>
      </div>
    </div>
  );
}
