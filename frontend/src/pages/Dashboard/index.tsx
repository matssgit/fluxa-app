import { useState } from "react";
import {
   ArrowDownCircle,
   ArrowUpCircle,
   DollarSign,
   Plus,
   Calendar,
   CheckCircle,
} from "lucide-react";
import { Header } from "../../components/Header";
import { SummaryCard } from "../../components/SummaryCard";
import { TransactionTable } from "../../components/TransactionTable";
import { NewTransactionModal } from "../../components/NewTransactionModal";
import { useTransactions } from "../../hooks/useTransactions";

export function Dashboard() {
   // Removemos 'income' e 'expense' isolados, puxando tudo de 'summary'
   const { transactions, summary, isLoading } = useTransactions();
   const [isModalOpen, setIsModalOpen] = useState(false);

   // Estado da aba: 'completed' (Histórico) ou 'pending' (Pendências)
   const [currentTab, setCurrentTab] = useState<"completed" | "pending">(
      "completed",
   );

   // Filtra as transações em memória para máxima performance
   const filteredTransactions = transactions.filter(
      (transaction) => transaction.status === currentTab,
   );

   return (
      <div className="min-h-screen bg-slate-50 pb-12">
         <Header />

         <main className="max-w-6xl mx-auto px-6">
            {/* Cards de Sumário corrigidos */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 -mt-20 mb-12">
               <SummaryCard
                  title="Entradas"
                  amount={summary?.income || 0}
                  icon={
                     <ArrowUpCircle className="text-emerald-500" size={32} />
                  }
               />
               <SummaryCard
                  title="Saídas"
                  amount={summary?.expense || 0}
                  icon={<ArrowDownCircle className="text-red-500" size={32} />}
               />
               <SummaryCard
                  title="Saldo Total"
                  amount={summary?.amount || 0}
                  icon={<DollarSign className="text-white" size={32} />}
                  variant="success"
               />
            </div>

            {/* Linha de Controle: Título, Abas e Botão Novo */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
               <div>
                  <h2 className="text-xl font-bold text-slate-800">
                     {currentTab === "completed"
                        ? "Fluxo de Caixa Realizado"
                        : "Compromissos Futuros"}
                  </h2>
               </div>

               <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
                  {/* Segmented Control (Tabs mobile-friendly) */}
                  <div className="flex bg-slate-200/80 p-1 rounded-xl shadow-inner">
                     <button
                        onClick={() => setCurrentTab("completed")}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                           currentTab === "completed"
                              ? "bg-white text-slate-800 shadow-sm"
                              : "text-slate-500 hover:text-slate-800"
                        }`}
                     >
                        <CheckCircle size={16} />
                        Histórico
                     </button>
                     <button
                        onClick={() => setCurrentTab("pending")}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                           currentTab === "pending"
                              ? "bg-white text-slate-800 shadow-sm"
                              : "text-slate-500 hover:text-slate-800"
                        }`}
                     >
                        <Calendar size={16} />
                        Pendentes
                     </button>
                  </div>

                  <button
                     onClick={() => setIsModalOpen(true)}
                     className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm whitespace-nowrap"
                  >
                     <Plus size={18} />
                     Novo Lançamento
                  </button>
               </div>
            </div>

            {/* Tabela consumindo os dados já filtrados */}
            <TransactionTable
               transactions={filteredTransactions}
               isLoading={isLoading}
            />
         </main>

         <NewTransactionModal
            isOpen={isModalOpen}
            onClose={() => setIsModalOpen(false)}
         />
      </div>
   );
}
