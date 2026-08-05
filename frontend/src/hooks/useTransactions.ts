import type { NewTransactionInput } from "../types/transaction";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getTransactions,
  getSummary,
  createTransaction,
} from "../services/transactions";

export function useTransactions() {
  const queryClient = useQueryClient();

  const transactionsQuery = useQuery({
    queryKey: ["transactions"],
    queryFn: getTransactions,
    retry: false,
  });

  const summaryQuery = useQuery({
    queryKey: ["summary"],
    queryFn: getSummary,
    retry: false,
  });

  const createTransactionMutation = useMutation({
    mutationFn: (data: NewTransactionInput) => createTransaction(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
  });

  return {
    transactions: transactionsQuery.data || [],
    summary: summaryQuery.data || { amount: 0, income: 0, expense: 0 },
    isLoading: transactionsQuery.isLoading || summaryQuery.isLoading,
    isError: transactionsQuery.isError || summaryQuery.isError,
    createTransaction: createTransactionMutation.mutateAsync,
    isCreating: createTransactionMutation.isPending,
  };
}
