import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getSubscriptions,
  createSubscription,
  updateSubscriptionStatus,
  deleteSubscription,
  paySubscription,
  getSubscriptionAnalytics,
} from "../services/subscriptions";

// Hook para buscar a lista
export function useSubscriptions() {
  return useQuery({
    queryKey: ["subscriptions"],
    queryFn: getSubscriptions,
    staleTime: 1000 * 60 * 5, // 5 minutos de cache
  });
}

// Hook para criar
export function useCreateSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para pausar/ativar/cancelar
export function useUpdateSubscriptionStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateSubscriptionStatus,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para excluir definitivamente
export function useDeleteSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteSubscription,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para pagar (baixar) a assinatura
export function usePaySubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, account_id }: { id: string; account_id: string }) =>
      paySubscription(id, account_id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
    },
  });
}

// Hook para a inteligência analítica do painel
export function useSubscriptionAnalytics() {
  return useQuery({
    queryKey: ["subscriptions", "analytics"],
    queryFn: getSubscriptionAnalytics,
    staleTime: 1000 * 60 * 5, // Cache de 5 minutos
  });
}
