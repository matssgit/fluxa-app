export interface DashboardSummary {
   currentBalance: number;
   totalIncome: number;
   totalExpenses: number;
}

export interface HybridTimelineItem {
   id: string;
   type: "cash" | "credit";
   title: string;
   description: string;
   amount: number;
   date: string;
   cashType?: "entrada" | "saida";
}

export interface DashboardPendency {
   id: string;
   type: "transaction" | "installment" | "subscription"; // <--- Assinatura devidamente registrada
   title: string;
   amount: number;
   dueDate: string;
   info: string;
}

export interface DashboardResponse {
   monthReference: string;
   summary: DashboardSummary;
   projection: {
      projectedBalance: number;
      expectedIncome: number;
      expectedExpenses: number;
      pendingInstallments: number;
   };
   pendencies: DashboardPendency[]; // <--- Agora sim, usando a interface correta!
   timeline: HybridTimelineItem[];
   alerts: string[];
}
