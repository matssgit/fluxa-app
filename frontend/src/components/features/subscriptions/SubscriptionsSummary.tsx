import { type Subscription } from "../../../types/subscription";
import { DollarSign, Calendar, Repeat, Clock } from "lucide-react";

interface SubscriptionsSummaryProps {
  subscriptions: Subscription[];
}

export function SubscriptionsSummary({
  subscriptions,
}: SubscriptionsSummaryProps) {
  // Agregação Visual de Custo Mensal (soma estritamente serviços ativos)
  const monthlyCost = subscriptions.reduce((acc, sub) => {
    if (sub.status !== "active") return acc;

    const val = Number(sub.amount) || 0;
    const monthlyAmount = sub.frequency === "yearly" ? val / 12 : val;
    return acc + monthlyAmount;
  }, 0);

  // Projeção Anual Simples (Apresentação)
  const annualCost = monthlyCost * 12;

  // Contagem de Assinaturas Ativas (apenas as que estão rodando)
  const activeCount = subscriptions.filter((s) => s.status === "active").length;

  // Inteligência Visual de Próximos Vencimentos (Próximos 7 dias - Apenas Ativas)
  const todayDay = new Date().getDate();
  const upcomingCount = subscriptions.filter((sub) => {
    if (sub.status !== "active") return false;
    const diff = sub.due_day - todayDay;
    return diff >= 0 && diff <= 7;
  }).length;

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(isNaN(val) ? 0 : val);

  return (
    <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-fade-in">
      {/* KPI 1: Custo Mensal */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Custo Mensal
          </span>
          <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <DollarSign size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {formatCurrency(monthlyCost)}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Compromisso recorrente
          </span>
        </div>
      </div>

      {/* KPI 2: Custo Anual */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Custo Anual
          </span>
          <div className="w-9 h-9 rounded-xl bg-elevated text-secondary flex items-center justify-center shrink-0">
            <Calendar size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {formatCurrency(annualCost)}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Projeção acumulada (12m)
          </span>
        </div>
      </div>

      {/* KPI 3: Assinaturas Ativas */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Serviços Ativos
          </span>
          <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <Repeat size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {activeCount}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Assinaturas monitoradas
          </span>
        </div>
      </div>

      {/* KPI 4: Próximos Vencimentos */}
      <div className="card-default p-5 flex flex-col justify-between border-subtle/30">
        <div className="flex justify-between items-start">
          <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
            Vencem Esta Semana
          </span>
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
              upcomingCount > 0
                ? "bg-amber-500/10 text-amber-500"
                : "bg-elevated text-muted"
            }`}
          >
            <Clock size={18} />
          </div>
        </div>
        <div className="mt-4">
          <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
            {upcomingCount}
          </span>
          <span className="text-[11px] font-medium text-muted block mt-0.5">
            Nos próximos 7 dias
          </span>
        </div>
      </div>
    </section>
  );
}
