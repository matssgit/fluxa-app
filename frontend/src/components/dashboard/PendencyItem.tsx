import { formatCurrency } from "../../utils/formatters";
import { Calendar, ArrowRight, Check } from "lucide-react";

interface PendencyItemProps {
  title: string;
  amount: number;
  dueDate: string;
  onAction: () => void;
}

export function PendencyItem({
  title,
  amount,
  dueDate,
  onAction,
}: PendencyItemProps) {
  return (
    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-4 rounded-2xl bg-surface border border-subtle/30 hover:border-subtle/60 transition-all duration-200 shadow-2xs group">
      {/* Informações da Pendência */}
      <div className="space-y-1 w-full sm:w-auto min-w-0 flex-1 pr-0 sm:pr-3">
        <h4 className="text-sm font-extrabold text-primary truncate tracking-tight">
          {title}
        </h4>
        <div className="flex flex-wrap items-center gap-2 text-xs text-muted font-medium mt-1">
          <span className="flex items-center gap-1 shrink-0">
            <Calendar size={13} className="text-brand shrink-0" />
            {dueDate}
          </span>
          <span className="hidden xs:inline">•</span>
          <span className="font-bold text-primary shrink-0">
            {formatCurrency(amount)}
          </span>
        </div>
      </div>

      <button
        type="button"
        onClick={onAction}
        className="w-full sm:w-auto shrink-0 justify-center px-4 py-2.5 sm:py-2 rounded-xl bg-emerald-600/15 hover:bg-emerald-600 text-emerald-600 dark:text-emerald-600 hover:text-white border border-emerald-800/30 shadow-[0_0_15px_rgba(16,185,129,0.18)] animate-pulse hover:animate-none active:scale-95 transition-all duration-300 flex items-center gap-1.5 cursor-pointer mt-1 sm:mt-0 font-extrabold text-xs tracking-wide"
        title="Dar Baixa / Resolver Pendência"
      >
        <Check size={14} className="stroke-3 shrink-0" />
        <span>Resolver</span>
        <ArrowRight
          size={14}
          className="transition-transform group-hover:translate-x-0.5 shrink-0 hidden sm:inline-block"
        />
      </button>
    </div>
  );
}
