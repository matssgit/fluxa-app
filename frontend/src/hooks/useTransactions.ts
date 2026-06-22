import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
   getTransactions,
   getSummary,
   createTransaction,
} from "../services/transactions";
import type { TransactionFormData } from "../schemas/transactionSchema"; // Correção: adicionado o 'type'

export function useTransactions() {
   const queryClient = useQueryClient();

   const transactionsQuery = useQuery({
      queryKey: ["transactions"],
      queryFn: getTransactions,
      // Evita erro 401 quebrando a tela antes da primeira transação
      retry: false,
   });

   const summaryQuery = useQuery({
      queryKey: ["summary"],
      queryFn: getSummary,
      retry: false,
   });

   const createTransactionMutation = useMutation({
      mutationFn: (data: TransactionFormData) => createTransaction(data),
      onSuccess: () => {
         // Revalida a lista e o saldo automaticamente
         queryClient.invalidateQueries({ queryKey: ["transactions"] });
         queryClient.invalidateQueries({ queryKey: ["summary"] });
      },
   });

   // Cálculo local de Entradas e Saídas (já que a API só retorna o total no /summary)
   const income =
      transactionsQuery.data?.reduce(
         (acc, curr) => (curr.type === "credit" ? acc + curr.amount : acc),
         0,
      ) || 0;
   const expense =
      transactionsQuery.data?.reduce(
         (acc, curr) => (curr.type === "debit" ? acc + curr.amount : acc),
         0,
      ) || 0;

   return {
      transactions: transactionsQuery.data || [],
      summary: summaryQuery.data,
      income,
      expense,
      isLoading: transactionsQuery.isLoading || summaryQuery.isLoading,
      isError: transactionsQuery.isError,
      createTransaction: createTransactionMutation.mutateAsync,
      isCreating: createTransactionMutation.isPending,
   };
}
