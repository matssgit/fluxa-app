import type { FastifyInstance } from "fastify";
import { db as knex } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";
import { endOfMonth, startOfMonth, format } from "date-fns";

export async function dashboardRoutes(app: FastifyInstance) {
   app.get("/", { preHandler: [checkAuth] }, async (request, reply) => {
      const userId = (request.user as any).sub;

      // Define o intervalo do mês corrente para garantir a integridade da projeção
      const now = new Date();
      const monthStart = format(startOfMonth(now), "yyyy-MM-dd");
      const monthEnd = format(endOfMonth(now), "yyyy-MM-dd");
      const monthReference = format(now, "yyyy-MM");

      // 1. RESUMO FINANCEIRO (Dinheiro real já consolidado)
      const balanceResult = await knex("transactions")
         .where({ user_id: userId, status: "completed" })
         .select(
            knex.raw(
               "SUM(CASE WHEN type = 'entrada' THEN amount ELSE -amount END) as total",
            ),
         )
         .first<{ total: number | null }>();

      const accountSum = { total: Number(balanceResult?.total || 0) };

      const completedMonthTransactions = await knex("transactions")
         .where({ user_id: userId, status: "completed" })
         .whereBetween("completed_date", [monthStart, monthEnd])
         .select("type")
         .sum("amount as total")
         .groupBy("type");

      let totalIncome = 0;
      let totalExpenses = 0;
      completedMonthTransactions.forEach((t) => {
         if (t.type === "entrada") totalIncome = Number(t.total || 0);
         if (t.type === "saida") totalExpenses = Number(t.total || 0);
      });

      // BUSCA DE ASSINATURAS ATIVAS (Módulo Novo Integrado)
      // 1.5 FILTRO DE ASSINATURAS (Só exibe as que ainda NÃO foram pagas este mês)
      const allActiveSubscriptions = await knex("subscriptions").where({
         user_id: userId,
         status: "active",
      });

      // Busca quais assinaturas já geraram transação no mês corrente
      const paidSubscriptionsThisMonth = await knex("transactions")
         .where({ user_id: userId, status: "completed" })
         .whereNotNull("subscription_id")
         .whereBetween("completed_date", [monthStart, monthEnd])
         .select("subscription_id");

      const paidIds = paidSubscriptionsThisMonth.map((t) => t.subscription_id);

      // Assinaturas pendentes são as ativas que NÃO estão na lista de pagas
      const pendingSubscriptions = allActiveSubscriptions.filter(
         (sub) => !paidIds.includes(sub.id),
      );

      // O cálculo do peso na projeção agora olha só para o que falta pagar
      const totalSubscriptionsAmount = pendingSubscriptions.reduce(
         (acc, sub) => acc + Number(sub.amount || 0),
         0,
      );

      // 2. PROJEÇÃO (Eventos pendentes + Assinaturas do mês corrente)
      const [incomePending, expensePending, installmentsPending] =
         await Promise.all([
            knex("transactions")
               .where({ user_id: userId, status: "pending", type: "entrada" })
               .whereBetween("expected_date", [monthStart, monthEnd])
               .sum("amount as total")
               .first<{ total: number | null }>(),
            knex("transactions")
               .where({ user_id: userId, status: "pending", type: "saida" })
               .whereBetween("expected_date", [monthStart, monthEnd])
               .sum("amount as total")
               .first<{ total: number | null }>(),
            knex("installments")
               .join(
                  "credit_purchases",
                  "installments.purchase_id",
                  "credit_purchases.id",
               )
               .join("cards", "credit_purchases.card_id", "cards.id")
               .where({
                  "cards.user_id": userId,
                  "installments.status": "pending",
               })
               .whereBetween("installments.expected_date", [
                  monthStart,
                  monthEnd,
               ])
               .sum("installments.amount as total")
               .first<{ total: number | null }>(),
         ]);

      const expectedIncome = Number(incomePending?.total || 0);

      // As assinaturas entram somando nas despesas previstas do mês
      const expectedExpenses =
         Number(expensePending?.total || 0) + totalSubscriptionsAmount;
      const pendingInstallments = Number(installmentsPending?.total || 0);

      // O cálculo preditivo agora subtrai as assinaturas automaticamente do seu bolso
      const projectedBalance =
         Number(accountSum.total) +
         expectedIncome -
         expectedExpenses -
         pendingInstallments;

      // 3. PENDÊNCIAS (Lista global de atenção)
      const pendingTransactions = await knex("transactions")
         .where({ user_id: userId, status: "pending", type: "saida" })
         .whereBetween("expected_date", [monthStart, monthEnd])
         .select(
            "id",
            "description as title",
            "amount",
            "expected_date as dueDate",
         );

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

      // Montagem do Array de Pendências incluindo Assinaturas do Mês
      const pendencies = [
         ...pendingTransactions.map((t) => ({
            id: t.id,
            type: "transaction" as const,
            title: t.title,
            amount: Number(t.amount || 0),
            dueDate: String(t.dueDate),
            info: `Vence dia ${format(new Date(t.dueDate), "dd")}`,
         })),
         ...pendingInstallmentsList.map((i) => ({
            id: i.id,
            type: "installment" as const,
            title: `Parcela ${i.installment_number}/${i.total_installments} - ${i.title}`,
            amount: Number(i.amount || 0),
            dueDate: String(i.dueDate),
            info: `Vence dia ${format(new Date(i.dueDate), "dd")}`,
         })),
         // Injetando as Assinaturas como compromisso pendente na visão geral
         ...pendingSubscriptions.map((s) => ({
            id: s.id,
            type: "subscription" as const,
            title: `Assinatura - ${s.title}`,
            amount: Number(s.amount || 0),
            dueDate: `${monthReference}-${String(s.due_day).padStart(2, "0")}`,
            info: `Vence dia ${String(s.due_day).padStart(2, "0")}`,
         })),
      ];

      // 4. TIMELINE (Histórico consolidado)
      const timeline = await knex("transactions")
         .where({ user_id: userId, status: "completed" })
         .orderBy("completed_date", "desc")
         .limit(10)
         .select(
            "id",
            "description as title",
            "amount",
            "completed_date as date",
            "type",
         );

      return reply.send({
         monthReference,
         summary: {
            currentBalance: Number(accountSum.total || 0),
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
         timeline: timeline.map((t) => ({
            id: t.id,
            type: "cash" as const,
            title: t.title,
            description: "Movimentação de caixa",
            amount: Number(t.amount || 0),
            date: String(t.date),
            cashType: t.type as "entrada" | "saida",
         })),
         alerts: [],
      });
   });
}
