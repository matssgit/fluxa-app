import React from "react";
import { Card } from "../ui";

interface SummaryCardProps {
  title: string;
  value: number | undefined;
  icon: React.ElementType;
}

export function SummaryCard({ title, value, icon: Icon }: SummaryCardProps) {
  // A formatação de moeda é regra de apresentação de domínio (mantida sem alteração lógica)
  const formattedValue = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value ?? 0);

  return (
    <Card variant="metric" className="flex items-center justify-between">
      <div className="space-y-1 min-w-0">
        <span className="text-xs font-semibold uppercase tracking-wider block truncate">
          {title}
        </span>
        <strong className="text-2xl font-bold tracking-tight block truncate">
          {formattedValue}
        </strong>
      </div>

      {/* O contêiner do ícone usa apenas layout (flex/padding), sem definir bg-* ou text-* */}
      <div className="p-2.5 flex items-center justify-center shrink-0">
        <Icon size={24} />
      </div>
    </Card>
  );
}
