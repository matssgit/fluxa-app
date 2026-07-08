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

// Nossos 2 novos modais de interatividade
import { CreateWalletModal } from "./CreateWalletModal";
import { TransferWalletModal } from "./TransferWalletModal";

export function Wallets() {
  const { data: wallets = [], isLoading, isError } = useWallets();

  // Estado do Modal de Plantio (Criação)
  const [isNewWalletModalOpen, setIsNewWalletModalOpen] =
    useState<boolean>(false);

  // Estados do Modal de Aporte / Resgate
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
    <div className="w-full pb-16 min-h-screen animate-fade-in">
      <main className="max-w-6xl mx-auto px-1 py-8 space-y-8">
        {/* CABEÇALHO */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
              Minhas Metas
            </h2>
            <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
              Acompanhe o progresso das suas reservas e objetivos financeiros
            </p>
          </div>
          <button
            onClick={() => setIsNewWalletModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <Plus size={18} />
            <span>Nova Meta</span>
          </button>
        </div>

        {/* ESTADO DE CARREGAMENTO */}
        {isLoading && <WalletsSkeleton />}

        {/* ESTADO DE ERRO */}
        {isError && !isLoading && (
          <div className="py-12">
            <EmptyState
              icon={Target}
              title="Não foi possível consultar as suas estufas"
              description="Ocorreu uma instabilidade momentânea na conexão com o solo. Por favor, tente recarregar a página em instantes."
            />
          </div>
        )}

        {/* ESTADO VAZIO (UX Writing Limpo e Profissional) */}
        {!isLoading && !isError && wallets.length === 0 && (
          <div className="py-12">
            <EmptyState
              icon={Target}
              title="Você ainda não possui nenhuma meta cadastrada"
              description="Crie seu primeiro objetivo financeiro para organizar reservas e acompanhar sua evolução ao longo do tempo."
            />
          </div>
        )}

        {/* COCKPIT PREMIUM: BANNER DE KPIS + CARDS COM BARRA DE CULTIVO */}
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
      </main>

      {/* MODAL DE CRIAÇÃO (PLANTIO) */}
      <CreateWalletModal
        isOpen={isNewWalletModalOpen}
        onClose={() => setIsNewWalletModalOpen(false)}
      />

      {/* MODAL DE APORTE / RESGATE (NUTRIR VS COLHER) */}
      <TransferWalletModal
        isOpen={isTransferModalOpen}
        onClose={handleCloseTransfer}
        wallet={selectedWallet}
        type={transferType}
      />
    </div>
  );
}
