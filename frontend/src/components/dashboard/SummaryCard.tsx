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
      badge: "bg-emerald-500/15 text-emerald-500 border border-emerald-500/30",
      borderHover: "hover:border-emerald-500/50",
      glow: "group-hover:shadow-[0_4px_20px_rgba(16,185,129,0.15)]",
      emoji: "💰",
      valueColor: "text-emerald-500 dark:text-emerald-600 font-black",
    },
    expense: {
      badge: "bg-red-500/15 text-red-500 border border-red-500/30",
      borderHover: "hover:border-red-500/50",
      glow: "group-hover:shadow-[0_4px_20px_rgba(239,68,68,0.15)]",
      emoji: "📉",
      valueColor: "text-red-500 dark:text-red-700 font-black",
    },
    balance: {
      badge: "bg-brand/15 text-brand border border-brand/30",
      borderHover: "hover:border-brand/60",
      glow: "group-hover:shadow-[0_4px_20px_rgba(19,49,42,0.25)]",
      emoji: "🏦",
      valueColor: "text-primary font-black",
    },
    projection: {
      badge: "bg-purple-500/15 text-purple-500 border border-purple-500/30",
      borderHover: "hover:border-purple-500/50",
      glow: "group-hover:shadow-[0_4px_20px_rgba(168,85,247,0.15)]",
      emoji: "🔮",
      valueColor: "text-purple-600 dark:text-purple-700 font-black",
    },
    default: {
      badge: "bg-subtle/20 text-muted border border-subtle/30",
      borderHover: "hover:border-subtle/50",
      glow: "group-hover:shadow-md",
      emoji: "📊",
      valueColor: "text-primary font-bold",
    },
  };

  const style = variantStyles[variant] || variantStyles.default;

  return (
    <div
      className={`p-5 sm:p-6 rounded-3xl bg-surface border border-subtle/30 ${style.borderHover} ${style.glow} transition-all duration-300 shadow-2xs flex flex-col justify-between group cursor-default`}
    >
      <div className="flex items-center justify-between gap-3 mb-4">
        <span className="text-xs sm:text-sm font-extrabold text-muted tracking-tight flex items-center gap-1.5">
          <span className="text-sm select-none">{style.emoji}</span>
          <span>{title}</span>
        </span>
        <div
          className={`w-10 h-10 rounded-2xl flex items-center justify-center shrink-0 transition-transform duration-300 group-hover:scale-110 shadow-2xs ${style.badge}`}
        >
          <Icon size={20} />
        </div>
      </div>

      <div>
        <h4
          className={`text-xl sm:text-2xl tracking-tight transition-colors duration-200 ${style.valueColor}`}
        >
          <PrivacyMask amount={value} />
        </h4>
        {subtitle && (
          <p className="text-[11px] font-medium text-muted mt-1">{subtitle}</p>
        )}
      </div>
    </div>
  );
}
