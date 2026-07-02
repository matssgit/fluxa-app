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
      // Mantemos o staleTime para performance, garantindo que o Dashboard
      // seja a fonte de dados consolidada.
      staleTime: 60000,
   });
};
