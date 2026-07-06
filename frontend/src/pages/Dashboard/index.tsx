import { useState } from "react";
import {
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  Calendar,
  AlertCircle,
} from "lucide-react";
import { useDashboard } from "../../hooks/useDashBoard";
import { PayInstallmentModal } from "../../components/PayInstallmentModal";
import { PaySubscriptionModal } from "../../components/PaySubscriptionModal";

// Importação limpa da nossa UI Library
import { Loading, EmptyState, Card } from "../../components/ui";

// Importação dos Componentes de Domínio do Dashboard
import { SummaryCard } from "../../components/dashboard/SummaryCard";
import { TimelineItem } from "../../components/dashboard/TimelineItem";
import { PendencyItem } from "../../components/dashboard/PendencyItem";
import { SectionTitle } from "../../components/dashboard/SectionTitle";

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

  // 1. ESTADO DE CARREGAMENTO (Terceirizado para a UI Library)
  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Loading size="lg" text="Consolidando o seu painel financeiro..." />
      </div>
    );
  }

  // 2. ESTADO DE ERRO (Terceirizado para a UI Library)
  if (isError || !data) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center p-6">
        <EmptyState
          icon={AlertCircle}
          title="Não foi possível carregar o painel"
          description="Ocorreu um erro ao buscar suas informações financeiras. Por favor, atualize a página ou tente novamente mais tarde."
        />
      </div>
    );
  }

  // 3. ESTADO PRINCIPAL (Maestro de Layout Puro sem estilização visual própria)
  return (
    <div className="w-full pb-16 min-h-screen">
      <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
        {/* GRID SUPERIOR: CARDS DE RESUMO (KPIs) */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SummaryCard
            title="Entradas do Mês"
            value={data.summary?.totalIncome}
            icon={ArrowUpCircle}
          />
          <SummaryCard
            title="Saídas do Mês"
            value={data.summary?.totalExpenses}
            icon={ArrowDownCircle}
          />
          <SummaryCard
            title="Saldo Disponível"
            value={data.summary?.currentBalance}
            icon={DollarSign}
          />
          <SummaryCard
            title="Projeção Fim do Mês"
            value={data.projection?.projectedBalance}
            icon={Calendar}
          />
        </section>

        {/* GRID CENTRAL: HISTÓRICO + COMPROMISSOS */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* HISTÓRICO RECENTE (Ocupa 2 Colunas no Desktop) */}
          <div className="lg:col-span-2 flex flex-col">
            <Card variant="default" className="flex-1 flex flex-col">
              <SectionTitle
                title="Histórico Recente"
                subtitle="Visão unificada das últimas movimentações"
              />
              <div className="space-y-2 mt-4 flex-1">
                {data.timeline && data.timeline.length > 0 ? (
                  data.timeline.map((item) => (
                    <TimelineItem
                      key={item.id}
                      title={item.title}
                      description={item.description}
                      amount={item.amount}
                      type={item.type}
                    />
                  ))
                ) : (
                  <p className="text-sm text-secondary text-center py-8">
                    Nenhuma movimentação registrada recentemente.
                  </p>
                )}
              </div>
            </Card>
          </div>

          {/* COMPROMISSOS (Ocupa 1 Coluna no Desktop) */}
          <div className="flex flex-col">
            <Card variant="default" className="flex-1 flex flex-col">
              <SectionTitle
                title="Compromissos"
                subtitle="Pendências e faturas do mês"
              />
              <div className="space-y-3 mt-4">
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
                  <p className="text-sm text-secondary text-center py-8">
                    Nenhum compromisso pendente para este período.
                  </p>
                )}
              </div>
            </Card>
          </div>
        </section>
      </main>

      {/* MODAIS DE PAGAMENTO */}
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
