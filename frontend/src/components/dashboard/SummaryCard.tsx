import React from "react";

interface SummaryCardProps {
  title: string;
  value: number | undefined;
  icon: React.ElementType;
  variant?: "brand" | "secondary" | "accent";
}

export function SummaryCard({
  title,
  value,
  icon: Icon,
  variant = "brand",
}: SummaryCardProps) {
  const formattedValue = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value ?? 0);

  const iconColorMap = {
    brand: "text-brand",
    secondary: "text-secondary",
    accent: "text-accent",
  };

  return (
    <div className="card-interactive flex items-center justify-between">
      <div className="space-y-1">
        <span className="text-[11px] font-bold text-muted uppercase tracking-wider block">
          {title}
        </span>
        <strong className="text-2xl font-bold text-primary tracking-tight block">
          {formattedValue}
        </strong>
      </div>
      <div
        className={`p-2.5 rounded-xl bg-elevated border border-subtle ${iconColorMap[variant]}`}
      >
        <Icon size={20} />
      </div>
    </div>
  );
}
