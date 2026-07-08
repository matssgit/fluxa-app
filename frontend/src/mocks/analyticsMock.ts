import type { DashboardMetrics } from "../types/analytics";

export const MOCK_DASHBOARD_METRICS: DashboardMetrics = {
  total_balance: 14250.8,
  monthly_income: 8500.0,
  monthly_expenses: 4200.0,
  net_savings: 4300.0,
  health: {
    score: 82,
    status: "good",
    savings_rate: 32.5,
    commitment_rate: 38.0,
    liquidity_months: 3.4,
  },
  insights: [
    {
      id: "ins-1",
      type: "positive",
      title: "Taxa de economia em alta",
      description:
        "Você guardou 32.5% da sua renda líquida este mês, superando a média ideal de mercado (20%).",
      category: "savings",
      created_at: new Date().toISOString(),
    },
    {
      id: "ins-2",
      type: "warning",
      title: "Compromisso recorrente próximo do limite",
      description:
        "Suas assinaturas e custos fixos somam R$ 3.230,00 (38% da receita). O limite recomendado para segurança é 40%.",
      category: "subscriptions",
      created_at: new Date().toISOString(),
    },
  ],
  recommendations: [
    {
      id: "rec-1",
      title: "Otimizar liquidez imediata",
      description:
        "Sua conta corrente possui R$ 4.500,00 parados sem rendimento. Considere transferir parte para o objetivo 'Reserva de Emergência'.",
      action_label: "Ir para Objetivos",
      action_url: "/wallets",
      impact_estimate: "Rendimento estimado de R$ 38,00/mês",
    },
  ],
  cash_flow_evolution: [
    { month: "Fev", income: 7200, expense: 4800, balance: 2400 },
    { month: "Mar", income: 7500, expense: 5100, balance: 2400 },
    { month: "Abr", income: 8000, expense: 4300, balance: 3700 },
    { month: "Mai", income: 8200, expense: 4900, balance: 3300 },
    { month: "Jun", income: 8500, expense: 4500, balance: 4000 },
    { month: "Jul", income: 8500, expense: 4200, balance: 4300 },
  ],
  category_distribution: [
    {
      category_id: "cat-1",
      category_name: "Moradia & Fixos",
      amount: 1800,
      percentage: 42.8,
      color: "#10B981",
    }, // brand emerald
    {
      category_id: "cat-2",
      category_name: "Alimentação",
      amount: 950,
      percentage: 22.6,
      color: "#3B82F6",
    }, // blue
    {
      category_id: "cat-3",
      category_name: "Lazer & Assinaturas",
      amount: 650,
      percentage: 15.5,
      color: "#F59E0B",
    }, // amber
    {
      category_id: "cat-4",
      category_name: "Transporte",
      amount: 500,
      percentage: 11.9,
      color: "#6366F1",
    }, // indigo
    {
      category_id: "cat-5",
      category_name: "Outros",
      amount: 300,
      percentage: 7.2,
      color: "#64748B",
    }, // slate
  ],
};
