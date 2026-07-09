import { useState } from "react";
import { Plus, Repeat } from "lucide-react";
import { useSubscriptions } from "../../hooks/useSubscriptions";
import { type Subscription } from "../../types/subscription";
import { CreateSubscriptionModal } from "./CreateSubscriptionModal";
import { EmptyState } from "../../components/ui";
import { PaySubscriptionModal } from "../../components/subscriptions/PaySubscriptionModal";
import {
  SubscriptionsSkeleton,
  SubscriptionsSummary,
  SubscriptionCard,
} from "../../components/subscriptions";

export function Subscriptions() {
  const { data: subscriptions = [], isLoading, isError } = useSubscriptions();
  const [isNewSubModalOpen, setIsNewSubModalOpen] = useState<boolean>(false);
  const [isPayModalOpen, setIsPayModalOpen] = useState<boolean>(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  function handleOpenPayModal(id: string): void {
    setSelectedSubId(id);
    setIsPayModalOpen(true);
  }

  return (
    <div className="w-full min-h-screen animate-fade-in">
      {/* ✨ PADRONIZAÇÃO OFICIAL: max-w-7xl + pb-28 garantem alinhamento perfeito com Caixa e Cartões */}
      <main className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-8 pb-28 sm:pb-16">
        {/* 1. CABEÇALHO REFINADO PINE & SAGE */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-subtle/15 pb-6">
          <div className="flex items-center gap-3.5">
            <div className="w-11 h-11 rounded-2xl bg-brand/10 border border-brand/20 flex items-center justify-center text-brand shrink-0 shadow-2xs">
              <Repeat size={22} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
                  Minhas Assinaturas
                </h2>
                <span className="w-1.5 h-1.5 rounded-full bg-brand hidden sm:inline-block" />
              </div>
              <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
                Monitorize os seus serviços recorrentes, compromissos fixos e
                cobranças periódicas
              </p>
            </div>
          </div>
          <button
            onClick={() => setIsNewSubModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <Plus size={18} />
            <span>Nova Assinatura</span>
          </button>
        </div>

        {/* 2. ESTADOS DO SISTEMA */}
        {isLoading && <SubscriptionsSkeleton />}

        {isError && !isLoading && (
          <div className="py-12">
            <EmptyState
              icon={Repeat}
              title="Não foi possível consultar os seus serviços"
              description="Ocorreu uma instabilidade momentânea. Por favor, tente recarregar a página."
            />
          </div>
        )}

        {!isLoading && !isError && subscriptions.length === 0 && (
          <div className="py-12">
            <EmptyState
              icon={Repeat}
              title="Nenhuma assinatura ativa no seu ecossistema"
              description="Seu solo está livre de cobranças fixas por enquanto. Cadastre a primeira para ativar o acompanhamento de custos!"
            />
          </div>
        )}

        {!isLoading && !isError && subscriptions.length > 0 && (
          <div className="space-y-8">
            <SubscriptionsSummary
              subscriptions={subscriptions as Subscription[]}
            />

            <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {(subscriptions as Subscription[]).map((sub) => (
                <SubscriptionCard
                  key={sub.id}
                  subscription={sub}
                  onPay={handleOpenPayModal}
                />
              ))}
            </section>
          </div>
        )}
      </main>

      <CreateSubscriptionModal
        isOpen={isNewSubModalOpen}
        onClose={() => setIsNewSubModalOpen(false)}
      />

      <PaySubscriptionModal
        isOpen={isPayModalOpen}
        onClose={() => setIsPayModalOpen(false)}
        subscriptionId={selectedSubId}
      />
    </div>
  );
}
