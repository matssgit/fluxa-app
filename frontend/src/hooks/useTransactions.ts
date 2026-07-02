import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
   getTransactions,
   getSummary,
   createTransaction,
} from "../services/transactions";
import type { TransactionFormData } from "../schemas/transactionSchema";

// 1. Criamos um tipo local para o ESLint e o TS ficarem em paz com o cálculo
interface TransactionRecord {
   amount: number | string;
   type?: string;
}

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
      // 2. Usamos o TransactionFormData (resolvendo o erro de variável não usada)
      // E aceitamos campos extras que a API pede, forçando a tipagem com o Parameters
      mutationFn: (
         data: TransactionFormData & { account_id?: string; status?: string },
      ) => createTransaction(data as Parameters<typeof createTransaction>[0]),
      onSuccess: () => {
         queryClient.invalidateQueries({ queryKey: ["transactions"] });
         queryClient.invalidateQueries({ queryKey: ["summary"] });
         queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      },
   });

   // 3. Bypass perfeito e limpo: passamos por 'unknown' antes de assumir o nosso tipo auxiliar
   const txList =
      (transactionsQuery.data as unknown as TransactionRecord[]) || [];

   const income = txList.reduce(
      (acc, curr) =>
         curr.type === "entrada" ? acc + Number(curr.amount) : acc,
      0,
   );

   const expense = txList.reduce(
      (acc, curr) => (curr.type === "saida" ? acc + Number(curr.amount) : acc),
      0,
   );

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
