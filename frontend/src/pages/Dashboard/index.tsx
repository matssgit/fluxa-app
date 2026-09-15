import { useState } from "react";
import { DashboardTour } from "./DashboardTour";
import { EmptyState, Card } from "../../components/ui";
import { PrivacyMask } from "../../components/ui/PrivacyMask";
import { useDashboard } from "../../hooks/useDashboard";
import { formatCurrency } from "../../utils/formatters";
import { SummaryCard } from "../../components/dashboard/SummaryCard";
import { SectionTitle } from "../../components/dashboard/SectionTitle";
import { PendencyItem } from "../../components/dashboard/PendencyItem";
import { InsightsWidget } from "../../components/dashboard/InsightsWidgets";
import { DashboardSkeleton } from "../../components/dashboard/DashboardSkeleton";
import { CreditSummaryWidget } from "../../components/dashboard/CreditSummaryWidget";
import { PayInstallmentModal } from "../../components/features/cards/PayInstallmentModal";
import { PaySubscriptionModal } from "../../components/features/subscriptions/PaySubscriptionModal";
import {
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  AlertCircle,
  Clock,
  TrendingUp,
  ShieldCheck,
  Zap,
  PieChart,
} from "lucide-react";

function extractMetric(source: unknown, keys: string[]): number {
  if (!source || typeof source !== "object") return 0;
  const srcObj = source as Record<string, unknown>;

  const objectsToSearch = [
    srcObj.summary,
    srcObj.monthSummary,
    srcObj.overview,
    srcObj.kpis,
    srcObj.flow,
    srcObj.month_summary,
    srcObj.projection,
    srcObj,
  ];

  for (const obj of objectsToSearch) {
    if (obj && typeof obj === "object") {
      const record = obj as Record<string, unknown>;
      for (const key of keys) {
        const val = record[key];
        if (val !== undefined && val !== null) {
          const num = Number(val);
          if (!isNaN(num)) return num;
        }
      }
    }
  }
  return 0;
}

