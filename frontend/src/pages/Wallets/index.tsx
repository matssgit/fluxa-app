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
import { EmptyState } from "../../components/ui/EmptyState";

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
      {/* 1. Header (Mantido minimalista conforme o design system) */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
        <button
          onClick={() => setIsCreateOpen(true)}
          className="w-full sm:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary hover:bg-primary/90 text-surface text-sm font-bold transition-all shadow-sm cursor-pointer active:scale-95"
        >
          <Plus size={18} />
          <span>Nova Meta</span>
        </button>
      </div>

      {/* 2. Conteúdo Principal */}
      {isLoading ? (
        <WalletsSkeleton />
      ) : wallets.length === 0 ? (
        <EmptyState
          title="Nenhuma meta definida"
          description="Que tal planear a sua próxima viagem, a reserva de emergência ou o novo setup?"
          icon={Target}
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
