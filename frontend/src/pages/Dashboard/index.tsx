import { useState } from "react";
import {
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  Clock,
  Calendar,
} from "lucide-react";
import { useDashboard } from "../../hooks/useDashBoard";
import { PayInstallmentModal } from "../../components/PayInstallmentModal";
import { PaySubscriptionModal } from "../../components/PaySubscriptionModal";

// Importação dos Componentes Encapsulados (O Dashboard apenas OS USA!)
import { SectionTitle } from "../../components/dashboard/SectionTitle";
import { SummaryCard } from "../../components/dashboard/SummaryCard";
import { TimelineItem } from "../../components/dashboard/TimeLineItem";
import { PendencyItem } from "../../components/dashboard/PendencyItem";

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

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-4">
        <div className="w-6 h-6 border-2 border-brand border-t-transparent rounded-full animate-spin"></div>
        <span className="text-sm font-medium text-muted">
          Consolidando o seu painel financeiro...
        </span>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center p-6">
        <div className="badge-danger p-4 rounded-xl text-sm max-w-md text-center">
          Ocorreu um erro ao carregar o painel financeiro. Por favor, atualize a
          página.
        </div>
      </div>
    );
  }

  return (
    <div className="w-full pb-16 min-h-screen">
      <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
        {/* 1. SEÇÃO DE CARDS DE RESUMO (GRID 4 COLUNAS) */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <SummaryCard
            title="Entradas do Mês"
            value={data.summary?.totalIncome}
            icon={ArrowUpCircle}
            variant="brand"
          />
          <SummaryCard
            title="Saídas do Mês"
            value={data.summary?.totalExpenses}
            icon={ArrowDownCircle}
            variant="secondary"
          />
          <SummaryCard
            title="Saldo Disponível"
            value={data.summary?.currentBalance}
            icon={DollarSign}
            variant="brand"
          />
          <SummaryCard
            title="Projeção Fim do Mês"
            value={data.projection?.projectedBalance}
            icon={Calendar}
            variant="accent"
          />
        </section>

        {/* 2. GRID CENTRAL (HISTÓRICO + COMPROMISSOS) */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* HISTÓRICO (OCUPA 2 COLUNAS) */}
          <div className="lg:col-span-2 card-default flex flex-col">
            <SectionTitle
              title="Histórico Recente"
              subtitle="Visão unificada das movimentações"
            />
            <div className="space-y-1 flex-1">
              {data.timeline?.map((item) => (
                <TimelineItem
                  key={item.id}
                  title={item.title}
                  description={item.description}
                  amount={item.amount}
                  type={item.type}
                />
              ))}
            </div>
          </div>

          {/* COMPROMISSOS (OCUPA 1 COLUNA) */}
          <div className="card-default flex flex-col">
            <SectionTitle title="Compromissos" icon={Clock} />
            <div className="space-y-3">
              {data.pendencies?.map((pend) => (
                <PendencyItem
                  key={pend.id}
                  title={pend.title}
                  amount={pend.amount}
                  type={pend.type}
                  onAction={() =>
                    pend.type === "installment"
                      ? handleOpenPayModal(pend.id)
                      : handleOpenSubModal(pend.id)
                  }
                />
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* MODAIS */}
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
