import { Sprout, Target, TrendingUp, Layers } from "lucide-react";
import type { Wallet } from "../../types/wallet";

interface WalletsSummaryProps {
  wallets: Wallet[];
}

export function WalletsSummary({ wallets }: WalletsSummaryProps) {
  // Filtramos apenas as estufas em cultivo ativo ou concluído
  const activeWallets = wallets.filter((w) => w.status !== "paused");

  // 1. Acumulado Total (O quanto já cresceu em todas as caixinhas)
  const totalCurrent = activeWallets.reduce(
    (acc, w) => acc + (Number(w.current_amount) || 0),
    0,
  );

  // 2. Meta Global (A soma de todos os objetivos)
  const totalTarget = activeWallets.reduce(
    (acc, w) => acc + (Number(w.target_amount) || 0),
    0,
  );

  // 3. Progresso Médio do Ecossistema (%)
  const averageProgress =
    totalTarget > 0
      ? Math.min(100, Math.round((totalCurrent / totalTarget) * 100))
      : 0;

  // 4. Contagem de metas em andamento
  const activeCount = wallets.filter((w) => w.status === "active").length;

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(isNaN(val) ? 0 : val);

  return (
    <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-fade-in">
      {/* KPI 1: Acumulado Total */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Acumulado Total
          </span>
          <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <Sprout size={18} className="animate-pulse" />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {formatCurrency(totalCurrent)}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Recursos nas estufas
          </span>
        </div>
      </div>

      {/* KPI 2: Meta Global */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Alvo de Colheita
          </span>
          <div className="w-9 h-9 rounded-xl bg-elevated text-secondary flex items-center justify-center shrink-0">
            <Target size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {formatCurrency(totalTarget)}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Soma dos objetivos
          </span>
        </div>
      </div>

      {/* KPI 3: Progresso do Cultivo */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Maturidade Global
          </span>
          <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <TrendingUp size={18} />
          </div>
        </div>
        <div className="mt-4">
          <div className="flex items-baseline gap-1.5">
            <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
              {averageProgress}%
            </span>
          </div>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Crescimento consolidado
          </span>
        </div>
      </div>

      {/* KPI 4: Estufas Ativas */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Estufas Ativas
          </span>
          <div className="w-9 h-9 rounded-xl bg-elevated text-secondary flex items-center justify-center shrink-0">
            <Layers size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {activeCount}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Metas em andamento
          </span>
        </div>
      </div>
    </section>
  );
}
