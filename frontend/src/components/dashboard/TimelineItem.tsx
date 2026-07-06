import { CreditCard, Receipt } from "lucide-react";
import { Card, Badge } from "../ui";

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
    <Card
      variant="interactive"
      className="flex items-center justify-between p-3.5"
    >
      <div className="flex items-center gap-3.5 min-w-0 flex-1">
        {/* O Badge assume a cor de fundo e borda semântica sem definirmos bg-* ou border-* */}
        <Badge
          variant={type === "credit" ? "success" : "neutral"}
          className="flex items-center justify-center p-2 shrink-0"
        >
          {type === "credit" ? <CreditCard size={18} /> : <Receipt size={18} />}
        </Badge>

        {/* Utilitários puramente tipográficos e de layout */}
        <div className="min-w-0 flex-1">
          <h4 className="font-semibold text-sm truncate">{title}</h4>
          <span className="text-xs font-medium opacity-70 block truncate">
            {description}
          </span>
        </div>
      </div>

      <span className="font-semibold text-sm shrink-0 ml-4">
        {formattedValue}
      </span>
    </Card>
  );
}
