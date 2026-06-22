import type { Transaction } from "../types/transaction"; // Correção: adicionado o 'type'
import { formatCurrency } from "../utils/currency";
import { formatDate } from "../utils/date";

interface TransactionTableProps {
   transactions: Transaction[];
   isLoading: boolean;
}

export function TransactionTable({
   transactions,
   isLoading,
}: TransactionTableProps) {
   if (isLoading) {
      return (
         <div className="animate-pulse flex flex-col gap-4">
            {[1, 2, 3].map((i) => (
               <div
                  key={i}
                  className="h-16 bg-slate-200 rounded-xl w-full"
               ></div>
            ))}
         </div>
      );
   }

   if (transactions.length === 0) {
      return (
         <div className="text-center py-12 bg-white rounded-2xl border border-slate-100 shadow-sm">
            <p className="text-slate-500">Nenhuma transação encontrada.</p>
            <p className="text-sm text-slate-400 mt-1">
               Crie sua primeira transação acima para começar.
            </p>
         </div>
      );
   }

   return (
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
         <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
               <thead>
                  <tr className="bg-slate-50 border-b border-slate-100 text-slate-500 text-sm">
                     <th className="font-medium p-4 pl-6">Descrição</th>
                     <th className="font-medium p-4">Valor</th>
                     <th className="font-medium p-4">Categoria</th>
                     <th className="font-medium p-4 pr-6">Data</th>
                  </tr>
               </thead>
               <tbody>
                  {transactions.map((transaction) => (
                     <tr
                        key={transaction.id}
                        className="border-b border-slate-50 hover:bg-slate-50/50 transition-colors last:border-0"
                     >
                        <td className="p-4 pl-6 text-slate-800 font-medium">
                           {transaction.title}
                        </td>
                        <td
                           className={`p-4 font-medium ${transaction.type === "credit" ? "text-emerald-500" : "text-red-500"}`}
                        >
                           {transaction.type === "debit" && "- "}
                           {formatCurrency(transaction.amount)}
                        </td>
                        <td className="p-4">
                           <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600 capitalize">
                              {transaction.type === "credit"
                                 ? "Receita"
                                 : "Despesa"}
                           </span>
                        </td>
                        <td className="p-4 pr-6 text-slate-500 text-sm">
                           {formatDate(transaction.created_at)}
                        </td>
                     </tr>
                  ))}
               </tbody>
            </table>
         </div>
      </div>
   );
}
