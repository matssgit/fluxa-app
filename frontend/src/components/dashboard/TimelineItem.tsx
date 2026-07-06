import { CreditCard, Receipt } from "lucide-react";

interface TimelineItemProps {
  title: string;
  description: string;
  amount: number | undefined;
  type?: string;
}

export function TimelineItem({
  title,
  description,
  amount,
  type,
}: TimelineItemProps) {
  const formattedValue = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount ?? 0);

  return (
    <div className="flex items-center justify-between p-3.5 hover:bg-elevated rounded-xl transition-colors duration-200">
      <div className="flex items-center gap-4 min-w-0">
        <div className="p-2 rounded-lg bg-elevated text-brand border border-subtle">
          {type === "credit" ? <CreditCard size={16} /> : <Receipt size={16} />}
        </div>
        <div className="truncate">
          <h4 className="font-semibold text-sm text-primary truncate">
            {title}
          </h4>
          <span className="text-xs text-muted block truncate">
            {description}
          </span>
        </div>
      </div>
      <span className="font-semibold text-sm text-primary ml-4 shrink-0">
        {formattedValue}
      </span>
    </div>
  );
}
