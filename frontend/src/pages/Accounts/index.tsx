import { useState } from "react";
import { Plus, Landmark, Wallet, PiggyBank, Building } from "lucide-react";
import { useAccounts } from "../../hooks/useAccounts";
import { AccountModal } from "../../components/AccountModal";
import { EmptyState, Skeleton } from "../../components/ui";

// Compatível com o retorno string do serviço backend
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
    badgeClass: "bg-emerald-500/10 text-emerald-500 border-emerald-500/20",
  },
  savings: {
    label: "Investimentos",
    icon: PiggyBank,
    badgeClass: "bg-amber-500/10 text-amber-500 border-amber-500/20",
  },
};

export function Accounts() {
  // Removido o isError para compatibilidade estrita com useAccounts
  const { accounts = [], isLoading } = useAccounts();
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  return (
    /* 🏛️ NAKED PAGE: O Container global comanda as larguras, respiros e responsividade */
    <div className="w-full space-y-6 sm:space-y-8">
      {/* 1. CABEÇALHO */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
            Contas & Carteiras
          </h2>
          <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
            Gerencie suas fontes de liquidez bancária e dinheiro em espécie
          </p>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
        >
          <Plus size={18} />
          <span>Nova Conta</span>
        </button>
      </div>

      {/* 2. ESTADO DE CARREGAMENTO */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-40 w-full rounded-2xl" />
          <Skeleton className="h-40 w-full rounded-2xl" />
          <Skeleton className="h-40 w-full rounded-2xl" />
        </div>
      )}

      {/* 3. LISTA DE CONTAS */}
      {!isLoading && accounts.length === 0 ? (
        <div className="py-12">
          <EmptyState
            icon={Landmark}
            title="Nenhuma conta cadastrada"
            description="Você ainda não cadastrou nenhuma conta corrente ou carteira. Cadastre a primeira para liberar os lançamentos no seu Caixa!"
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {accounts.map((acc: AccountData) => {
            // Type casting seguro sem quebrar o linter
            const typeKey = (acc.type as AccountTypeKey) || "checking";
            const config =
              ACCOUNT_TYPE_CONFIG[typeKey] ?? ACCOUNT_TYPE_CONFIG.checking;
            const IconComponent = config.icon;

            return (
              <div
                key={acc.id}
                className="card-default p-6 flex flex-col justify-between h-44 border border-subtle/30 bg-surface group transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-xl hover:shadow-black/5"
              >
                <div className="flex justify-between items-start">
                  <div className="w-12 h-12 rounded-2xl bg-elevated flex items-center justify-center text-primary group-hover:bg-brand/10 group-hover:text-brand transition-colors">
                    <IconComponent size={24} />
                  </div>

                  <span
                    className={`text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-full border ${config.badgeClass}`}
                  >
                    {config.label}
                  </span>
                </div>

                <div>
                  <h3 className="font-bold text-base sm:text-lg text-primary tracking-tight truncate">
                    {acc.name}
                  </h3>
                  <p className="text-xs text-muted font-medium mt-0.5">
                    Saldo Atual:{" "}
                    <strong className="text-primary">
                      {formatCurrency(acc.balance ?? 0)}
                    </strong>
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* MODAL DE CRIAÇÃO */}
      <AccountModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
}
