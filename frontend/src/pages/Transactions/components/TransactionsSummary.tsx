import { TrendingUp, TrendingDown, Wallet } from "lucide-react";

export interface SummaryData {
  income: number;
  expense: number;
  amount: number;
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
            {formatCurrency(summary.income ?? 0)}
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
            {formatCurrency(summary.expense ?? 0)}
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
            className={`text-lg font-extrabold ${summary.amount >= 0 ? "text-primary" : "text-red-500"}`}
          >
            {formatCurrency(summary.amount ?? 0)}
          </span>
        </div>
      </div>
    </div>
  );
}
