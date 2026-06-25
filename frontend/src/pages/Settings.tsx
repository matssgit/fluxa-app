import { useState } from "react";
import { Header } from "../components/Header";
import { useAccounts } from "../hooks/useAccounts";
import { useCategories } from "../hooks/useCategories"; // Importado
import {
   PlusCircle,
   CreditCard,
   Building,
   ArrowUpCircle,
   ArrowDownCircle,
} from "lucide-react"; // Importei novos ícones
import { AccountModal } from "../components/AccountModal";
import { CategoryModal } from "../components/CategoryModal"; // Importado

export function Settings() {
   // Hooks das Contas
   const { accounts, isLoading: isLoadingAccounts } = useAccounts();
   const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);

   // Hooks das Categorias
   const { categories, isLoading: isLoadingCategories } = useCategories();
   const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

   return (
      <div className="min-h-screen bg-slate-50 pb-12">
         <Header />

         <main className="max-w-6xl mx-auto px-6 -mt-20 relative z-10 space-y-8">
            {/* BLOCO: CONTAS */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
               <div className="flex justify-between items-center mb-6">
                  <div>
                     <h3 className="text-xl font-bold text-slate-800">
                        Minhas Contas
                     </h3>
                     <p className="text-sm text-slate-500">
                        Gerencie onde seu dinheiro está guardado.
                     </p>
                  </div>
                  <button
                     onClick={() => setIsAccountModalOpen(true)}
                     className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2 rounded-xl text-sm font-medium transition-colors"
                  >
                     <PlusCircle size={18} />
                     Nova Conta
                  </button>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {isLoadingAccounts ? (
                     <p className="text-slate-500 text-sm">
                        Carregando contas...
                     </p>
                  ) : accounts.length === 0 ? (
                     <p className="text-slate-500 text-sm italic">
                        Nenhuma conta cadastrada.
                     </p>
                  ) : (
                     accounts.map((account) => (
                        <div
                           key={account.id}
                           className="flex items-center gap-4 p-4 border border-slate-200 rounded-xl bg-slate-50"
                        >
                           <div className="p-3 bg-white rounded-lg shadow-sm">
                              {account.type === "wallet" ? (
                                 <Building
                                    className="text-slate-700"
                                    size={24}
                                 />
                              ) : (
                                 <CreditCard
                                    className="text-slate-700"
                                    size={24}
                                 />
                              )}
                           </div>
                           <div>
                              <strong className="block text-slate-800">
                                 {account.name}
                              </strong>
                              <span className="text-xs text-slate-500 uppercase tracking-wider">
                                 {account.type === "checking" &&
                                    "Conta Corrente"}
                                 {account.type === "wallet" && "Carteira"}
                                 {account.type === "savings" && "Poupança"}
                              </span>
                           </div>
                        </div>
                     ))
                  )}
               </div>
            </div>

            {/* BLOCO: CATEGORIAS */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
               <div className="flex justify-between items-center mb-6">
                  <div>
                     <h3 className="text-xl font-bold text-slate-800">
                        Categorias
                     </h3>
                     <p className="text-sm text-slate-500">
                        Organize seus lançamentos por tipo de gasto/receita.
                     </p>
                  </div>
                  <button
                     onClick={() => setIsCategoryModalOpen(true)}
                     className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2 rounded-xl text-sm font-medium transition-colors"
                  >
                     <PlusCircle size={18} />
                     Nova Categoria
                  </button>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  {isLoadingCategories ? (
                     <p className="text-slate-500 text-sm">
                        Carregando categorias...
                     </p>
                  ) : categories.length === 0 ? (
                     <p className="text-slate-500 text-sm italic">
                        Nenhuma categoria cadastrada.
                     </p>
                  ) : (
                     categories.map((category) => (
                        <div
                           key={category.id}
                           className="flex items-center gap-3 p-3 border border-slate-200 rounded-xl"
                        >
                           {category.type === "income" ? (
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
                           <span className="font-medium text-slate-700">
                              {category.name}
                           </span>
                        </div>
                     ))
                  )}
               </div>
            </div>
         </main>

         {/* MODAIS INVISÍVEIS (Só aparecem quando chamados) */}
         <AccountModal
            isOpen={isAccountModalOpen}
            onClose={() => setIsAccountModalOpen(false)}
         />
         <CategoryModal
            isOpen={isCategoryModalOpen}
            onClose={() => setIsCategoryModalOpen(false)}
         />
      </div>
   );
}
