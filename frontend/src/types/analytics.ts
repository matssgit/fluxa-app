export interface FinancialHealth {
  score: number; // 0 a 100
  status: "excellent" | "good" | "attention" | "critical";
  savings_rate: number; // Porcentagem da renda direcionada para reservas
  commitment_rate: number; // Porcentagem comprometida com custos fixos/assinaturas
  liquidity_months: number; // Quantos meses o usuário sobrevive com o saldo atual
}

export interface Insight {
  id: string;
  type: "positive" | "warning" | "neutral" | "info";
  title: string;
  description: string;
  category: "savings" | "spending" | "subscriptions" | "wallets" | "general";
  created_at: string;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  action_label: string;
  action_url?: string; // Rota interna para acionar a melhoria (ex: "/subscriptions")
  impact_estimate?: string; // ex: "Economia estimada de R$ 45,00/mês"
}

// Estrutura para os futuros gráficos (Sprint 6.1)
export interface CashFlowDataPoint {
  month: string; // Ex: "Jan", "Fev" ou "2026-07"
  income: number;
  expense: number;
  balance: number;
}

export interface CategoryDistributionPoint {
  category_id: string;
  category_name: string;
  amount: number;
  percentage: number;
  color?: string;
}

// O Payload Executivo Global (Consumido pelo Dashboard)
export interface DashboardMetrics {
  total_balance: number;
  monthly_income: number;
  monthly_expenses: number;
  net_savings: number;
  health: FinancialHealth;
  insights: Insight[];
  recommendations: Recommendation[];
  cash_flow_evolution: CashFlowDataPoint[];
  category_distribution: CategoryDistributionPoint[];
}
