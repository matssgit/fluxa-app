import {
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  TrendingUp,
} from "lucide-react";

interface InsightsWidgetProps {
  totalIncome: number;
  totalExpenses: number;
  projectedBalance: number;
  savingsRate: number;
  burnRatePercentage: number;
}

export function InsightsWidget({
  totalIncome,
  totalExpenses,
  projectedBalance,
  savingsRate,
  burnRatePercentage,
}: InsightsWidgetProps) {
  // Geração dinâmica de insights baseada nas métricas do usuário
  const getInsights = () => {
    const tips = [];

    if (totalIncome === 0 && totalExpenses === 0) {
      return [
        {
          id: 1,
          type: "neutral",
          icon: Sparkles,
          title: "Movimente seu Caixa",
          text: "Registre suas primeiras receitas e despesas para liberar análises personalizadas por inteligência artificial.",
        },
      ];
    }

    if (projectedBalance >= 0 && savingsRate >= 20) {
      tips.push({
        id: 2,
        type: "success",
        icon: CheckCircle2,
        title: "Excelente Capacidade de Poupança",
        text: `Você está reservando ${savingsRate.toFixed(0)}% das suas receitas deste mês. Continue mantendo esse padrão de liquidez!`,
      });
    }

    if (burnRatePercentage > 80) {
      tips.push({
        id: 3,
        type: "warning",
        icon: AlertTriangle,
        title: "Atenção ao Ritmo de Gastos",
        text: `Suas despesas já consumiram ${burnRatePercentage.toFixed(0)}% do seu faturamento. Avalie segurar gastos não essenciais nas próximas semanas.`,
      });
    } else if (totalIncome > 0) {
      tips.push({
        id: 4,
        type: "success",
        icon: TrendingUp,
        title: "Fluxo Operacional Controlado",
        text: "Seu ritmo de saídas está bem distribuído em relação às entradas registradas no período.",
      });
    }

    return tips;
  };

  const insights = getInsights();

  return (
    <div className="card-default p-6 sm:p-8 flex flex-col justify-between border-subtle/30 shadow-sm">
      <div>
        <div className="flex items-center gap-2.5 mb-6">
          <div className="w-9 h-9 rounded-xl bg-gold-500/10 text-gold-500 flex items-center justify-center shrink-0 shadow-2xs border border-gold-500/20">
            <Sparkles size={18} />
          </div>
          <div>
            <h4 className="text-sm font-extrabold text-primary tracking-tight">
              Inteligência & Recomendações
            </h4>
            <p className="text-[11px] font-medium text-muted">
              Insights automáticos baseados no seu comportamento
            </p>
          </div>
        </div>

        <div className="space-y-3">
          {insights.map((tip) => {
            const Icon = tip.icon;
            const isWarning = tip.type === "warning";

            return (
              <div
                key={tip.id}
                className={`p-4 rounded-2xl border transition-all duration-200 flex items-start gap-3.5 ${
                  isWarning
                    ? "bg-amber-500/5 border-amber-500/20 text-amber-500"
                    : "bg-elevated/40 border-subtle/20 text-primary"
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 mt-0.5 ${
                    isWarning
                      ? "bg-amber-500/10 text-amber-500"
                      : "bg-emerald-500/10 text-emerald-500"
                  }`}
                >
                  <Icon size={16} />
                </div>
                <div>
                  <h5 className="text-xs sm:text-sm font-bold tracking-tight">
                    {tip.title}
                  </h5>
                  <p className="text-xs font-medium text-muted mt-1 leading-relaxed">
                    {tip.text}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="mt-6 pt-4 border-t border-subtle/20 flex items-center justify-between text-[11px] font-bold text-muted">
        <span>🤖 Motor de BI Ativo</span>
        <span className="text-secondary">Atualizado em tempo real</span>
      </div>
    </div>
  );
}
