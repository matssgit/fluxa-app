import {
  ArrowDownCircle,
  ArrowUpCircle,
  Building,
  Tag,
  Check,
} from "lucide-react";
import { api } from "../../api/client";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Skeleton } from "../ui/Skeleton";

interface Transaction {
  id: string;
  title: string;
  amount: number;
  created_at: string;
  expected_date?: string;
  completed_date?: string;
  status: "pending" | "completed";
  account_name?: string;
  category_name?: string;
}

interface TransactionTableProps {
  transactions: Transaction[];
  isLoading: boolean;
}

export function TransactionTable({
  transactions,
  isLoading,
}: TransactionTableProps) {
  const queryClient = useQueryClient();
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  async function handleCompleteTransaction(id: string): Promise<void> {
    try {
      setUpdatingId(id);
      await api.patch(`/transactions/${id}/complete`);

      // Invalida os caches seguindo o padrão estabelecido
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["transactions"] }),
        queryClient.invalidateQueries({ queryKey: ["summary"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
    } catch (error) {
      console.error("Erro ao dar baixa na transação", error);
      alert("Não foi possível dar baixa neste lançamento.");
    } finally {
      setUpdatingId(null);
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-16 w-full rounded-xl" />
        <Skeleton className="h-16 w-full rounded-xl" />
        <Skeleton className="h-16 w-full rounded-xl" />
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <div className="card-default p-12 text-center border-dashed border-subtle/30">
        <p className="text-muted font-medium">
          Nenhum lançamento encontrado nesta aba.
        </p>
      </div>
    );
  }

  return (
    <div className="card-default overflow-hidden border-subtle/30">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-elevated/40 border-b border-subtle/20 text-muted text-[11px] font-extrabold uppercase tracking-widest">
              <th className="p-4 pl-6 w-2/5">Descrição</th>
              <th className="p-4">Conta</th>
              <th className="p-4">Categoria</th>
              <th className="p-4">Data</th>
              <th className="p-4 pr-6 text-right">Valor</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-subtle/20">
            {transactions.map((transaction: Transaction) => {
              const isIncome = transaction.amount > 0;
              const isPending = transaction.status === "pending";
              const displayDate = isPending
                ? transaction.expected_date
                : transaction.completed_date || transaction.created_at;

              return (
                <tr
                  key={transaction.id}
                  className="hover:bg-elevated/60 transition-colors group"
                >
                  <td className="p-4 pl-6">
                    <div className="flex items-center gap-3">
                      {isIncome ? (
                        <ArrowUpCircle className="text-emerald-500" size={20} />
                      ) : (
                        <ArrowDownCircle className="text-red-500" size={20} />
                      )}
                      <span className="font-semibold text-primary group-hover:text-brand transition-colors">
                        {transaction.title}
                      </span>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center gap-2 text-secondary text-sm font-medium">
                      <Building size={16} className="text-muted" />
                      {transaction.account_name ?? "Sem conta"}
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center gap-2 text-secondary text-sm font-medium">
                      <Tag size={16} className="text-muted" />
                      {transaction.category_name ?? "Sem categoria"}
                    </div>
                  </td>
                  <td className="p-4 text-secondary text-sm font-medium">
                    {displayDate
                      ? new Intl.DateTimeFormat("pt-BR").format(
                          new Date(displayDate),
                        )
                      : "---"}
                  </td>
                  <td className="p-4 pr-6 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-3">
                      <span
                        className={`font-bold ${isIncome ? "text-emerald-500" : "text-primary"}`}
                      >
                        {new Intl.NumberFormat("pt-BR", {
                          style: "currency",
                          currency: "BRL",
                        }).format(transaction.amount)}
                      </span>
                      {isPending && (
                        <button
                          onClick={() =>
                            handleCompleteTransaction(transaction.id)
                          }
                          disabled={updatingId === transaction.id}
                          className="flex items-center gap-1.5 bg-emerald-500/10 hover:bg-emerald-500 text-emerald-500 hover:text-white px-3 py-1.5 rounded-lg text-xs font-bold transition-all border border-emerald-500/20"
                          title="Marcar como Pago/Recebido"
                        >
                          <Check size={14} className="stroke-3" />
                          <span className="hidden sm:inline">Baixar</span>
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
