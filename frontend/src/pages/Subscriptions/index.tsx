import { useState } from "react";
import { Plus, Repeat } from "lucide-react";
import { useSubscriptions } from "../../hooks/useSubscriptions";
import { type Subscription } from "../../types/subscription";
import { CreateSubscriptionModal } from "./CreateSubscriptionModal";
import { EmptyState } from "../../components/ui";

// Reaproveitando o Modal Atômico existente no sistema
import { PaySubscriptionModal } from "../../components/PaySubscriptionModal";

// Consumindo o Barrel File dos componentes de domínio
import {
  SubscriptionsSkeleton,
  SubscriptionsSummary,
  SubscriptionCard,
} from "../../components/subscriptions";

export function Subscriptions() {
  const { data: subscriptions = [], isLoading, isError } = useSubscriptions();

  // Controle do Modal de Criação
  const [isNewSubModalOpen, setIsNewSubModalOpen] = useState<boolean>(false);

  // Controle do Modal Atômico de Baixa Mensal
  const [isPayModalOpen, setIsPayModalOpen] = useState<boolean>(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  function handleOpenPayModal(id: string): void {
    setSelectedSubId(id);
    setIsPayModalOpen(true);
  }

  return (
    <div className="w-full pb-16 min-h-screen animate-fade-in">
      {/* Alinhamento estrito preservado com px-1 */}
      <main className="max-w-6xl mx-auto px-1 py-8 space-y-8">
        {/* 1. CABEÇALHO */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
              Minhas Assinaturas
            </h2>
            <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
              Monitorize os seus serviços recorrentes, compromissos fixos e
              cobranças periódicas
            </p>
          </div>
          <button
            onClick={() => setIsNewSubModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <Plus size={18} />
            <span>Nova Assinatura</span>
          </button>
        </div>

        {/* 2. ESTADO DE CARREGAMENTO */}
        {isLoading && <SubscriptionsSkeleton />}

        {/* 3. ESTADO DE ERRO */}
        {isError && !isLoading && (
          <div className="py-12">
            <EmptyState
              icon={Repeat}
              title="Não foi possível carregar as assinaturas"
              description="Ocorreu um erro ao consultar os seus serviços fixos. Por favor, atualize a página em instantes."
            />
          </div>
        )}

        {/* 4. ESTADO VAZIO */}
        {!isLoading && !isError && subscriptions.length === 0 && (
          <div className="py-12">
            <EmptyState
              icon={Repeat}
              title="Nenhuma assinatura ativa"
              description="Você ainda não cadastrou nenhum serviço recorrente. Cadastre o primeiro para liberar a telemetria e monitorar os seus custos fixos!"
            />
          </div>
        )}

        {/* 5. COCKPIT PREMIUM: BANNER DE KPIS + CARDS METALIZADOS */}
        {!isLoading && !isError && subscriptions.length > 0 && (
          <div className="space-y-8">
            {/* Banner de Telemetria Específica */}
            <SubscriptionsSummary
              subscriptions={subscriptions as Subscription[]}
            />

            {/* Grelha de Cartões Metalizados */}
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

      {/* MODAL DE CRIAÇÃO */}
      <CreateSubscriptionModal
        isOpen={isNewSubModalOpen}
        onClose={() => setIsNewSubModalOpen(false)}
      />

      {/* MODAL ATÔMICO DE BAIXA MENSAL */}
      <PaySubscriptionModal
        isOpen={isPayModalOpen}
        onClose={() => setIsPayModalOpen(false)}
        subscriptionId={selectedSubId}
      />
    </div>
  );
}
