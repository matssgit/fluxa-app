import { api } from "../api/client";
import { useQuery } from "@tanstack/react-query";
import type { DashboardResponse } from "../types/dashboard";

export const useDashboard = () => {
  return useQuery<DashboardResponse>({
    queryKey: ["dashboard"],
    queryFn: async () => {
      const { data } = await api.get<DashboardResponse>("/dashboard");
      return data;
    },

    staleTime: 60000,
  });
};
