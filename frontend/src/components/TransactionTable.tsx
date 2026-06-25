import {
   ArrowDownCircle,
   ArrowUpCircle,
   Building,
   Tag,
   Check,
} from "lucide-react";
import { api } from "../api/client";
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

export function TransactionTable({
   transactions,
   isLoading,
}: TransactionTableProps) {
   const queryClient = useQueryClient();
   const [updatingId, setUpdatingId] = useState<string | null>(null);

   async function handleCompleteTransaction(id: string) {
      try {
         setUpdatingId(id);
         // Dispara a rota PATCH que criamos no Back-end
         await api.patch(`/transactions/${id}/complete`);

         // Invalida os caches para o Dashboard recalcular o saldo e recarregar a lista
         queryClient.invalidateQueries({ queryKey: ["transactions"] });
         queryClient.invalidateQueries({ queryKey: ["summary"] });
      } catch (error) {
         console.error("Erro ao dar baixa na transação", error);
         alert("Não foi possível dar baixa neste lançamento.");
      } finally {
         setUpdatingId(null);
      }
   }

   if (isLoading) {
      return (
         <div className="text-center py-10 text-slate-500">
            Carregando lançamentos...
         </div>
      );
   }

   if (transactions.length === 0) {
      return (
         <div className="bg-white rounded-2xl border border-slate-100 p-12 text-center shadow-sm">
            <p className="text-slate-500">
               Nenhum lançamento encontrado nesta aba.
            </p>
         </div>
      );
   }

   return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
         <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
               <thead>
                  <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 text-sm uppercase tracking-wider">
                     <th className="font-medium p-4 pl-6 w-2/5">Descrição</th>
                     <th className="font-medium p-4">Conta</th>
                     <th className="font-medium p-4">Categoria</th>
                     <th className="font-medium p-4">Vencimento / Pagamento</th>
                     <th className="font-medium p-4 pr-6 text-right">Valor</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-slate-100">
                  {transactions.map((transaction) => {
                     const isIncome = transaction.amount > 0;
                     const isPending = transaction.status === "pending";

                     // Se for pendente, exibe expected_date. Se já foi pago, exibe completed_date ou created_at.
                     const displayDate = isPending
                        ? transaction.expected_date
                        : transaction.completed_date || transaction.created_at;

                     return (
                        <tr
                           key={transaction.id}
                           className="hover:bg-slate-50/50 transition-colors group"
                        >
                           <td className="p-4 pl-6">
                              <div className="flex items-center gap-3">
                                 {isIncome ? (
                                    <ArrowUpCircle
                                       className="text-emerald-500"
                                       size={20}
                                    />
                                 ) : (
                                    <ArrowDownCircle
                                       className="text-red-500"
                                       size={20}
                                    />
                                 )}
                                 <span className="font-medium text-slate-700 group-hover:text-slate-900 transition-colors">
                                    {transaction.title}
                                 </span>
                              </div>
                           </td>

                           <td className="p-4">
                              <div className="flex items-center gap-2 text-slate-500 text-sm">
                                 <Building
                                    size={16}
                                    className="text-slate-400"
                                 />
                                 {transaction.account_name || "Sem conta"}
                              </div>
                           </td>

                           <td className="p-4">
                              <div className="flex items-center gap-2 text-slate-500 text-sm">
                                 <Tag size={16} className="text-slate-400" />
                                 {transaction.category_name || "Sem categoria"}
                              </div>
                           </td>

                           <td className="p-4 text-slate-500 text-sm">
                              {displayDate
                                 ? new Intl.DateTimeFormat("pt-BR").format(
                                      new Date(displayDate),
                                   )
                                 : "---"}
                           </td>

                           <td className="p-4 pr-6 text-right whitespace-nowrap">
                              <div className="flex items-center justify-end gap-3">
                                 {/* O valor do lançamento */}
                                 <span
                                    className={`font-medium ${isIncome ? "text-emerald-500" : "text-red-500"}`}
                                 >
                                    {new Intl.NumberFormat("pt-BR", {
                                       style: "currency",
                                       currency: "BRL",
                                    }).format(transaction.amount)}
                                 </span>

                                 {/* Botão Dinâmico de "Dar Baixa" - Aparece apenas na aba de Pendentes */}
                                 {isPending && (
                                    <button
                                       onClick={() =>
                                          handleCompleteTransaction(
                                             transaction.id,
                                          )
                                       }
                                       disabled={updatingId === transaction.id}
                                       className="flex items-center gap-1 bg-emerald-50 hover:bg-emerald-500 text-emerald-600 hover:text-white p-1.5 rounded-lg text-xs font-semibold transition-all shadow-sm border border-emerald-200/50"
                                       title="Marcar como Pago/Recebido"
                                    >
                                       <Check size={14} />
                                       <span className="hidden sm:inline">
                                          Baixar
                                       </span>
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
