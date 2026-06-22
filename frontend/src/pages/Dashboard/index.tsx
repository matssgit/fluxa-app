import { ArrowDownCircle, ArrowUpCircle, DollarSign } from "lucide-react";
import { Header } from "../../components/Header";
import { SummaryCard } from "../../components/SummaryCard";
import { TransactionForm } from "../../components/TransactionForm";
import { TransactionTable } from "../../components/TransactionTable";
import { useTransactions } from "../../hooks/useTransactions";

export function Dashboard() {
   const { transactions, summary, income, expense, isLoading } =
      useTransactions();

   return (
      <div className="min-h-screen bg-slate-50 pb-12">
         <Header />

         <main className="max-w-6xl mx-auto px-6">
            {/* Sumário sobrepondo o Header */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 -mt-20 mb-12">
               <SummaryCard
                  title="Entradas"
                  amount={income}
                  icon={
                     <ArrowUpCircle className="text-emerald-500" size={32} />
                  }
               />
               <SummaryCard
                  title="Saídas"
                  amount={expense}
                  icon={<ArrowDownCircle className="text-red-500" size={32} />}
               />
               <SummaryCard
                  title="Saldo Total"
                  amount={summary?.amount || 0}
                  icon={<DollarSign className="text-white" size={32} />}
                  variant="success"
               />
            </div>

            <TransactionForm />

            <TransactionTable
               transactions={transactions}
               isLoading={isLoading}
            />
         </main>
      </div>
   );
}
