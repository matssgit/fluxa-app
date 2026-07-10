import { useState } from "react";
import { Plus, Repeat, AlertCircle, TrendingUp, Wallet, PieChart } from "lucide-react";
import { useSubscriptions, useSubscriptionAnalytics } from "../../hooks/useSubscriptions";
import { type Subscription } from "../../types/subscription";
import { CreateSubscriptionModal } from "./CreateSubscriptionModal";
import { EmptyState } from "../../components/ui";
import { PaySubscriptionModal } from "../../components/subscriptions/PaySubscriptionModal";
import {
  SubscriptionsSkeleton,
  SubscriptionCard,
} from "../../components/subscriptions";

export function Subscriptions() {
  const { data: subscriptions = [], isLoading, isError } = useSubscriptions();
  
  // ✨ AQUI: Chamando nosso novo motor de BI Real!
  const { data: analytics, isLoading: isLoadingAnalytics } = useSubscriptionAnalytics();

  const [isNewSubModalOpen, setIsNewSubModalOpen] = useState<boolean>(false);
  const [isPayModalOpen, setIsPayModalOpen] = useState<boolean>(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  function handleOpenPayModal(id: string): void {
    setSelectedSubId(id);
    setIsPayModalOpen(true);
  }

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(val);

  const isPageLoading = isLoading || isLoadingAnalytics;

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in">
        
        {/* CABEÇALHO */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
              Centro de Assinaturas
            </h2>
            <p className="text-muted text-sm font-medium mt-0.5">
              Gestão de compromissos recorrentes e impacto anual
            </p>
          </div>
          <button
            onClick={() => setIsNewSubModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <Plus size={18} />
            <span>Adicionar Serviço</span>
          </button>
        </div>

        {/* ESTADOS DE CARGA E ERRO */}
        {isPageLoading && <SubscriptionsSkeleton />}
        
        {isError && !isPageLoading && (
          <div className="py-12">
            <EmptyState
              icon={AlertCircle}
              title="Não foi possível consultar os seus serviços"
              description="Ocorreu uma instabilidade momentânea. Por favor, tente recarregar a página."
            />
          </div>
        )}

        {/* VISÃO ANALÍTICA COM DADOS REAIS DO BACKEND */}
        {!isPageLoading && !isError && subscriptions.length > 0 && analytics && (
          <div className="space-y-8">
            
            {/* COCKPIT DE IMPACTO FINANCEIRO */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              {/* O NÚMERO QUE ASSUSTA (Impacto Anual) */}
              <div className="lg:col-span-2 card-default p-6 sm:p-8 flex flex-col justify-between border-l-4 border-l-brand bg-linear-to-br from-surface to-brand/5 relative overflow-hidden">
                <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-brand/10 rounded-full blur-3xl pointer-events-none" />
                
                <div className="z-10 flex justify-between items-start">
                  <div>
                    <span className="inline-flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-widest px-2.5 py-1 rounded-full bg-brand/10 text-brand border border-brand/20 mb-3">
                      <TrendingUp size={12} /> Impacto Projetado
                    </span>
                    <h3 className="text-sm font-bold text-primary">Custo Anual Recorrente</h3>
                  </div>
                </div>
                
                <div className="z-10 mt-6 flex items-end gap-4">
                  <span className="text-4xl sm:text-5xl font-black text-primary tracking-tighter">
                    {formatCurrency(analytics.yearlyProjection)}
                  </span>
                  <span className="text-sm font-medium text-muted mb-2">/ ano</span>
                </div>
                
                <p className="z-10 text-xs sm:text-sm font-medium text-secondary mt-4">
                  O cancelamento de serviços não utilizados pode gerar uma economia direta neste montante.
                </p>
              </div>

              {/* KPIS RÁPIDOS */}
              <div className="flex flex-col gap-4">
                <div className="flex-1 card-default p-5 flex items-center gap-4">
                  <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
                    <Wallet size={24} />
                  </div>
                  <div>
                    <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">Gasto Mensal Fixo</span>
                    <span className="text-xl font-extrabold text-primary">{formatCurrency(analytics.monthlyTotal)}</span>
                  </div>
                </div>

                <div className="flex-1 card-default p-5 flex items-center gap-4">
                  <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
                    <PieChart size={24} />
                  </div>
                  <div>
                    <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">Peso no Orçamento</span>
                    <span className="text-xl font-extrabold text-primary">{analytics.budgetImpact}% da renda</span>
                  </div>
                </div>
              </div>
            </div>

            {/* RADAR E LISTAGEM DE SERVIÇOS */}
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-bold text-primary uppercase tracking-wider">
                  Meus Compromissos ({subscriptions.length})
                </h3>
                {analytics.upcomingNext7Days > 0 && (
                  <span className="text-xs font-bold text-amber-500 bg-amber-500/10 px-3 py-1.5 rounded-lg border border-amber-500/20 shadow-sm">
                    {analytics.upcomingNext7Days} vencimentos em até 7 dias
                  </span>
                )}
              </div>
              
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

          </div>
        )}

        {/* ESTADO VAZIO */}
        {!isPageLoading && !isError && subscriptions.length === 0 && (
          <div className="py-12">
            <EmptyState
              icon={Repeat}
              title="Nenhum compromisso financeiro mapeado"
              description="Cadastre seus serviços de streaming, softwares e mensalidades para analisar o impacto anual."
            />
          </div>
        )}
      </div>

      <CreateSubscriptionModal
        isOpen={isNewSubModalOpen}
        onClose={() => setIsNewSubModalOpen(false)}
      />

      <PaySubscriptionModal
        isOpen={isPayModalOpen}
        onClose={() => setIsPayModalOpen(false)}
        subscriptionId={selectedSubId}
      />
    </>
  );
}