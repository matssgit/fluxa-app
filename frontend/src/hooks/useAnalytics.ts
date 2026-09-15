import { useQuery } from "@tanstack/react-query";
import { getDashboardMetrics } from "../services/analytics";

export function useDashboardMetrics(month?: string, year?: number) {
  return useQuery({
    queryKey: ["analytics", "dashboard", month, year],
    queryFn: () => getDashboardMetrics(month, year),
    staleTime: 1000 * 60 * 5,
  });
}
