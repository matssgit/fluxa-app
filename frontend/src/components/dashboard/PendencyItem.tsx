import { Calendar, ArrowRight } from "lucide-react";
import { formatCurrency } from "../../utils/formatters";

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
    <div className="flex items-center justify-between p-4 rounded-2xl bg-surface border border-subtle/30 hover:border-subtle/60 transition-all duration-200 shadow-2xs group">
      {/* Informações da Pendência */}
      <div className="space-y-1 pr-3 overflow-hidden">
        <h4 className="text-sm font-extrabold text-primary truncate tracking-tight">
          {title}
        </h4>
        <div className="flex items-center gap-2 text-xs text-muted font-medium">
          <span className="flex items-center gap-1">
            <Calendar size={13} className="text-brand shrink-0" />
            {dueDate}
          </span>
          <span>•</span>
          <span className="font-bold text-primary">
            {formatCurrency(amount)}
          </span>
        </div>
      </div>

      {/* Botão "Resolver" Estilizado (Pine & Sage) */}
      <button
        type="button"
        onClick={onAction}
        className="shrink-0 px-4 py-2 rounded-xl bg-brand text-white text-xs font-bold tracking-wide hover:bg-secondary active:scale-95 transition-all duration-200 flex items-center gap-1.5 shadow-xs cursor-pointer"
      >
        <span>Resolver</span>
        <ArrowRight
          size={14}
          className="transition-transform group-hover:translate-x-0.5"
        />
      </button>
    </div>
  );
}
