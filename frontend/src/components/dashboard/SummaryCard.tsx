import type { LucideIcon } from "lucide-react";
import { PrivacyMask } from "../ui/PrivacyMask";

interface SummaryCardProps {
  title: string;
  value: number;
  icon: LucideIcon;
  variant?: "income" | "expense" | "balance" | "projection" | "default";
  subtitle?: string;
  trend?: { value: number; isPositive: boolean };
}

export function SummaryCard({
  title,
  value,
  icon: Icon,
  variant = "default",
  subtitle,
}: SummaryCardProps) {
  const variantStyles = {
    income: {
      shell: "bg-income-soft border-income/10 rounded-tl-[2.25rem] rounded-br-[2.25rem] rounded-tr-xl rounded-bl-xl",
      badge: "bg-surface/70 text-income",
      valueColor: "text-income",
      rail: "bg-income",
    },
    expense: {
      shell: "bg-expense-soft border-expense/10 rounded-tr-[2.25rem] rounded-bl-[2.25rem] rounded-tl-xl rounded-br-xl",
      badge: "bg-surface/70 text-expense",
      valueColor: "text-expense",
      rail: "bg-expense",
    },
    balance: {
      shell: "bg-surface border-border rounded-3xl",
      badge: "bg-brand/10 text-brand",
      valueColor: "text-primary",
      rail: "bg-brand",
    },
    projection: {
      shell: "bg-surface border-border rounded-3xl",
      badge: "bg-elevated text-secondary",
      valueColor: "text-primary",
      rail: "bg-info",
    },
    default: {
      shell: "bg-surface border-border rounded-3xl",
      badge: "bg-elevated text-muted",
      valueColor: "text-primary",
      rail: "bg-brand",
    },
  };

  const style = variantStyles[variant] || variantStyles.default;

  return (
    <article
      className={`relative min-h-36 overflow-hidden border p-5 sm:p-6 transition-all duration-300 flex flex-col justify-between group cursor-default ${style.shell}`}
    >
      <div className={`absolute left-0 top-7 h-10 w-1 rounded-r-full ${style.rail}`} />
      <div className="absolute -right-8 -bottom-12 w-28 h-28 rounded-full border-[18px] border-current opacity-[0.035]" />
      <div className="relative flex items-center justify-between gap-3 mb-5">
        <span className="text-[10px] sm:text-[11px] font-extrabold uppercase tracking-[0.16em] text-muted flex items-center">
          <span>{title}</span>
        </span>
        <div
          className={`w-10 h-10 rounded-[1rem_1rem_1rem_0.3rem] flex items-center justify-center shrink-0 transition-transform duration-300 group-hover:-translate-y-0.5 ${style.badge}`}
        >
          <Icon size={20} />
        </div>
      </div>

      <div className="relative">
        <h4
          data-financial-value="true"
          className={`text-2xl sm:text-3xl font-extrabold tracking-[-0.05em] tabular-nums transition-colors duration-200 ${style.valueColor}`}
        >
          <PrivacyMask amount={value} />
        </h4>
        {subtitle && (
          <p className="text-[11px] font-medium text-muted mt-1">{subtitle}</p>
        )}
      </div>
    </article>
  );
}
