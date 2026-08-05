import { db as knex } from "../database/database.js";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import { endOfMonth, startOfMonth, format } from "date-fns";

export async function dashboardRoutes(app: FastifyInstance) {
  app.get("/", { preHandler: [checkAuth] }, async (request, reply) => {
    const userId = (request.user as any).sub;

    const now = new Date();
    const monthStart = format(startOfMonth(now), "yyyy-MM-dd");
    const monthEnd = format(endOfMonth(now), "yyyy-MM-dd");
    const monthReference = format(now, "yyyy-MM");

    const balanceResult = await knex("transactions")
      .where({ user_id: userId, status: "completed" })
      .select("amount", "type");

    let currentBalance = 0;
    balanceResult.forEach((t) => {
      const num = Number(t.amount || 0);
      const typeStr = String(t.type || "").toLowerCase();

      // Inteligência relacional de legado: Se type for nulo/antigo, infere receita caso amount seja > 0
      if (
        typeStr === "entrada" ||
        typeStr === "income" ||
        typeStr === "receita" ||
        (typeStr === "" && num > 0)
      ) {
        currentBalance += Math.abs(num);
      } else {
        currentBalance -= Math.abs(num);
      }
    });

    const completedMonthTransactions = await knex("transactions")
      .where({ user_id: userId, status: "completed" })
      .where(function () {
        this.whereBetween("completed_date", [monthStart, monthEnd])
          .orWhereBetween("expected_date", [monthStart, monthEnd])
          .orWhereBetween("created_at", [monthStart, monthEnd]);
      })
      .select("type", "amount");

    let totalIncome = 0;
    let totalExpenses = 0;
    completedMonthTransactions.forEach((t) => {
      const num = Number(t.amount || 0);
      const val = Math.abs(num);
      const typeStr = String(t.type || "").toLowerCase();
      if (
        typeStr === "entrada" ||
        typeStr === "income" ||
        typeStr === "receita" ||
        (typeStr === "" && num > 0)
      ) {
        totalIncome += val;
      } else {
        totalExpenses += val;
      }
    });

    const allActiveSubscriptions = await knex("subscriptions").where({
      user_id: userId,
      status: "active",
    });

    const paidSubscriptionsThisMonth = await knex("transactions")
      .where({ user_id: userId, status: "completed" })
      .whereNotNull("subscription_id")
      .where(function () {
        this.whereBetween("completed_date", [monthStart, monthEnd])
          .orWhereBetween("expected_date", [monthStart, monthEnd])
          .orWhereBetween("created_at", [monthStart, monthEnd]);
      })
      .select("subscription_id");

    const paidIds = paidSubscriptionsThisMonth.map((t) => t.subscription_id);

    const pendingSubscriptions = allActiveSubscriptions.filter(
      (sub) => !paidIds.includes(sub.id),
    );

    const totalSubscriptionsAmount = pendingSubscriptions.reduce(
      (acc, sub) => acc + Number(sub.amount || 0),
      0,
    );

    const pendingMonthTransactions = await knex("transactions")
      .where({ user_id: userId, status: "pending" })
      .where(function () {
        this.whereBetween("expected_date", [monthStart, monthEnd])
          .orWhereBetween("completed_date", [monthStart, monthEnd])
          .orWhereBetween("created_at", [monthStart, monthEnd]);
      })
      .select("type", "amount");

    let expectedIncome = 0;
    let pendingExpensesOnly = 0;
    pendingMonthTransactions.forEach((t) => {
      const num = Number(t.amount || 0);
      const val = Math.abs(num);
      const typeStr = String(t.type || "").toLowerCase();
      if (
        typeStr === "entrada" ||
        typeStr === "income" ||
        typeStr === "receita" ||
        (typeStr === "" && num > 0)
      ) {
        expectedIncome += val;
      } else {
        pendingExpensesOnly += val;
      }
    });

    const installmentsPending = await knex("installments")
      .join(
        "credit_purchases",
        "installments.purchase_id",
        "credit_purchases.id",
      )
      .join("cards", "credit_purchases.card_id", "cards.id")
      .where({ "cards.user_id": userId, "installments.status": "pending" })
      .whereBetween("installments.expected_date", [monthStart, monthEnd])
      .sum("installments.amount as total")
      .first<{ total: number | null }>();

    const expectedExpenses = pendingExpensesOnly + totalSubscriptionsAmount;
    const pendingInstallments = Number(installmentsPending?.total || 0);

    const projectedBalance =
      currentBalance + expectedIncome - expectedExpenses - pendingInstallments;

    const formatDueDateBR = (dateVal: unknown): string => {
      if (!dateVal) return "Mês atual";
      const d = new Date(String(dateVal));
      if (isNaN(d.getTime())) return String(dateVal);
      return format(d, "dd/MM/yyyy");
    };

    const pendingTransactionsList = await knex("transactions")
      .where({ user_id: userId, status: "pending" })
      .where(function () {
        this.whereBetween("expected_date", [
          monthStart,
          monthEnd,
        ]).orWhereBetween("created_at", [monthStart, monthEnd]);
      })
      .select(
        "id",
        "title",
        "description",
        "amount",
        "expected_date as dueDate",
        "type",
      );

    const filteredPendingExpenses = pendingTransactionsList.filter((t) => {
      const num = Number(t.amount || 0);
      const typeStr = String(t.type || "").toLowerCase();
      return !(
        typeStr === "entrada" ||
        typeStr === "income" ||
        typeStr === "receita" ||
        (typeStr === "" && num > 0)
      );
    });

    const pendingInstallmentsList = await knex("installments")
      .join(
        "credit_purchases",
        "installments.purchase_id",
        "credit_purchases.id",
      )
      .join("cards", "credit_purchases.card_id", "cards.id")
      .where({ "cards.user_id": userId, "installments.status": "pending" })
      .whereBetween("installments.expected_date", [monthStart, monthEnd])
      .select(
        "installments.id",
        "credit_purchases.title",
        "installments.amount",
        "installments.expected_date as dueDate",
        "installments.installment_number",
        "credit_purchases.total_installments",
      );

    const pendencies = [
      ...filteredPendingExpenses.map((t) => ({
        id: t.id,
        type: "transaction" as const,
        title: t.title || t.description || "Despesa pendente",
        amount: Math.abs(Number(t.amount || 0)),
        dueDate: formatDueDateBR(t.dueDate),
        info: `Vence dia ${formatDueDateBR(t.dueDate).substring(0, 2)}`,
      })),
      ...pendingInstallmentsList.map((i) => ({
        id: i.id,
        type: "installment" as const,
        title: `Parcela ${i.installment_number}/${i.total_installments} - ${i.title}`,
        amount: Math.abs(Number(i.amount || 0)),
        dueDate: formatDueDateBR(i.dueDate),
        info: `Vence dia ${formatDueDateBR(i.dueDate).substring(0, 2)}`,
      })),
      ...pendingSubscriptions.map((s) => ({
        id: s.id,
        type: "subscription" as const,
        title: `Assinatura - ${s.title}`,
        amount: Math.abs(Number(s.amount || 0)),
        dueDate: `${String(s.due_day).padStart(2, "0")}/${monthReference.split("-")[1]}/${monthReference.split("-")[0]}`,
        info: `Vence dia ${String(s.due_day).padStart(2, "0")}`,
      })),
    ];

    const timeline = await knex("transactions")
      .where({ user_id: userId, status: "completed" })
      .orderBy("created_at", "desc")
      .limit(10)
      .select(
        "id",
        "title",
        "description",
        "amount",
        "completed_date",
        "expected_date",
        "created_at",
        "type",
      );

    return reply.send({
      monthReference,
      summary: {
        currentBalance: Number(currentBalance || 0),
        totalIncome: Number(totalIncome || 0),
        totalExpenses: Number(totalExpenses || 0),
      },
      projection: {
        projectedBalance: Number(projectedBalance || 0),
        expectedIncome: Number(expectedIncome || 0),
        expectedExpenses: Number(expectedExpenses || 0),
        pendingInstallments: Number(pendingInstallments || 0),
      },
      pendencies,
      timeline: timeline.map((t) => {
        const num = Number(t.amount || 0);
        const typeStr = String(t.type || "").toLowerCase();
        const isIncome =
          typeStr === "entrada" ||
          typeStr === "income" ||
          typeStr === "receita" ||
          (typeStr === "" && num > 0);
        const effDate = t.completed_date || t.expected_date || t.created_at;
        return {
          id: t.id,
          type: "cash" as const,
          title: t.title || t.description || "Movimentação",
          description: "Movimentação de caixa",
          amount: Math.abs(num),
          date: formatDueDateBR(effDate),
          cashType: isIncome ? "entrada" : "saida",
        };
      }),
      alerts: [],
    });
  });
}
