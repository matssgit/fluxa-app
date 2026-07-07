import { useState } from "react";
import {
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  Calendar,
  AlertCircle,
  Clock,
  TrendingUp,
  ShieldCheck,
  Zap,
  PieChart,
} from "lucide-react";
import { useDashboard } from "../../hooks/useDashBoard";
import { PayInstallmentModal } from "../../components/PayInstallmentModal";
import { PaySubscriptionModal } from "../../components/PaySubscriptionModal";
import { CreditSummaryWidget } from "../../components/dashboard/CreditSummaryWidget";
import { InsightsWidget } from "../../components/dashboard/InsightsWidgets";

// UI Library
import { EmptyState, Card } from "../../components/ui";

// Componentes de Domínio
import { SummaryCard } from "../../components/dashboard/SummaryCard";
import { PendencyItem } from "../../components/dashboard/PendencyItem";
import { SectionTitle } from "../../components/dashboard/SectionTitle";
import { DashboardSkeleton } from "../../components/dashboard/DashboardSkeleton";

export function Dashboard() {
  const [isPayModalOpen, setIsPayModalOpen] = useState(false);
  const [selectedInstallmentId, setSelectedInstallmentId] = useState<
    string | null
  >(null);

  const [isSubModalOpen, setIsSubModalOpen] = useState(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  const { data, isLoading, isError } = useDashboard();

  function handleOpenPayModal(installmentId: string) {
    setSelectedInstallmentId(installmentId);
    setIsPayModalOpen(true);
  }

  function handleOpenSubModal(subId: string) {
    setSelectedSubId(subId);
    setIsSubModalOpen(true);
  }

  // 1. ESTADO DE CARREGAMENTO
  if (isLoading) {
    return <DashboardSkeleton />;
  }

  // 2. ESTADO DE ERRO
  if (isError || !data) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center p-6 animate-fade-in">
        <EmptyState
          icon={AlertCircle}
          title="Não foi possível carregar a telemetria"
          description="Ocorreu um erro ao processar sua inteligência financeira. Por favor, atualize a página em instantes."
        />
      </div>
    );
  }

  // Extração segura de métricas para o BI
  const totalIncome = Number(data.summary?.totalIncome || 0);
  const totalExpenses = Number(data.summary?.totalExpenses || 0);
  const currentBalance = Number(data.summary?.currentBalance || 0);
  const projectedBalance = Number(data.projection?.projectedBalance || 0);

  // Cálculos de Telemetria Financeira
  const burnRatePercentage =
    totalIncome > 0 ? Math.min((totalExpenses / totalIncome) * 100, 100) : 0;
  const savingsRate =
    totalIncome > 0
      ? Math.max(((totalIncome - totalExpenses) / totalIncome) * 100, 0)
      : 0;
  const isHealthy = projectedBalance >= 0 && burnRatePercentage <= 80;

  const formatCurrency = (val: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(val);

  // 3. COCKPIT ANALÍTICO PRINCIPAL
  return (
    <div className="w-full pb-16 min-h-screen animate-fade-in">
      <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
        {/* BANNER DE SAÚDE FINANCEIRA (Telemetria Instantânea) */}
        <div className="card-default p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-l-4 border-l-brand">
          <div className="flex items-center gap-4">
            <div
              className={`w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 shadow-2xs ${
                isHealthy
                  ? "bg-brand/10 text-brand"
                  : "bg-amber-500/10 text-amber-500"
              }`}
            >
              {isHealthy ? <ShieldCheck size={26} /> : <Zap size={26} />}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base sm:text-lg font-extrabold text-primary tracking-tight">
                  {isHealthy
                    ? "Sua liquidez operacional está saudável!"
                    : "Atenção ao ritmo de gastos do mês"}
                </h3>
                <span className="text-[10px] font-extrabold uppercase tracking-widest px-2 py-0.5 rounded bg-surface border border-subtle/30 text-secondary hidden md:inline-block">
                  Telemetria Ativa
                </span>
              </div>
              <p className="text-xs sm:text-sm font-medium text-muted mt-0.5">
                {isHealthy
                  ? `Você está mantendo uma margem de poupança de ${savingsRate.toFixed(0)}% sobre as suas receitas.`
                  : `Suas saídas já consumiram ${burnRatePercentage.toFixed(0)}% das entradas deste período.`}
              </p>
            </div>
          </div>
        </div>

        {/* GRID SUPERIOR: CARDS DE RESUMO (KPIs) */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SummaryCard
            title="Entradas do Mês"
            value={totalIncome}
            icon={ArrowUpCircle}
          />
          <SummaryCard
            title="Saídas do Mês"
            value={totalExpenses}
            icon={ArrowDownCircle}
          />
          <SummaryCard
            title="Saldo Disponível"
            value={currentBalance}
            icon={DollarSign}
          />
          <SummaryCard
            title="Projeção Fim do Mês"
            value={projectedBalance}
            icon={Calendar}
          />
        </section>

        {/* GRID CENTRAL: TELEMETRIA + RADAR DE CURTO PRAZO */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* WIDGET 1: TERMÔMETRO DE QUEIMA DE CAIXA (Substitui o antigo Histórico!) */}
          <div className="lg:col-span-2 flex flex-col">
            <Card
              variant="default"
              className="flex-1 flex flex-col justify-between p-6 sm:p-8"
            >
              <div>
                <SectionTitle
                  eyebrow="TELEMETRIA OPERACIONAL"
                  title="Raio-X do Fluxo Mensal"
                  subtitle="Análise de comprometimento da receita e ritmo de queima"
                />

                <div className="mt-8 space-y-6">
                  {/* Barra de Consumo (Burn Rate) */}
                  <div className="space-y-2">
                    <div className="flex justify-between items-end">
                      <div>
                        <span className="text-xs font-bold text-primary block">
                          Comprometimento de Receita
                        </span>
                        <span className="text-[11px] font-medium text-muted">
                          Saídas vs. Entradas do período
                        </span>
                      </div>
                      <span className="text-sm font-extrabold text-primary">
                        {burnRatePercentage.toFixed(1)}%
                      </span>
                    </div>
                    <div className="w-full h-3 bg-subtle/30 rounded-full overflow-hidden p-0.5">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          burnRatePercentage > 85
                            ? "bg-red-500"
                            : burnRatePercentage > 70
                              ? "bg-amber-500"
                              : "bg-brand"
                        }`}
                        style={{ width: `${burnRatePercentage}%` }}
                      />
                    </div>
                  </div>

                  {/* Mini-Cards de Indicadores */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-subtle/20">
                    <div className="p-4 rounded-2xl bg-elevated/40 border border-subtle/20 flex items-center gap-3.5">
                      <div className="w-10 h-10 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
                        <TrendingUp size={20} />
                      </div>
                      <div>
                        <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                          Margem Livre
                        </span>
                        <span className="text-base font-extrabold text-primary tracking-tight">
                          {formatCurrency(
                            Math.max(totalIncome - totalExpenses, 0),
                          )}
                        </span>
                      </div>
                    </div>

                    <div className="p-4 rounded-2xl bg-elevated/40 border border-subtle/20 flex items-center gap-3.5">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
                        <PieChart size={20} />
                      </div>
                      <div>
                        <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                          Taxa de Poupança
                        </span>
                        <span className="text-base font-extrabold text-primary tracking-tight">
                          {savingsRate.toFixed(1)}%
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 rounded-xl bg-surface border border-subtle/30 text-xs text-muted flex items-center justify-between">
                    <span>
                      💡 <strong>Dica Executiva:</strong> Para ver o extrato
                      detalhado de cada transação, acesse a central operacional.
                    </span>
                    <a
                      href="/transactions"
                      className="text-brand font-bold hover:underline shrink-0 ml-2"
                    >
                      Ir para o Caixa →
                    </a>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          {/* WIDGET 2: RADAR DE VENCIMENTOS (Mantido e lapidado) */}
          <div className="flex flex-col">
            <Card
              variant="default"
              className="flex-1 flex flex-col justify-between p-6 sm:p-8"
            >
              <div>
                <SectionTitle
                  eyebrow="ATENÇÃO IMEDIATA"
                  title="Radar de Vencimentos"
                  subtitle="Compromissos e faturas pendentes"
                />

                <div className="space-y-3 mt-6">
                  {data.pendencies && data.pendencies.length > 0 ? (
                    data.pendencies.map((pend) => (
                      <PendencyItem
                        key={pend.id}
                        title={pend.title}
                        amount={pend.amount}
                        dueDate={pend.dueDate || "Mês atual"}
                        onAction={() =>
                          pend.type === "installment"
                            ? handleOpenPayModal(pend.id)
                            : handleOpenSubModal(pend.id)
                        }
                      />
                    ))
                  ) : (
                    <div className="py-12">
                      <EmptyState
                        icon={Clock}
                        title="Radar Limpo!"
                        description="Você não possui compromissos, faturas ou parcelas exigindo atenção imediata."
                      />
                    </div>
                  )}
                </div>
              </div>
            </Card>
          </div>
        </section>

        {/* GRID INFERIOR: WIDGETS DE CRÉDITO E INSIGHTS */}
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-6 pt-2">
          {/* WIDGET DE CRÉDITO */}
          <CreditSummaryWidget />

          {/* WIDGET DE INSIGHTS */}
          <InsightsWidget
            totalIncome={totalIncome}
            totalExpenses={totalExpenses}
            projectedBalance={projectedBalance}
            savingsRate={savingsRate}
            burnRatePercentage={burnRatePercentage}
          />
        </section>
      </main>

      {/* MODAIS DE PAGAMENTO ATÓMICOS */}
      <PayInstallmentModal
        isOpen={isPayModalOpen}
        onClose={() => setIsPayModalOpen(false)}
        installmentId={selectedInstallmentId}
      />
      <PaySubscriptionModal
        isOpen={isSubModalOpen}
        onClose={() => setIsSubModalOpen(false)}
        subscriptionId={selectedSubId}
      />
    </div>
  );
}
