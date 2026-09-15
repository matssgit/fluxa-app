import { api } from "../api/client";
import type {
  DashboardMetrics,
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
