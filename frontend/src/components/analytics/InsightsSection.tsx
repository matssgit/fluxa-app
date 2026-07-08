import {
  CheckCircle2,
  AlertTriangle,
  Info,
  ArrowUpRight,
  Sparkles,
} from "lucide-react";
import { Link } from "react-router-dom";
import type { Insight, Recommendation } from "../../types/analytics";

interface InsightsSectionProps {
  insights: Insight[];
  recommendations: Recommendation[];
}

export function InsightsSection({
  insights,
  recommendations,
}: InsightsSectionProps) {
  const getInsightIcon = (type: Insight["type"]) => {
    switch (type) {
      case "positive":
        return (
          <CheckCircle2
            size={18}
            className="text-emerald-500 shrink-0 mt-0.5"
          />
        );
      case "warning":
        return (
          <AlertTriangle size={18} className="text-amber-500 shrink-0 mt-0.5" />
        );
      default:
        return <Info size={18} className="text-brand shrink-0 mt-0.5" />;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Sparkles size={18} className="text-brand animate-pulse" />
        <h3 className="font-bold text-lg text-primary tracking-tight">
          Insights & Recomendações do Ecossistema
        </h3>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Lista de Insights Automáticos */}
        <div className="space-y-3">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted block mb-2">
            Diagnóstico Comportamental
          </span>
          {insights.map((item) => (
            <div
              key={item.id}
              className="card-default p-4 border-subtle/30 flex items-start gap-3.5 bg-elevated/30"
            >
              {getInsightIcon(item.type)}
              <div>
                <h4 className="font-bold text-sm text-primary">{item.title}</h4>
                <p className="text-xs text-muted mt-1 leading-relaxed">
                  {item.description}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Lista de Recomendações Acionáveis - Sem conflito de borda! */}
        <div className="space-y-3">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted block mb-2">
            Ações Sugeridas para Otimização
          </span>
          {recommendations.map((rec) => (
            <div
              key={rec.id}
              className="card-default p-4 flex flex-col justify-between bg-brand/5 border-brand/20"
            >
              <div>
                <div className="flex justify-between items-start gap-2">
                  <h4 className="font-bold text-sm text-primary">
                    {rec.title}
                  </h4>
                  {rec.impact_estimate && (
                    <span className="text-[10px] font-extrabold px-2 py-0.5 rounded bg-brand/10 text-brand border border-brand/20 shrink-0">
                      {rec.impact_estimate}
                    </span>
                  )}
                </div>
                <p className="text-xs text-muted mt-1.5 leading-relaxed">
                  {rec.description}
                </p>
              </div>

              {rec.action_url && (
                <div className="mt-4 pt-3 border-t border-brand/10 flex justify-end">
                  <Link
                    to={rec.action_url}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-brand hover:text-brand-light transition-colors"
                  >
                    <span>{rec.action_label}</span>
                    <ArrowUpRight size={14} />
                  </Link>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
