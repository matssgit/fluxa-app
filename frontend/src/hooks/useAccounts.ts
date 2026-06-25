import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAccounts, createAccount } from "../services/accounts";

export function useAccounts() {
   const queryClient = useQueryClient();

   const accountsQuery = useQuery({
      queryKey: ["accounts"],
      queryFn: getAccounts,
   });

   const createAccountMutation = useMutation({
      mutationFn: createAccount,
      onSuccess: () => {
         // Invalida o cache para recarregar a lista automaticamente
         queryClient.invalidateQueries({ queryKey: ["accounts"] });
      },
   });

   return {
      accounts: accountsQuery.data || [],
      isLoading: accountsQuery.isLoading,
      createAccount: createAccountMutation.mutateAsync,
      isCreating: createAccountMutation.isPending,
   };
}
