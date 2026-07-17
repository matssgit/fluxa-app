import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db as knex } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

interface AuthUser {
  sub: string;
}

export async function subscriptionsRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  // ****** 1. ANALYTICS ******
  app.get("/analytics", async (request) => {
    const userId = (request.user as AuthUser).sub;

    const activeSubs = await knex("subscriptions").where({
      user_id: userId,
      status: "active",
    });

    const today = new Date();
    const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1)
      .toISOString()
      .substring(0, 10);
    const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0)
      .toISOString()
      .substring(0, 10);

    const incomeResult = await knex("transactions")
      .where({ user_id: userId })
      .andWhere("amount", ">", 0)
      .andWhere(function () {
        this.where("status", "completed")
          .andWhereBetween("completed_date", [startOfMonth, endOfMonth])
          .orWhere("status", "pending")
          .andWhereBetween("expected_date", [startOfMonth, endOfMonth]);
      })
      .sum("amount as totalIncome")
      .first();

    const totalIncomeMonth = Number(incomeResult?.totalIncome) || 0;

    let monthlyTotal = 0;
    let yearlyProjection = 0;
    let upcomingNext7Days = 0;

    const currentDay = today.getDate();
    const daysInMonth = new Date(
      today.getFullYear(),
      today.getMonth() + 1,
      0,
    ).getDate();

    for (const sub of activeSubs) {
      const amount = Number(sub.amount);

      if (sub.frequency === "yearly") {
        yearlyProjection += amount;
        monthlyTotal += amount / 12;
      } else {
        monthlyTotal += amount;
        yearlyProjection += amount * 12;
      }

      const dueDay = sub.due_day || 1;
      let daysUntilDue = dueDay - currentDay;

      if (daysUntilDue < 0) {
        daysUntilDue += daysInMonth;
      }

      if (daysUntilDue >= 0 && daysUntilDue <= 7) {
        upcomingNext7Days++;
      }
    }

    const budgetImpact =
      totalIncomeMonth > 0 ? (monthlyTotal / totalIncomeMonth) * 100 : 0;

    return {
      monthlyTotal,
      yearlyProjection,
      budgetImpact: Number(budgetImpact.toFixed(1)),
      upcomingNext7Days,
    };
  });

  // ****** 2. CRIAR ASSINATURAS ******
  app.post("/", async (request, reply) => {
    // LOG DE SEGURANÇA: Ver o que está a chegar EXATAMENTE antes de validar
    console.log(
      "⚡ RECEBIDO NO BACKEND:",
      JSON.stringify(request.body, null, 2),
    );

    try {
      const createSubscriptionSchema = z.object({
        title: z.string().min(1),
        amount: z.number().positive(),
        due_day: z.number().min(1).max(31).optional(),
        next_billing_date: z.string().optional(),
        frequency: z.enum(["monthly", "yearly"]),
        category_id: z.string().nullable().optional(), // Relaxado
        account_id: z.string().nullable().optional(), // Relaxado
        card_id: z.string().nullable().optional(), // Relaxado
      });

      const body = createSubscriptionSchema.parse(request.body);

      // LOG DE SEGURANÇA: Verificar se os IDs estão nulos
      console.log("🛠️ DADOS PROCESSADOS:", {
        acc: body.account_id,
        card: body.card_id,
        cat: body.category_id,
      });

      if (!body.account_id && !body.card_id) {
        throw new Error("A assinatura precisa de uma Conta ou Cartão!");
      }

      const userId = (request.user as AuthUser).sub;

      const calculatedDueDay =
        body.due_day ||
        (body.next_billing_date
          ? parseInt(body.next_billing_date.split("-")[2] || "1", 10)
          : 1);

      await knex("subscriptions").insert({
        id: randomUUID(),
        user_id: userId,
        category_id: body.category_id || null,
        account_id: body.account_id || null,
        card_id: body.card_id || null,
        title: body.title,
        amount: body.amount,
        due_day: calculatedDueDay,
        next_billing_date: body.next_billing_date || null,
        frequency: body.frequency,
        status: "active",
      });

      return reply.status(201).send();
    } catch (error: unknown) {
      console.error("🔥 ERRO FATAL:", error);
      return reply.status(400).send({
        message: "Erro de validação",
        detail: error instanceof Error ? error.message : "Erro desconhecido",
      });
    }
  });

  // ****** 3. LISTAR ASSINATURAS ******
  app.get("/", async (request) => {
    const userId = (request.user as AuthUser).sub;

    const subscriptions = await knex("subscriptions")
      .leftJoin("categories", "subscriptions.category_id", "categories.id")
      .leftJoin("accounts", "subscriptions.account_id", "accounts.id")
      .leftJoin("cards", "subscriptions.card_id", "cards.id")
      .where("subscriptions.user_id", userId)
      .select(
        "subscriptions.*",
        "categories.name as category_name",
        "categories.color as category_color",
        "accounts.name as account_name",
        "cards.name as card_name",
      )
      .orderBy("subscriptions.title", "asc");

    return { subscriptions };
  });

  // ****** 4. ALTERAR STATUS DA ASSINATURA ******
  app.patch("/:id/status", async (request, reply) => {
    const updateParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const updateBodySchema = z.object({
      status: z.enum(["active", "paused", "cancelled"]),
    });

    const { id } = updateParamsSchema.parse(request.params);
    const { status } = updateBodySchema.parse(request.body);
    const userId = (request.user as AuthUser).sub;

    const subscription = await knex("subscriptions")
      .where({ id, user_id: userId })
      .first();

    if (!subscription) {
      return reply.status(404).send({ message: "Assinatura não encontrada." });
    }

    await knex("subscriptions").where({ id }).update({ status });

    return reply.status(204).send();
  });

  // ****** 5. EXCLUIR ASSINATURA ******
  app.delete("/:id", async (request, reply) => {
    const deleteParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const { id } = deleteParamsSchema.parse(request.params);
    const userId = (request.user as AuthUser).sub;

    const subscription = await knex("subscriptions")
      .where({ id, user_id: userId })
      .first();

    if (!subscription) {
      return reply.status(404).send({ message: "Assinatura não encontrada." });
    }

    await knex("subscriptions").where({ id }).delete();

    return reply.status(204).send();
  });

  // ****** 6. PAGAR/BAIXAR ASSINATURA DO MÊS ******
  app.post("/:id/pay", async (request, reply) => {
    const payParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const payBodySchema = z.object({
      account_id: z.string().uuid("Selecione a conta para o débito"),
    });

    const { id } = payParamsSchema.parse(request.params);
    const { account_id } = payBodySchema.parse(request.body);
    const userId = (request.user as AuthUser).sub;

    const subscription = await knex("subscriptions")
      .where({ id, user_id: userId })
      .first();

    if (!subscription) {
      return reply.status(404).send({ message: "Assinatura não encontrada." });
    }

    await knex("transactions").insert({
      id: randomUUID(),
      user_id: userId,
      account_id: account_id,
      category_id: subscription.category_id,
      subscription_id: subscription.id,
      title: `Pagamento: ${subscription.title}`,
      description: "Assinatura Mensal",
      amount: subscription.amount,
      status: "completed",
      completed_date: new Date().toISOString().split("T")[0],
    });

    return reply.status(201).send();
  });
}
