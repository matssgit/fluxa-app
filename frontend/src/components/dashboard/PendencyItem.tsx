interface PendencyItemProps {
  title: string;
  amount: number | undefined;
  type: string;
  onAction: () => void;
}

export function PendencyItem({
  title,
  amount,
  type,
  onAction,
}: PendencyItemProps) {
  const formattedValue = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount ?? 0);

  return (
    <div className="p-4 bg-elevated rounded-xl border border-subtle flex flex-col justify-between gap-3">
      <h4 className="font-semibold text-sm text-primary">{title}</h4>
      <div className="flex items-center justify-between pt-2.5 border-t border-border/40">
        <span className="font-bold text-sm text-primary">{formattedValue}</span>
        <button onClick={onAction} className="btn-primary text-xs py-1.5 px-3">
          {type === "installment" ? "Pagar Parcela" : "Baixar"}
        </button>
      </div>
    </div>
  );
}