export function Dashboard() {
  const [isPayModalOpen, setIsPayModalOpen] = useState<boolean>(false);
  const [selectedInstallmentId, setSelectedInstallmentId] = useState<
    string | null
  >(null);

  const [isSubModalOpen, setIsSubModalOpen] = useState<boolean>(false);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

  const { data, isLoading, isError } = useDashboard();

  function handleOpenPayModal(installmentId: string): void {
    setSelectedInstallmentId(installmentId);
    setIsPayModalOpen(true);
  }

  function handleOpenSubModal(subId: string): void {
    setSelectedSubId(subId);
    setIsSubModalOpen(true);
  }

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (isError || !data) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center p-6 animate-fade-in">
        <EmptyState
          icon={AlertCircle}
          title="Não foi possível carregar a telemetria"
          description="Ocorreu um erro ao processar sua inteligência financeira. Por favor, atualize a página em instantes."
        />
      </div>
    );
  }

  const totalIncome = extractMetric(data, [
    "totalIncome",
    "income",
    "total_income",
    "incomes",
    "totalIncomes",
    "revenues",
    "revenue",
    "totalRevenues",
    "total_revenues",
    "receitas",
    "receita",
    "totalReceitas",
    "total_receitas",
    "entradas",
    "entrada",
    "totalEntradas",
    "total_entradas",
    "monthIncome",
    "month_income",
  ]);
  const totalExpenses = extractMetric(data, [
    "totalExpenses",
    "expense",
    "total_expense",
    "expenses",
    "totalExpense",
    "despesas",
    "despesa",
    "totalDespesas",
    "total_despesas",
    "saidas",
    "saida",
    "totalSaidas",
    "total_saidas",
    "monthExpense",
    "month_expense",
  ]);
  const currentBalance = extractMetric(data, [
    "currentBalance",
    "amount",
    "current_balance",
    "balance",
    "totalBalance",
    "total_balance",
    "saldo",
    "saldoAtual",
    "saldo_atual",
  ]);
  const projectedBalance = extractMetric(data, [
    "projectedBalance",
    "projected_balance",
    "projection",
    "projections",
    "saldoProjetado",
    "saldo_projetado",
  ]);

  const availableLiquidity =
    currentBalance !== 0
      ? currentBalance
      : Math.max(totalIncome - totalExpenses, 0);

  const burnRatePercentage =
    totalIncome > 0 ? Math.min((totalExpenses / totalIncome) * 100, 100) : 0;
  const savingsRate =
    totalIncome > 0
      ? Math.max(((totalIncome - totalExpenses) / totalIncome) * 100, 0)
      : 0;
  const isHealthy = projectedBalance >= 0 && burnRatePercentage <= 80;

  return (
    <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-10">
      <DashboardTour />
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
        <div>
          <span className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-brand">
            Visão geral
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-primary tracking-[-0.045em]">
            Seu fluxo financeiro
          </h1>
          <p className="text-xs sm:text-sm font-medium text-muted mt-1">
            Clareza para decidir hoje e tranquilidade para planejar amanhã.
          </p>
        </div>
      </div>
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-5 tour-dashboard-summary">
        <div className="lg:col-span-8 min-h-80 sm:min-h-96 rounded-tl-[2.75rem] rounded-tr-[2.75rem] rounded-br-[2.75rem] rounded-bl-xl bg-[#173f36] text-[#fffdf7] p-7 sm:p-10 relative overflow-hidden shadow-md flex flex-col justify-between">
          <div className="absolute -right-24 -top-28 w-96 h-96 rounded-full border-[58px] border-white/5" />
          <div className="absolute right-8 -bottom-40 w-80 h-80 rounded-[42%_58%_68%_32%] bg-[#829b88]/18 rotate-12" />
          <div className="absolute left-[45%] top-14 w-24 h-56 rounded-[70%_30%_64%_36%] bg-white/3 rotate-45" />
          <div className="relative flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-[0.18em] text-[#dfe8de]">
              Saldo disponível
            </span>
            <div className="w-11 h-11 rounded-2xl bg-white/10 flex items-center justify-center">
              <DollarSign size={21} strokeWidth={1.8} />
            </div>
          </div>
          <div className="relative my-10">
            <div
              data-financial-value="true"
              className="text-5xl sm:text-6xl lg:text-7xl leading-none font-extrabold tracking-[-0.065em] tabular-nums"
            >
              <PrivacyMask amount={availableLiquidity} />
            </div>
            <p className="text-sm text-[#dfe8de]/75 mt-5 max-w-sm leading-relaxed">
              Recursos livres considerando as entradas e saídas registradas.
            </p>
          </div>

          <div className="relative flex flex-col sm:flex-row sm:items-end sm:justify-between gap-5 border-t border-white/10 pt-5">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#dfe8de]/60 block">
                Projeção no fim do mês
              </span>
              <span className="text-lg sm:text-xl font-extrabold tabular-nums mt-1 block">
                {formatCurrency(projectedBalance)}
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs font-semibold text-[#dfe8de]">
              {isHealthy ? <ShieldCheck size={17} /> : <Zap size={17} />}
              <span>
                {isHealthy
                  ? `${savingsRate.toFixed(0)}% de margem preservada`
                  : `${burnRatePercentage.toFixed(0)}% das entradas comprometidas`}
              </span>
            </div>
          </div>
        </div>

        <div className="lg:col-span-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-1 gap-4">
          <SummaryCard
            title="Entradas do mês"
            value={totalIncome}
            icon={ArrowUpCircle}
            variant="income"
          />
          <SummaryCard
            title="Saídas do mês"
            value={totalExpenses}
            icon={ArrowDownCircle}
            variant="expense"
          />
        </div>
      </section>

      <div className="tour-dash-health flex flex-col sm:flex-row sm:items-center gap-4 py-5 sm:py-6 border-y border-border">
        <div
          className={`w-12 h-12 rounded-[1.25rem_1.25rem_1.25rem_0.35rem] flex items-center justify-center shrink-0 ${
            isHealthy
              ? "bg-income-soft text-income"
              : "bg-warning/10 text-warning"
          }`}
        >
          {isHealthy ? <ShieldCheck size={25} /> : <Zap size={25} />}
        </div>
        <div className="flex-1">
          <span className="text-[10px] font-extrabold uppercase tracking-[0.18em] text-muted">
            Leitura do período
          </span>
          <h3 className="text-base sm:text-lg font-extrabold text-primary tracking-tight">
            {isHealthy
              ? "Seu fluxo mantém uma margem confortável"
              : "O ritmo de gastos merece atenção"}
          </h3>
          <p className="text-xs sm:text-sm font-medium text-muted mt-0.5">
            {isHealthy
              ? `Você está preservando ${savingsRate.toFixed(0)}% das receitas deste período.`
              : `Suas saídas já consumiram ${burnRatePercentage.toFixed(0)}% das entradas deste período.`}
          </p>
        </div>
      </div>
      {/* TELEMETRIA + RADAR */}
      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 flex flex-col">
          <Card
            variant="default"
            className="flex-1 flex flex-col justify-between p-6 sm:p-8 tour-dash-telemetry rounded-tl-[2.5rem] rounded-br-[2.5rem] rounded-tr-xl rounded-bl-xl"
          >
            <div>
              <SectionTitle
                eyebrow="TELEMETRIA OPERACIONAL"
                title="Raio-X do Fluxo Mensal"
                subtitle="Análise de comprometimento da receita e ritmo de queima"
              />

              <div className="mt-8 space-y-6">
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
              </div>
            </div>
          </Card>
        </div>

        <div className="flex flex-col">
          <Card
            variant="default"
            className="flex-1 flex flex-col justify-between p-6 sm:p-8 tour-dash-radar rounded-tr-[2.5rem] rounded-bl-[2.5rem] rounded-tl-xl rounded-br-xl"
          >
            <div>
              <SectionTitle
                eyebrow="ATENÇÃO IMEDIATA"
                title="Radar de Vencimentos"
                subtitle="Compromissos e faturas pendentes"
              />

              <div className="space-y-3 mt-6">
                {data.pendencies && data.pendencies.length > 0 ? (
                  data.pendencies.map(
                    (pend: {
                      id: string;
                      title: string;
                      amount: number;
                      dueDate?: string;
                      type: string;
                    }) => (
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
                    ),
                  )
                ) : (
                  <div className="py-12">
                    <EmptyState
                      icon={Clock}
                      title="Radar Limpo!"
                      description="Você não possui compromissos exigindo atenção imediata."
                    />
                  </div>
                )}
              </div>
            </div>
          </Card>
        </div>
      </section>
      {/* GRID INFERIOR */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6 pt-2">
        <CreditSummaryWidget />
        <InsightsWidget
          totalIncome={totalIncome}
          totalExpenses={totalExpenses}
          projectedBalance={projectedBalance}
          savingsRate={savingsRate}
          burnRatePercentage={burnRatePercentage}
        />
      </section>
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
