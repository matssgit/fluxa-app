import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getDashboardMetrics,
  getFinancialHealth,
  getInsights,
  getRecommendations,
  dismissInsight,
} from "../services/analytics";

export function useDashboardMetrics(month?: string, year?: number) {
  return useQuery({
    queryKey: ["analytics", "dashboard", month, year],
    queryFn: () => getDashboardMetrics(month, year),
    staleTime: 1000 * 60 * 5,
  });
}

export function useFinancialHealth() {
  return useQuery({
    queryKey: ["analytics", "health"],
    queryFn: getFinancialHealth,
    staleTime: 1000 * 60 * 10,
  });
}

export function useInsights() {
  return useQuery({
    queryKey: ["analytics", "insights"],
    queryFn: getInsights,
    staleTime: 1000 * 60 * 5,
  });
}

export function useRecommendations() {
  return useQuery({
    queryKey: ["analytics", "recommendations"],
    queryFn: getRecommendations,
    staleTime: 1000 * 60 * 10,
  });
}

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
