import { useState } from "react";
import {
  Plus,
  Repeat,
  AlertCircle,
  TrendingUp,
  Wallet,
  PieChart,
} from "lucide-react";
import {
  useSubscriptions,
  useSubscriptionAnalytics,
} from "../../hooks/useSubscriptions";
import { type Subscription } from "../../types/subscription";
import { CreateSubscriptionModal } from "./CreateSubscriptionModal";
import { PaySubscriptionModal } from "../../components/subscriptions/PaySubscriptionModal";
import {
  SubscriptionsSkeleton,
  SubscriptionCard,
} from "../../components/subscriptions";

// ✨ IMPORTAMOS O NOVO COMPONENTE EDUCACIONAL
import { FeatureIntroduction } from "../../components/ui/EmptyState/FeatureIntroduction";

export function Subscriptions() {
  const { data: subscriptions = [], isLoading, isError } = useSubscriptions();
  const { data: analytics, isLoading: isLoadingAnalytics } =
    useSubscriptionAnalytics();

  const [isNewSubModalOpen, setIsNewSubModalOpen] = useState<boolean>(false);
  const [isPayModalOpen, setIsPayModalOpen] = useState<boolean>(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  function handleOpenPayModal(id: string): void {
    setSelectedSubId(id);
    setIsPayModalOpen(true);
  }

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  const isPageLoading = isLoading || isLoadingAnalytics;

  return (
    <>
      <div className="w-full space-y-6 sm:space-y-8 animate-fade-in">
        {/* CABEÇALHO (Exibido apenas se já existirem assinaturas) */}
        {!isPageLoading && subscriptions.length > 0 && (
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
                Assinaturas
              </h1>
              <p className="text-xs sm:text-sm font-medium text-muted mt-1">
                Acompanhe e controle os seus gastos recorrentes.
              </p>
            </div>
            <button
              onClick={() => setIsNewSubModalOpen(true)}
              className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-5 py-2.5 rounded-xl text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer active:scale-95"
            >
              <Plus size={18} />
              <span>Adicionar Serviço</span>
            </button>
          </div>
        )}

        {/* ESTADOS DE CARGA E ERRO */}
        {isPageLoading && <SubscriptionsSkeleton />}

        {isError && !isPageLoading && (
          <div className="py-12 flex flex-col items-center justify-center text-center">
            <AlertCircle size={48} className="text-red-500 mb-4 opacity-80" />
            <h2 className="text-lg font-bold text-primary mb-2">
              Não foi possível consultar os seus serviços
            </h2>
            <p className="text-sm text-muted">
              Ocorreu uma instabilidade momentânea. Por favor, tente recarregar
              a página.
            </p>
          </div>
        )}

        {/* 🚀 O NOVO ONBOARDING EDUCACIONAL ENTRA AQUI */}
        {!isPageLoading && !isError && subscriptions.length === 0 ? (
          <FeatureIntroduction
            icon={Repeat}
            title="Assinaturas e Recorrências"
            subtitle="O controlo inteligente das suas mensalidades"
            whatIs="Assinaturas são cobranças automáticas mensais ou anuais. Ao cadastrá-las, o Fluxa analisa o impacto desses serviços no seu orçamento e projeta o verdadeiro custo anual, ajudando a identificar o que deve ser cancelado."
            examples={[
              {
                label: "Netflix / Spotify / Prime",
                category: "Streaming e Lazer",
              },
              { label: "Academia / Natação", category: "Saúde e Bem-estar" },
              {
                label: "iCloud / Google Drive / Adobe",
                category: "Software e Cloud",
              },
              { label: "Mensalidade Escolar / Cursos", category: "Educação" },
            ]}
            tip="Assinaturas esquecidas são as maiores vilãs do dinheiro invisível. O sistema calculará instantaneamente o seu peso real ao ano."
            actionLabel="Cadastrar a minha primeira Assinatura"
            onAction={() => setIsNewSubModalOpen(true)}
          />
        ) : (
          /* VISÃO ANALÍTICA COM DADOS REAIS DO BACKEND */
          !isPageLoading &&
          !isError &&
          subscriptions.length > 0 &&
          analytics && (
            <div className="space-y-8">
              {/* COCKPIT DE IMPACTO FINANCEIRO */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* O NÚMERO QUE ASSUSTA (Impacto Anual) */}
                <div className="lg:col-span-2 card-default p-6 sm:p-8 flex flex-col justify-between border-l-4 border-l-brand bg-linear-to-br from-surface to-brand/5 relative overflow-hidden rounded-3xl">
                  <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-brand/10 rounded-full blur-3xl pointer-events-none" />

                  <div className="z-10 flex justify-between items-start">
                    <div>
                      <span className="inline-flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-widest px-2.5 py-1 rounded-full bg-brand/10 text-brand border border-brand/20 mb-3">
                        <TrendingUp size={12} /> Impacto Projetado
                      </span>
                      <h3 className="text-sm font-bold text-primary">
                        Custo Anual Recorrente
                      </h3>
                    </div>
                  </div>

                  <div className="z-10 mt-6 flex items-end gap-4">
                    <span className="text-4xl sm:text-5xl font-black text-primary tracking-tighter">
                      {formatCurrency(analytics.yearlyProjection)}
                    </span>
                    <span className="text-sm font-medium text-muted mb-2">
                      / ano
                    </span>
                  </div>

                  <p className="z-10 text-xs sm:text-sm font-medium text-secondary mt-4">
                    O cancelamento de serviços não utilizados pode gerar uma
                    economia direta neste montante.
                  </p>
                </div>

                {/* KPIS RÁPIDOS */}
                <div className="flex flex-col gap-4">
                  <div className="flex-1 card-default p-5 flex items-center gap-4 rounded-3xl">
                    <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
                      <Wallet size={24} />
                    </div>
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                        Gasto Mensal Fixo
                      </span>
                      <span className="text-xl font-extrabold text-primary">
                        {formatCurrency(analytics.monthlyTotal)}
                      </span>
                    </div>
                  </div>

                  <div className="flex-1 card-default p-5 flex items-center gap-4 rounded-3xl">
                    <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
                      <PieChart size={24} />
                    </div>
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                        Peso no Orçamento
                      </span>
                      <span className="text-xl font-extrabold text-primary">
                        {analytics.budgetImpact}% da renda
                      </span>
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
                    <span className="text-xs font-bold text-gray-200 bg-amber-700/40 px-3 py-1.5 rounded-lg border border-amber-900/30 shadow-sm">
                      {analytics.upcomingNext7Days} vencimento(s) em até 7 dias
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
          )
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
