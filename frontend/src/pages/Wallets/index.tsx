import { useState } from "react";
import { Plus, Target } from "lucide-react";
import { useWallets } from "../../hooks/useWallets";
import type { Wallet } from "../../types/wallet";

// Componentes da Página
import { WalletsSummary } from "../../components/wallets/WalletsSummary";
import { WalletCard } from "../../components/wallets/WalletCard";
import { WalletsSkeleton } from "../../components/wallets/WalletsSkeleton";

// Os 4 Modais do Ecossistema de Metas
import { CreateWalletModal } from "../../components/wallets/CreateWalletModal";
import { TransferWalletModal } from "../../components/wallets/TransferWalletModal";
import { EditWalletModal } from "../../components/wallets/EditWalletModal";
import { DeleteWalletModal } from "../../components/wallets/DeleteWalletModal";

// ✨ IMPORTAMOS O NOVO COMPONENTE EDUCACIONAL
import { FeatureIntroduction } from "../../components/ui/EmptyState/FeatureIntroduction";

export function Wallets() {
  const { data: wallets = [], isLoading } = useWallets();

  // Estados de Controle de Modais
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const [progressConfig, setProgressConfig] = useState<{
    isOpen: boolean;
    wallet: Wallet | null;
    type: "deposit" | "withdraw" | null;
  }>({
    isOpen: false,
    wallet: null,
    type: null,
  });

  const [editConfig, setEditConfig] = useState<{
    isOpen: boolean;
    wallet: Wallet | null;
  }>({
    isOpen: false,
    wallet: null,
  });

  const [deleteConfig, setDeleteConfig] = useState<{
    isOpen: boolean;
    wallet: Wallet | null;
  }>({
    isOpen: false,
    wallet: null,
  });

  // Handlers para abrir modais
  const handleOpenProgress = (wallet: Wallet, type: "deposit" | "withdraw") => {
    setProgressConfig({ isOpen: true, wallet, type });
  };

  const handleOpenEdit = (wallet: Wallet) => {
    setEditConfig({ isOpen: true, wallet });
  };

  const handleOpenDelete = (wallet: Wallet) => {
    setDeleteConfig({ isOpen: true, wallet });
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in pb-20 sm:pb-8">
      {/* 1. Header (Escondido se a página estiver vazia para dar foco à introdução) */}
      {!isLoading && wallets.length > 0 && (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Metas e Objetivos
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Guarde dinheiro com propósito e acompanhe o seu progresso.
            </p>
          </div>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold transition-all shadow-sm cursor-pointer active:scale-95"
          >
            <Plus size={18} />
            <span>Nova Meta</span>
          </button>
        </div>
      )}

      {/* 2. Conteúdo Principal */}
      {isLoading ? (
        <WalletsSkeleton />
      ) : wallets.length === 0 ? (
        // 🚀 O NOVO ONBOARDING EDUCACIONAL ENTRA AQUI
        <FeatureIntroduction
          icon={Target}
          title="Metas e Objetivos"
          subtitle="Transforme os seus sonhos em planos reais"
          whatIs="Metas ajudam você a guardar dinheiro com um propósito específico. Em vez de deixar o dinheiro solto na conta correndo o risco de ser gasto, você separa virtualmente o valor, acompanha o progresso e protege os seus sonhos."
          examples={[
            {
              label: "Reserva de Emergência",
              category: "Segurança Financeira",
            },
            { label: "Viagem de Férias", category: "Lazer e Experiências" },
            { label: "Troca do Carro", category: "Bens Materiais" },
            {
              label: "Novo Setup / Notebook",
              category: "Trabalho e Tecnologia",
            },
          ]}
          tip="Definir uma data limite para a sua meta ajuda o Fluxa a calcular automaticamente quanto você precisa poupar por mês para chegar lá a tempo."
          actionLabel="Criar a minha primeira Meta"
          onAction={() => setIsCreateOpen(true)}
        />
      ) : (
        <div className="space-y-6 sm:space-y-8">
          <WalletsSummary wallets={wallets} />

          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
            {wallets.map((wallet) => (
              <WalletCard
                key={wallet.id}
                wallet={wallet}
                onProgress={handleOpenProgress}
                onEdit={handleOpenEdit}
                onDelete={handleOpenDelete}
              />
            ))}
          </div>
        </div>
      )}

      {/* 3. Renderização dos Modais */}
      <CreateWalletModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
      />

      <TransferWalletModal
        isOpen={progressConfig.isOpen}
        onClose={() => setProgressConfig({ ...progressConfig, isOpen: false })}
        wallet={progressConfig.wallet}
        type={progressConfig.type}
      />

      <EditWalletModal
        isOpen={editConfig.isOpen}
        onClose={() => setEditConfig({ ...editConfig, isOpen: false })}
        wallet={editConfig.wallet}
      />

      <DeleteWalletModal
        isOpen={deleteConfig.isOpen}
        onClose={() => setDeleteConfig({ ...deleteConfig, isOpen: false })}
        wallet={deleteConfig.wallet}
      />
    </div>
  );
}
