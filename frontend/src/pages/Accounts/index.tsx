import { useState } from "react";
import { Skeleton } from "../../components/ui";
import { useAccounts } from "../../hooks/useAccounts";
import { AccountModal } from "../../components/features/accounts/AccountModal";
import { DeleteAccountModal } from "../../components/features/accounts/DeleteAccountModal";
import { FeatureIntroduction } from "../../components/ui/EmptyState/FeatureIntroduction";
import {
  Plus,
  Landmark,
  Wallet,
  PiggyBank,
  Building,
  Pencil,
  Trash2,
} from "lucide-react";

interface AccountData {
  id: string;
  name: string;
  type: string;
  balance?: number;
}

type AccountTypeKey = "checking" | "wallet" | "savings";

const ACCOUNT_TYPE_CONFIG: Record<
  AccountTypeKey,
  { label: string; icon: typeof Landmark; badgeClass: string }
> = {
  checking: {
    label: "Conta Corrente",
    icon: Building,
    badgeClass: "bg-brand/10 text-brand border-brand/20",
  },
  wallet: {
    label: "Carteira Física",
    icon: Wallet,
    badgeClass: "bg-income-soft text-income border-income/20",
  },
  savings: {
    label: "Investimentos",
    icon: PiggyBank,
    badgeClass: "bg-amber-500/10 text-amber-500 border-amber-500/20",
  },
};

export function Accounts() {
  const { accounts = [], isLoading } = useAccounts();

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState<boolean>(false);
  const [selectedAccount, setSelectedAccount] = useState<AccountData | null>(
    null,
  );

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  function handleCreate() {
    setSelectedAccount(null);
    setIsModalOpen(true);
  }

  function handleEdit(acc: AccountData) {
    setSelectedAccount(acc);
    setIsModalOpen(true);
  }

  function handleDelete(acc: AccountData) {
    setSelectedAccount(acc);
    setIsDeleteModalOpen(true);
  }

  return (
    <div className="w-full space-y-6 sm:space-y-8 animate-fade-in">
      {accounts.length > 0 && !isLoading && (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Contas
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Faça a gestão dos seus saldos e patrimónios.
            </p>
          </div>
          <button
            onClick={handleCreate}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer active:scale-95"
          >
            <Plus size={18} />
            <span>Nova Conta</span>
          </button>
        </div>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
          <Skeleton className="h-44 w-full rounded-2xl" />
          <Skeleton className="h-44 w-full rounded-2xl" />
          <Skeleton className="h-44 w-full rounded-2xl" />
        </div>
      )}

      {!isLoading && accounts.length === 0 ? (
        <FeatureIntroduction
          icon={Landmark}
          title="Contas"
          subtitle="Onde o seu dinheiro mora"
          whatIs="Uma conta representa o lugar onde o seu dinheiro vive fisicamente ou digitalmente. Ela reflete o seu saldo real. Toda a movimentação financeira no Fluxa precisa estar ligada a uma conta."
          examples={[
            { label: "Conta Corrente", category: "Nubank, Itaú, Santander" },
            {
              label: "Carteira Física",
              category: "Dinheiro vivo (notas e moedas)",
            },
            {
              label: "Investimentos",
              category: "Cora, Poupança, Tesouro Direto",
            },
          ]}
          tip="Para que o seu Caixa seja preciso, cadastre as suas contas com os saldos exatos que tem hoje."
          actionLabel="Criar a minha primeira Conta"
          onAction={handleCreate}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
          {accounts.map((acc: AccountData, index: number) => {
            const typeKey = (acc.type as AccountTypeKey) || "checking";
            const config =
              ACCOUNT_TYPE_CONFIG[typeKey] ?? ACCOUNT_TYPE_CONFIG.checking;
            const IconComponent = config.icon;

            return (
              <div
                key={acc.id}
                className={`relative overflow-hidden p-6 sm:p-7 flex flex-col justify-between min-h-56 border border-border bg-surface group transition-all duration-300 hover:shadow-md ${
                  index % 2 === 0
                    ? "md:col-span-7 rounded-tl-[2.5rem] rounded-br-[2.5rem] rounded-tr-xl rounded-bl-xl"
                    : "md:col-span-5 rounded-tr-[2.5rem] rounded-bl-[2.5rem] rounded-tl-xl rounded-br-xl"
                }`}
              >
                <div className="absolute -right-12 -bottom-16 w-40 h-40 rounded-full border-[24px] border-brand/4 pointer-events-none" />
                <div>
                  <div className="flex justify-between items-start mb-4">
                    <div className="w-12 h-12 rounded-[1.15rem_1.15rem_1.15rem_0.35rem] bg-elevated flex items-center justify-center text-primary group-hover:bg-brand group-hover:text-white dark:group-hover:text-[#102f29] transition-colors">
                      <IconComponent size={24} />
                    </div>
                    <span
                      className={`text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-full border ${config.badgeClass}`}
                    >
                      {config.label}
                    </span>
                  </div>

                  <div className="mt-7">
                    <span className="text-[10px] font-extrabold uppercase tracking-[0.18em] text-muted block mb-1">
                      Saldo atual
                    </span>
                    <strong className="block text-3xl sm:text-4xl font-extrabold text-primary tracking-[-0.055em] tabular-nums">
                      {formatCurrency(acc.balance ?? 0)}
                    </strong>
                    <h3 className="font-bold text-sm text-secondary tracking-tight truncate mt-3 group-hover:text-brand transition-colors">
                      {acc.name}
                    </h3>
                  </div>
                </div>

                <div className="relative pt-4 mt-4 border-t border-subtle flex items-center justify-end">
                  <div className="flex items-center gap-1 bg-elevated/60 p-1 rounded-xl border border-subtle/20">
                    <button
                      onClick={() => handleEdit(acc)}
                      title="Editar conta"
                      className="p-1.5 text-muted hover:text-brand hover:bg-surface rounded-lg transition-all cursor-pointer"
                    >
                      <Pencil size={15} />
                    </button>
                    <button
                      onClick={() => handleDelete(acc)}
                      title="Excluir conta"
                      className="p-1.5 text-muted hover:text-expense hover:bg-expense-soft rounded-lg transition-all cursor-pointer"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <AccountModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        account={
          selectedAccount
            ? {
                ...selectedAccount,
                type: selectedAccount.type as "checking" | "wallet" | "savings",
              }
            : null
        }
      />

      <DeleteAccountModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        account={selectedAccount}
      />
    </div>
  );
}
