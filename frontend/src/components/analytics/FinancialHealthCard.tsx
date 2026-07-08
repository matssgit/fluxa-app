import type { FinancialHealth } from "../../types/analytics";

interface FinancialHealthCardProps {
  health: FinancialHealth;
}

export function FinancialHealthCard({ health }: FinancialHealthCardProps) {
  const getStatusConfig = (status: FinancialHealth["status"]) => {
    switch (status) {
      case "excellent": return { label: "Excelente", color: "text-emerald-500 bg-emerald-500/10 border-emerald-500/20" };
      case "good": return { label: "Saudável", color: "text-brand bg-brand/10 border-brand/20" };
      case "attention": return { label: "Atenção", color: "text-amber-500 bg-amber-500/10 border-amber-500/20" };
      case "critical": return { label: "Crítico", color: "text-red-500 bg-red-500/10 border-red-500/20" };
    }
  };

  const statusConfig = getStatusConfig(health.status);

  return (
    <div className="card-default p-6 border-subtle/30 flex flex-col justify-between">
      {/* Topo: Título + Badge de Status */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h3 className="font-bold text-lg text-primary tracking-tight">Saúde Financeira</h3>
          <p className="text-xs text-muted mt-0.5">Indicador global de estabilidade e liquidez</p>
        </div>
        <span className={`text-xs font-extrabold uppercase tracking-wider px-3 py-1 rounded-full border ${statusConfig.color}`}>
          {statusConfig.label}
        </span>
      </div>

      {/* Centro: O Score 0-100 em Destaque Neumórfico */}
      <div className="my-6 flex items-center gap-6">
        <div className="relative flex items-center justify-center w-20 h-20 rounded-2xl bg-elevated border border-subtle/30 shadow-inner shrink-0">
          <span className="text-3xl font-extrabold text-primary tracking-tight">{health.score}</span>
          <span className="absolute bottom-1.5 text-[9px] font-extrabold uppercase tracking-widest text-muted">/100</span>
        </div>
        <div>
          <p className="text-sm font-semibold text-primary">
            {health.score >= 80 ? "Sua gestão está em excelente patamar técnico." : "Existem oportunidades claras para otimizar suas reservas."}
          </p>
          <p className="text-xs text-muted mt-1">
            Cálculo automatizado considerando liquidez, poupança e custos fixos.
          </p>
        </div>
      </div>

      {/* Rodapé: 3 KPIs do Algoritmo */}
      <div className="grid grid-cols-3 gap-3 pt-4 border-t border-subtle/20 text-center">
        <div className="p-2.5 rounded-xl bg-elevated/50">
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">Poupança</span>
          <span className="text-sm sm:text-base font-bold text-emerald-500 mt-0.5 block">{health.savings_rate}%</span>
        </div>
        <div className="p-2.5 rounded-xl bg-elevated/50">
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">Comprometido</span>
          <span className="text-sm sm:text-base font-bold text-primary mt-0.5 block">{health.commitment_rate}%</span>
        </div>
        <div className="p-2.5 rounded-xl bg-elevated/50">
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">Liquidez</span>
          <span className="text-sm sm:text-base font-bold text-secondary mt-0.5 block">{health.liquidity_months}m</span>
        </div>
      </div>
    </div>
  );
}