import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
} from "../services/accounts";

export function useAccounts() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["accounts"],
    queryFn: getAccounts,
    staleTime: 1000 * 60 * 5,
  });

  const createMutation = useMutation({
    mutationFn: createAccount,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["accounts"] }),
  });

  const updateMutation = useMutation({
    mutationFn: updateAccount,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["accounts"] }),
  });

  return {
    accounts: query.data || [],
    isLoading: query.isLoading,
    createAccount: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    // ✨ AQUI: Expondo a edição para o Modal!
    updateAccount: updateMutation.mutateAsync,
    isUpdating: updateMutation.isPending,
  };
}

export function useDeleteAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
