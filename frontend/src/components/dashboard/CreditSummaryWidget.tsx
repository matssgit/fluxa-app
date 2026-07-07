import { CreditCard, ArrowRight, ShieldAlert } from "lucide-react";
import { Link } from "react-router-dom";
import { useCards } from "../../hooks/useCredit";
import { Skeleton } from "../ui/Skeleton";

export function CreditSummaryWidget() {
  const { data: cards = [], isLoading } = useCards();

  if (isLoading) {
    return (
      <div className="card-default p-6 space-y-4">
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-3 w-full rounded-full" />
      </div>
    );
  }

  // Cálculos consolidados do ecossistema de crédito
  const totalLimit = cards.reduce(
    (acc, card) => acc + Number(card.total_limit || 0),
    0,
  );
  const availableLimit = cards.reduce(
    (acc, card) => acc + Number(card.available_limit || 0),
    0,
  );
  const usedLimit = Math.max(totalLimit - availableLimit, 0);
  const usedPercentage =
    totalLimit > 0 ? Math.min((usedLimit / totalLimit) * 100, 100) : 0;

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  return (
    <div className="card-default p-6 sm:p-8 flex flex-col justify-between border-subtle/30 shadow-sm">
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0 shadow-2xs">
              <CreditCard size={18} />
            </div>
            <div>
              <h4 className="text-sm font-extrabold text-primary tracking-tight">
                Ecossistema de Crédito
              </h4>
              <p className="text-[11px] font-medium text-muted">
                {cards.length}{" "}
                {cards.length === 1 ? "cartão ativo" : "cartões ativos"} no
                sistema
              </p>
            </div>
          </div>

          <Link
            to="/cards"
            className="text-xs font-bold text-brand hover:text-brand-light flex items-center gap-1 group transition-colors"
          >
            <span>Gerenciar</span>
            <ArrowRight
              size={14}
              className="group-hover:translate-x-0.5 transition-transform"
            />
          </Link>
        </div>

        {cards.length === 0 ? (
          <div className="py-6 text-center border border-dashed border-subtle/30 rounded-2xl bg-elevated/20">
            <p className="text-xs font-medium text-muted mb-2">
              Nenhum cartão cadastrado ainda.
            </p>
            <Link
              to="/cards"
              className="text-xs font-bold text-brand hover:underline"
            >
              + Adicionar primeiro cartão
            </Link>
          </div>
        ) : (
          <div className="space-y-4 mt-6">
            <div className="flex justify-between items-end">
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                  Fatura / Limite Usado
                </span>
                <span className="text-lg sm:text-xl font-extrabold text-primary tracking-tight mt-0.5 block">
                  {formatCurrency(usedLimit)}
                </span>
              </div>
              <div className="text-right">
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                  Disponível
                </span>
                <span className="text-sm font-bold text-secondary">
                  {formatCurrency(availableLimit)}
                </span>
              </div>
            </div>

            {/* Barra de Progresso Global */}
            <div className="space-y-1.5">
              <div className="w-full h-3 bg-subtle/30 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    usedPercentage > 85
                      ? "bg-red-500 shadow-red-500/30"
                      : usedPercentage > 70
                        ? "bg-amber-500 shadow-amber-500/30"
                        : "bg-brand shadow-brand/30"
                  }`}
                  style={{ width: `${usedPercentage}%` }}
                />
              </div>
              <div className="flex justify-between items-center text-[11px] font-bold">
                <span className="text-muted">Consumo global</span>
                <span
                  className={
                    usedPercentage > 85 ? "text-red-500" : "text-secondary"
                  }
                >
                  {usedPercentage.toFixed(1)}% utilizado
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {usedPercentage > 85 && (
        <div className="mt-4 p-3 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center gap-2 text-xs font-semibold text-red-500">
          <ShieldAlert size={16} className="shrink-0" />
          <span>
            Alerta: Você está se aproximando do limite máximo do seu crédito.
          </span>
        </div>
      )}
    </div>
  );
}
