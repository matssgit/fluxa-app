import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { db as knex } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

export async function analyticsRoutes(app: FastifyInstance) {
  // Protege todas as rotas de analytics
  app.addHook("preHandler", checkAuth);

  // COCKPIT EXECUTIVO: ENDPOINT PRINCIPAL DO DASHBOARD
  app.get("/dashboard", async (request) => {
    // Blindagem estrita do ID do usuário (garante string pura sem undefined)
    const userId = request.user?.sub || "";
    if (!userId) {
      throw new Error("Usuário não autenticado.");
    }

    const querySchema = z.object({
      month: z.string().optional(), // Ex: "2026-07"
      year: z.coerce.number().optional(),
    });

    const { month } = querySchema.parse(request.query);

    // 1. Definição de datas do ciclo atual (Mês atual se não for passado parâmetro)
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonthIndex = now.getMonth(); // 0 a 11

    const targetDate = month ? new Date(`${month}-01T00:00:00`) : now;
    const startOfMonth = new Date(
      targetDate.getFullYear(),
      targetDate.getMonth(),
      1,
    );
    const endOfMonth = new Date(
      targetDate.getFullYear(),
      targetDate.getMonth() + 1,
      0,
      23,
      59,
      59,
    );

    // 2. PATRIMÔNIO LÍQUIDO (Soma do saldo de todas as contas do usuário)
    const accounts = await knex("accounts").where({ user_id: userId });
    const totalBalance = accounts.reduce(
      (acc, curr) => acc + (Number(curr.balance) || 0),
      0,
    );

    // 3. TRANSAÇÕES DO MÊS ATUAL (Entradas e Saídas)
    const currentTransactions = await knex("transactions")
      .where({ user_id: userId })
      .whereBetween("date", [
        startOfMonth.toISOString(),
        endOfMonth.toISOString(),
      ]);

    let monthlyIncome = 0;
    let monthlyExpenses = 0;

    currentTransactions.forEach((t) => {
      const val = Number(t.amount) || 0;
      if (t.type === "income") monthlyIncome += val;
      if (t.type === "expense") monthlyExpenses += val;
    });

    const netSavings = monthlyIncome - monthlyExpenses;

    // 4. CUSTOS FIXOS COMPROMETIDOS (Assinaturas Ativas)
    const activeSubscriptions = await knex("subscriptions").where({
      user_id: userId,
      status: "active",
    });

    const monthlySubscriptionsCost = activeSubscriptions.reduce((acc, sub) => {
      const val = Number(sub.amount) || 0;
      return acc + (sub.frequency === "yearly" ? val / 12 : val);
    }, 0);

    // 5. ALGORITMO DE SAÚDE FINANCEIRA (Score 0 a 100)
    const savingsRate =
      monthlyIncome > 0
        ? Math.max(0, Math.round((netSavings / monthlyIncome) * 100))
        : 0;
    const commitmentRate =
      monthlyIncome > 0
        ? Math.min(
            100,
            Math.round((monthlySubscriptionsCost / monthlyIncome) * 100),
          )
        : 0;
    const liquidityMonths =
      monthlyExpenses > 0
        ? Number((totalBalance / monthlyExpenses).toFixed(1))
        : totalBalance > 0
          ? 12.0
          : 0;

    // Cálculo ponderado do Score
    let score = 50;
    score += Math.min(30, savingsRate * 1.5);
    score += Math.min(20, liquidityMonths * 3.3);
    score -= Math.max(0, (commitmentRate - 30) * 1.0);

    score = Math.max(0, Math.min(100, Math.round(score)));

    let status: "excellent" | "good" | "attention" | "critical" = "good";
    if (score >= 80) status = "excellent";
    else if (score >= 60) status = "good";
    else if (score >= 40) status = "attention";
    else status = "critical";

    // 6. DISTRIBUIÇÃO POR CATEGORIA (Despesas do Mês) - Com Fallbacks Estritos!
    const categories = await knex("categories").where({ user_id: userId });

    // Blindagem de tipos no Map para garantir que name e color nunca sejam undefined
    const categoryMap = new Map(
      categories.map((c) => [
        String(c.id || ""),
        {
          name: String(c.name || "Outros"),
          color: String(c.color || "#64748B"),
        },
      ]),
    );

    const categorySpendMap = new Map<string, number>();
    currentTransactions
      .filter((t) => t.type === "expense")
      .forEach((t) => {
        // Garante que o catId seja sempre uma string válida
        const catId = String(t.category_id || "uncategorized");
        const current = categorySpendMap.get(catId) || 0;
        categorySpendMap.set(catId, current + (Number(t.amount) || 0));
      });

    const categoryDistribution: Array<{
      category_id: string;
      category_name: string;
      amount: number;
      percentage: number;
      color?: string;
    }> = [];

    categorySpendMap.forEach((amount, catId) => {
      const catInfo = categoryMap.get(catId) || {
        name: "Outros",
        color: "#64748B",
      };
      const percentage =
        monthlyExpenses > 0 ? Math.round((amount / monthlyExpenses) * 100) : 0;
      categoryDistribution.push({
        category_id: catId,
        category_name: catInfo.name || "Outros", // Fallback estrito para string pura
        amount,
        percentage,
        color: catInfo.color || "#64748B",
      });
    });

    categoryDistribution.sort((a, b) => b.amount - a.amount);

    // 7. EVOLUÇÃO DE FLUXO DE CAIXA (Últimos 6 Meses)
    const sixMonthsAgo = new Date(currentYear, currentMonthIndex - 5, 1);
    const historicalTransactions = await knex("transactions")
      .where({ user_id: userId })
      .where("date", ">=", sixMonthsAgo.toISOString());

    const monthNames = [
      "Jan",
      "Fev",
      "Mar",
      "Abr",
      "Mai",
      "Jun",
      "Jul",
      "Ago",
      "Set",
      "Out",
      "Nov",
      "Dez",
    ];
    const cashFlowEvolution: Array<{
      month: string;
      income: number;
      expense: number;
      balance: number;
    }> = [];

    for (let i = 5; i >= 0; i--) {
      const d = new Date(currentYear, currentMonthIndex - i, 1);
      const mIndex = d.getMonth();
      const y = d.getFullYear();
      const mLabel = monthNames[mIndex] || "";

      let mIncome = 0;
      let mExpense = 0;

      historicalTransactions.forEach((t) => {
        // Blindagem contra data indefinida no banco
        const tDate = new Date(t.date || Date.now());
        if (tDate.getMonth() === mIndex && tDate.getFullYear() === y) {
          const val = Number(t.amount) || 0;
          if (t.type === "income") mIncome += val;
          if (t.type === "expense") mExpense += val;
        }
      });

      cashFlowEvolution.push({
        month: mLabel,
        income: mIncome,
        expense: mExpense,
        balance: mIncome - mExpense,
      });
    }

    // 8. GERADOR DINÂMICO DE INSIGHTS E RECOMENDAÇÕES
    const insights = [];
    const recommendations = [];

    if (savingsRate >= 20) {
      insights.push({
        id: "ins-sav",
        type: "positive" as const,
        title: "Taxa de economia em alto nível",
        description: `Você direcionou ${savingsRate}% da sua renda líquida para reservas este mês, superando o ideal técnico de mercado (20%).`,
        category: "savings" as const,
        created_at: new Date().toISOString(),
      });
    } else if (savingsRate < 10 && monthlyIncome > 0) {
      insights.push({
        id: "ins-sav-low",
        type: "warning" as const,
        title: "Margem de poupança reduzida",
        description: `Sua margem de economia está em ${savingsRate}%. Tente rever despesas variáveis para alcançar ao menos 15% de reserva.`,
        category: "savings" as const,
        created_at: new Date().toISOString(),
      });
    }

    if (commitmentRate > 40) {
      insights.push({
        id: "ins-sub",
        type: "warning" as const,
        title: "Compromisso recorrente elevado",
        description: `Suas assinaturas e custos fixos representam ${commitmentRate}% da sua receita. O limite seguro recomendado é de 35%.`,
        category: "subscriptions" as const,
        created_at: new Date().toISOString(),
      });
    }

    if (liquidityMonths < 2 && monthlyExpenses > 0) {
      recommendations.push({
        id: "rec-liq",
        title: "Reforçar liquidez imediata",
        description: `Sua reserva atual cobre ${liquidityMonths} meses de despesas. O ideal para proteção patrimonial é acumular ao menos 3 meses de custo fixo.`,
        action_label: "Organizar Metas",
        action_url: "/wallets",
        impact_estimate: "Proteção patrimonial",
      });
    } else {
      recommendations.push({
        id: "rec-opt",
        title: "Otimização de patrimônio",
        description:
          "Seu fluxo de caixa está positivo. Considere alocar o excedente do mês em objetivos de longo prazo para evitar que o saldo fique parado.",
        action_label: "Aportar em Metas",
        action_url: "/wallets",
        impact_estimate: "Evolução contínua",
      });
    }

    // Retorno estruturado seguindo rigorosamente o contrato do Frontend!
    return {
      metrics: {
        total_balance: totalBalance,
        monthly_income: monthlyIncome,
        monthly_expenses: monthlyExpenses,
        net_savings: netSavings,
        health: {
          score,
          status,
          savings_rate: savingsRate,
          commitment_rate: commitmentRate,
          liquidity_months: liquidityMonths,
        },
        insights,
        recommendations,
        cash_flow_evolution: cashFlowEvolution,
        category_distribution: categoryDistribution,
      },
    };
  });
}
