import { useState } from "react";
import { useAccounts } from "../hooks/useAccounts";
import { useCategories } from "../hooks/useCategories";
import {
  Plus,
  CreditCard,
  Building,
  ArrowUpCircle,
  ArrowDownCircle,
  Landmark,
  Tag,
} from "lucide-react";
import { AccountModal } from "../components/AccountModal";
import { CategoryModal } from "../components/CategoryModal";
import { EmptyState, Skeleton } from "../components/ui";

interface Account {
  id: string;
  name: string;
  type: "checking" | "wallet" | "savings";
}

interface Category {
  id: string;
  name: string;
  type: "income" | "expense" | "entrada" | "saida";
}

export function Settings() {
  const { accounts = [], isLoading: isLoadingAccounts } = useAccounts();
  const [isAccountModalOpen, setIsAccountModalOpen] = useState<boolean>(false);

  const { categories = [], isLoading: isLoadingCategories } = useCategories();
  const [isCategoryModalOpen, setIsCategoryModalOpen] =
    useState<boolean>(false);

  return (
    <div className="w-full pb-16 min-h-screen animate-fade-in">
      {/* Alinhamento estrutural padronizado com px-1 */}
      <main className="max-w-6xl mx-auto px-1 py-8 space-y-8">
        {/* 1. CABEÇALHO */}
        <div>
          <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
            Configurações & Administração
          </h2>
          <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
            Gerencie as suas contas, carteiras e estruture as categorias de
            fluxo
          </p>
        </div>

        {/* BLOCO 1: CONTAS E CARTEIRAS */}
        <section className="card-default p-6 sm:p-8 border-subtle/30 space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-subtle/20 pb-6">
            <div>
              <h3 className="text-lg font-bold text-primary tracking-tight flex items-center gap-2">
                <Landmark className="text-brand w-5 h-5" />
                <span>Minhas Contas & Fontes de Liquidez</span>
              </h3>
              <p className="text-xs text-muted font-medium mt-0.5">
                Locais onde o seu dinheiro ou crédito estão armazenados
              </p>
            </div>
            <button
              onClick={() => setIsAccountModalOpen(true)}
              className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-sm cursor-pointer"
            >
              <Plus size={16} />
              <span>Nova Conta</span>
            </button>
          </div>

          {isLoadingAccounts ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Skeleton className="h-20 w-full rounded-2xl" />
              <Skeleton className="h-20 w-full rounded-2xl" />
              <Skeleton className="h-20 w-full rounded-2xl" />
            </div>
          ) : accounts.length === 0 ? (
            <div className="py-6">
              <EmptyState
                icon={Building}
                title="Nenhuma conta cadastrada"
                description="Adicione a sua primeira conta corrente ou carteira para habilitar os lançamentos."
              />
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {(accounts as Account[]).map((account) => (
                <div
                  key={account.id}
                  className="flex items-center gap-3.5 p-4 border border-subtle/30 rounded-2xl bg-elevated/40 hover:bg-elevated/80 transition-all"
                >
                  <div className="w-11 h-11 bg-surface rounded-xl flex items-center justify-center text-primary shadow-2xs shrink-0 border border-subtle/20">
                    {account.type === "wallet" ? (
                      <Building size={20} className="text-brand" />
                    ) : (
                      <CreditCard size={20} className="text-brand" />
                    )}
                  </div>
                  <div className="truncate">
                    <strong className="block text-primary text-sm font-bold truncate">
                      {account.name}
                    </strong>
                    <span className="text-[10px] font-extrabold text-muted uppercase tracking-wider block mt-0.5">
                      {account.type === "checking" && "Conta Corrente"}
                      {account.type === "wallet" && "Carteira Física"}
                      {account.type === "savings" && "Investimentos"}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* BLOCO 2: CATEGORIAS */}
        <section className="card-default p-6 sm:p-8 border-subtle/30 space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-subtle/20 pb-6">
            <div>
              <h3 className="text-lg font-bold text-primary tracking-tight flex items-center gap-2">
                <Tag className="text-brand w-5 h-5" />
                <span>Categorias & Tags</span>
              </h3>
              <p className="text-xs text-muted font-medium mt-0.5">
                Classificação analítica para despesas e receitas no seu fluxo
                diário
              </p>
            </div>
            <button
              onClick={() => setIsCategoryModalOpen(true)}
              className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-sm cursor-pointer"
            >
              <Plus size={16} />
              <span>Nova Categoria</span>
            </button>
          </div>

          {isLoadingCategories ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <Skeleton className="h-14 w-full rounded-xl" />
              <Skeleton className="h-14 w-full rounded-xl" />
              <Skeleton className="h-14 w-full rounded-xl" />
              <Skeleton className="h-14 w-full rounded-xl" />
            </div>
          ) : categories.length === 0 ? (
            <div className="py-6">
              <EmptyState
                icon={Tag}
                title="Sem categorias cadastradas"
                description="Crie categorias para classificar corretamente os seus gastos e receitas."
              />
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {(categories as Category[]).map((category) => {
                const isIncome =
                  category.type === "income" || category.type === "entrada";
                return (
                  <div
                    key={category.id}
                    className="flex items-center gap-3 p-3.5 border border-subtle/30 rounded-xl bg-elevated/40 hover:bg-elevated/80 transition-all"
                  >
                    {isIncome ? (
                      <ArrowUpCircle
                        className="text-emerald-500 shrink-0"
                        size={18}
                      />
                    ) : (
                      <ArrowDownCircle
                        className="text-danger shrink-0"
                        size={18}
                      />
                    )}
                    <span className="font-semibold text-xs sm:text-sm text-primary truncate">
                      {category.name}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </main>

      {/* MODAIS */}
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
