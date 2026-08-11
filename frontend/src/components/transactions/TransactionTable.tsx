import {
  ArrowDownCircle,
  ArrowUpCircle,
  Building,
  Check,
  Landmark,
  PiggyBank,
  Tag,
  Wallet,
  WalletCards,
} from "lucide-react";

import { PrivacyMask } from "../ui/PrivacyMask";
import { Skeleton } from "../ui/Skeleton";
import { api } from "../../api/client";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

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

function getAccountIcon(accountName?: string) {
  if (!accountName)
    return <Building size={16} className="text-muted shrink-0" />;

  const name = accountName.toLowerCase();

  if (
    name.includes("cartão") ||
    name.includes("card") ||
    name.includes("crédito") ||
    name.includes("black") ||
    name.includes("platinum") ||
    name.includes("visa") ||
    name.includes("master") ||
    name.includes("elo") ||
    name.includes("amex")
  ) {
    return <WalletCards size={16} className="text-muted shrink-0" />;
  }
  if (
    name.includes("cofre") ||
    name.includes("cofrinho") ||
    name.includes("reserva") ||
    name.includes("meta") ||
    name.includes("poupança") ||
    name.includes("investimento")
  ) {
    return <PiggyBank size={16} className="text-muted shrink-0" />;
  }
  if (
    name.includes("carteira") ||
    name.includes("dinheiro") ||
    name.includes("físico") ||
    name.includes("espécie") ||
    name.includes("caixa")
  ) {
    return <Wallet size={16} className="text-muted shrink-0" />;
  }
  if (
    name.includes("banco") ||
    name.includes("itaú") ||
    name.includes("nubank") ||
    name.includes("bradesco") ||
    name.includes("inter") ||
    name.includes("santander") ||
    name.includes("caixa") ||
    name.includes("btg") ||
    name.includes("sicredi") ||
    name.includes("sicoob") ||
    name.includes("bb") ||
    name.includes("brasil") ||
    name.includes("c6") ||
    name.includes("sofisa") ||
    name.includes("xp")
  ) {
    return <Landmark size={16} className="text-muted shrink-0" />;
  }

  return <Building size={16} className="text-muted shrink-0" />;
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
      <div className="overflow-x-auto w-full pb-2">
        <table className="w-full min-w-600px text-left border-collapse">
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
                        <ArrowUpCircle
                          className="text-emerald-500 shrink-0"
                          size={20}
                        />
                      ) : (
                        <ArrowDownCircle
                          className="text-red-500 shrink-0"
                          size={20}
                        />
                      )}
                      <span className="font-semibold text-primary group-hover:text-brand transition-colors truncate">
                        {transaction.title}
                      </span>
                    </div>
                  </td>

                  <td className="p-4">
                    <div className="flex items-center gap-2 text-secondary text-sm font-medium whitespace-nowrap">
                      {getAccountIcon(transaction.account_name)}
                      <span className="truncate max-w-37.5 sm:max-w-none">
                        {transaction.account_name ?? "Sem conta"}
                      </span>
                    </div>
                  </td>

                  <td className="p-4">
                    {transaction.category_name ? (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-subtle/40 border border-subtle/30 text-primary/90 text-xs font-bold shadow-2xs whitespace-nowrap">
                        <Tag size={14} className="text-brand shrink-0" />
                        <span>{transaction.category_name}</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-elevated/20 text-muted/50 text-xs font-medium italic whitespace-nowrap">
                        <Tag size={13} className="text-muted/40 shrink-0" />
                        <span>Sem categoria</span>
                      </span>
                    )}
                  </td>

                  <td className="p-4 text-secondary text-sm font-medium whitespace-nowrap">
                    {displayDate && !isNaN(new Date(displayDate).getTime())
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
                        <PrivacyMask amount={transaction.amount} />
                      </span>
                      {isPending && (
                        <button
                          onClick={() =>
                            handleCompleteTransaction(transaction.id)
                          }
                          disabled={updatingId === transaction.id}
                          className="flex items-center gap-1.5 bg-emerald-500/10 hover:bg-emerald-500 text-emerald-500 hover:text-white px-3 py-1.5 rounded-lg text-xs font-bold transition-all border border-emerald-500/20 cursor-pointer disabled:opacity-50"
                          title="Marcar como Pago/Recebido"
                        >
                          <Check size={14} className="stroke-3 shrink-0" />
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
