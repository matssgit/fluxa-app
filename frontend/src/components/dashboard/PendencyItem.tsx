import { AlertCircle } from "lucide-react";
import { Card, Button, Badge } from "../ui";

interface PendencyItemProps {
  title: string;
  amount: number | undefined;
  dueDate: string;
  onAction: () => void;
}

export function PendencyItem({
  title,
  amount,
  dueDate,
  onAction,
}: PendencyItemProps) {
  const formattedValue = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount ?? 0);

  return (
    <Card variant="default" className="flex items-center justify-between p-4">
      <div className="flex items-center gap-3 min-w-0 flex-1">
        {/* O Badge aplica o tom semântico de alerta sem digitarmos bg-* ou border-* */}
        <Badge
          variant="warning"
          className="p-2 flex items-center justify-center shrink-0"
        >
          <AlertCircle size={18} />
        </Badge>

        {/* Utilitários puramente de layout e tipografia semântica */}
        <div className="min-w-0 flex-1">
          <h4 className="font-semibold text-sm truncate">{title}</h4>
          <div className="flex items-center gap-2 mt-0.5 text-xs font-medium opacity-70">
            <span>{formattedValue}</span>
            <span>•</span>
            <span>Vence dia {dueDate}</span>
          </div>
        </div>
      </div>

      {/* Botão tipado, autônomo e sem classes manuais de estilização */}
      <Button
        variant="secondary"
        size="sm"
        onClick={onAction}
        className="ml-3 shrink-0"
      >
        Resolver
      </Button>
    </Card>
  );
}
