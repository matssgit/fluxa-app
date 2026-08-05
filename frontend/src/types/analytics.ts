export interface FinancialHealth {
  score: number;
  status: "excellent" | "good" | "attention" | "critical";
  savings_rate: number;
  commitment_rate: number;
  liquidity_months: number;
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
  action_url?: string;
  impact_estimate?: string;
}

export interface CashFlowDataPoint {
  month: string;
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
