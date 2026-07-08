import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getDashboardMetrics,
  getFinancialHealth,
  getInsights,
  getRecommendations,
  dismissInsight,
} from "../services/analytics";

// Hook do Cockpit Executivo (Consumirá tudo de uma vez na tela inicial)
export function useDashboardMetrics(month?: string, year?: number) {
  return useQuery({
    queryKey: ["analytics", "dashboard", month, year],
    queryFn: () => getDashboardMetrics(month, year),
    staleTime: 1000 * 60 * 5, // 5 minutos de cache (evita recálculo excessivo no Back)
  });
}

// Hook Isolado de Saúde Financeira
export function useFinancialHealth() {
  return useQuery({
    queryKey: ["analytics", "health"],
    queryFn: getFinancialHealth,
    staleTime: 1000 * 60 * 10, // 10 minutos
  });
}

// Hook de Insights
export function useInsights() {
  return useQuery({
    queryKey: ["analytics", "insights"],
    queryFn: getInsights,
    staleTime: 1000 * 60 * 5,
  });
}

// Hook de Recomendações
export function useRecommendations() {
  return useQuery({
    queryKey: ["analytics", "recommendations"],
    queryFn: getRecommendations,
    staleTime: 1000 * 60 * 10,
  });
}

// Mutação para dispensar/arquivar um alerta
export function useDismissInsight() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => dismissInsight(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["analytics"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
