import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
   getSubscriptions,
   createSubscription,
   updateSubscriptionStatus,
   deleteSubscription,
} from "../services/subscriptions";
import { paySubscription } from "../services/subscriptions";

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
         // Invalida o cache para forçar a tela a buscar a lista atualizada
         queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
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
         // Invalida o dashboard e transações para a tela piscar atualizada na mesma hora
         queryClient.invalidateQueries({ queryKey: ["dashboard"] });
         queryClient.invalidateQueries({ queryKey: ["transactions"] });
         queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      },
   });
}
