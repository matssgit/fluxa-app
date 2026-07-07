import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getTransactions,
  getSummary,
  createTransaction,
} from "../services/transactions";
import type { NewTransactionInput } from "../types/transaction";

export function useTransactions() {
  const queryClient = useQueryClient();

  // 1. Busca a lista oficial de lançamentos do Backend V2
  const transactionsQuery = useQuery({
    queryKey: ["transactions"],
    queryFn: getTransactions,
    retry: false,
  });

  // 2. Busca o resumo operacional (income, expense, net amount) já calculado pelo banco
  const summaryQuery = useQuery({
    queryKey: ["summary"],
    queryFn: getSummary,
    retry: false,
  });

  // 3. Mutação limpa para novos lançamentos
  const createTransactionMutation = useMutation({
    mutationFn: (data: NewTransactionInput) => createTransaction(data),
    onSuccess: () => {
      // Invalida todos os pontos da plataforma que dependem do caixa
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
