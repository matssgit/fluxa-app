import { useState } from "react";
import { Plus, Target } from "lucide-react";
import { useWallets } from "../../hooks/useWallets";
import type { Wallet } from "../../types/wallet";
import { EmptyState } from "../../components/ui";
import {
  WalletsSkeleton,
  WalletsSummary,
  WalletCard,
} from "../../components/wallets";

// Nossos modais com infraestrutura fluida
import { CreateWalletModal } from "./CreateWalletModal";
import { TransferWalletModal } from "./TransferWalletModal";

export function Wallets() {
  const { data: wallets = [], isLoading, isError } = useWallets();

  const [isNewWalletModalOpen, setIsNewWalletModalOpen] =
    useState<boolean>(false);
  const [selectedWallet, setSelectedWallet] = useState<Wallet | null>(null);
  const [transferType, setTransferType] = useState<
    "deposit" | "withdraw" | null
  >(null);
  const [isTransferModalOpen, setIsTransferModalOpen] =
    useState<boolean>(false);

  function handleOpenTransfer(
    wallet: Wallet,
    type: "deposit" | "withdraw",
  ): void {
    setSelectedWallet(wallet);
    setTransferType(type);
    setIsTransferModalOpen(true);
  }

  function handleCloseTransfer(): void {
    setIsTransferModalOpen(false);
    setSelectedWallet(null);
    setTransferType(null);
  }

  return (
    <>
      {/* 🏛️ NOVA ARQUITETURA (Naked Page): O DefaultLayout já gerencia o max-w, paddings, áreas seguras e animações */}
      <div className="w-full space-y-6 sm:space-y-8">
        {/* CABEÇALHO */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
              Minhas Metas
            </h2>
            <p className="text-muted text-sm font-medium mt-0.5">
              Acompanhe o progresso das suas reservas e objetivos financeiros
            </p>
          </div>
          <button
            onClick={() => setIsNewWalletModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <Plus size={18} />
            <span>Nova Meta</span>
          </button>
        </div>

        {isLoading && <WalletsSkeleton />}

        {isError && !isLoading && (
          <div className="py-12">
            <EmptyState
              icon={Target}
              title="Não foi possível consultar as suas reservas"
              description="Ocorreu uma instabilidade momentânea. Por favor, tente recarregar a página em instantes."
            />
          </div>
        )}

        {!isLoading && !isError && wallets.length === 0 && (
          <div className="py-12">
            <EmptyState
              icon={Target}
              title="Você ainda não possui nenhuma meta cadastrada"
              description="Crie seu primeiro objetivo financeiro para organizar reservas e acompanhar sua evolução ao longo do tempo."
            />
          </div>
        )}

        {/* COCKPIT DE METAS */}
        {!isLoading && !isError && wallets.length > 0 && (
          <div className="space-y-8">
            <WalletsSummary wallets={wallets} />

            <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {wallets.map((wallet) => (
                <WalletCard
                  key={wallet.id}
                  wallet={wallet}
                  onTransfer={handleOpenTransfer}
                />
              ))}
            </section>
          </div>
        )}
      </div>

      <CreateWalletModal
        isOpen={isNewWalletModalOpen}
        onClose={() => setIsNewWalletModalOpen(false)}
      />

      <TransferWalletModal
        isOpen={isTransferModalOpen}
        onClose={handleCloseTransfer}
        wallet={selectedWallet}
        type={transferType}
      />
    </>
  );
}
