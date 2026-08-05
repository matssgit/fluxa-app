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
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-8">
      {/* Entradas */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-emerald-500">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0 shadow-2xs">
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
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-red-500">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center shrink-0 shadow-2xs">
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
            className={`text-base sm:text-lg font-extrabold ${summary.amount >= 0 ? "text-primary" : "text-red-500"}`}
          >
            {formatCurrency(summary.amount ?? 0)}
          </span>
        </div>
      </div>

      {/* Projeção */}
      <div className="card-default p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center gap-3 sm:gap-4 border-l-4 border-l-blue-500 bg-linear-to-br from-surface to-blue-500/5">
        <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-blue-500/10 text-blue-500 flex items-center justify-center shrink-0 shadow-2xs">
          <Target size={20} className="sm:w-6 sm:h-6" />
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase text-muted block">
            Projeção
          </span>
          <span
            className={`text-base sm:text-lg font-extrabold ${
              (summary.projection ?? 0) >= 0 ? "text-primary" : "text-red-500"
            }`}
          >
            {formatCurrency(summary.projection ?? 0)}
          </span>
        </div>
      </div>
    </div>
  );
}
