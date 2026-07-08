import { api } from "../api/client";
import type {
  DashboardMetrics,
  FinancialHealth,
  Insight,
  Recommendation,
} from "../types/analytics";

// Busca o consolidado geral para o Cockpit Executivo
export async function getDashboardMetrics(
  month?: string,
  year?: number,
): Promise<DashboardMetrics> {
  const params = { month, year };
  const response = await api.get<{ metrics: DashboardMetrics }>(
    "/analytics/dashboard",
    { params },
  );
  return response.data.metrics;
}

// Busca isolada do Score de Saúde Financeira
export async function getFinancialHealth(): Promise<FinancialHealth> {
  const response = await api.get<{ health: FinancialHealth }>(
    "/analytics/health",
  );
  return response.data.health;
}

// Busca a lista de Insights e Avisos Inteligentes
export async function getInsights(): Promise<Insight[]> {
  const response = await api.get<{ insights: Insight[] }>(
    "/analytics/insights",
  );
  return response.data.insights;
}

// Busca Recomendações Acionáveis
export async function getRecommendations(): Promise<Recommendation[]> {
  const response = await api.get<{ recommendations: Recommendation[] }>(
    "/analytics/recommendations",
  );
  return response.data.recommendations;
}

// Ação de dispensar/arquivar um insight já lido pelo usuário
export async function dismissInsight(id: string): Promise<void> {
  await api.patch(`/analytics/insights/${id}/dismiss`);
}
