import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db as knex } from "../database/database.js";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import {
  paySubscription,
  SubscriptionPaymentError,
} from "../services/subscription-payments.service.js";
import {
  getTransactionDirection,
  getTransactionMagnitude,
} from "../domain/transaction-money.js";

interface AuthUser {
  sub: string;
}

export async function subscriptionsRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

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

    const incomeTransactions = await knex("transactions")
      .where({ user_id: userId })
      .andWhere(function () {
        this.where("status", "completed")
          .andWhereBetween("completed_date", [startOfMonth, endOfMonth])
          .orWhere("status", "pending")
          .andWhereBetween("expected_date", [startOfMonth, endOfMonth]);
      })
      .select("amount", "type", "subscription_id");

    const totalIncomeMonth = incomeTransactions.reduce(
      (total, transaction) =>
        getTransactionDirection(transaction) === "entrada"
          ? total + getTransactionMagnitude(transaction)
          : total,
      0,
    );

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

  app.post("/", async (request, reply) => {
    try {
      const createSubscriptionSchema = z.object({
        title: z.string().min(1),
        amount: z.number().positive(),
        due_day: z.number().min(1).max(31).optional(),
        next_billing_date: z.string().optional(),
        frequency: z.enum(["monthly", "yearly"]),
        category_id: z.string().uuid(),
        account_id: z.string().nullable().optional(),
        card_id: z.string().nullable().optional(),
      }).strict();

      const body = createSubscriptionSchema.parse(request.body);

      if (!body.account_id && !body.card_id) {
        return reply.status(400).send({
          message: "A assinatura precisa de uma conta ou cartão.",
        });
      }

      const userId = (request.user as AuthUser).sub;

      if (body.account_id) {
        const acc = await knex("accounts")
          .where({ id: body.account_id, user_id: userId })
          .first();
        if (!acc) {
          return reply
            .status(403)
            .send({ detail: "Conta inválida ou não pertence a você." });
        }
      }

      if (body.card_id) {
        const card = await knex("cards")
          .where({ id: body.card_id, user_id: userId })
          .first();
        if (!card) {
          return reply
            .status(403)
            .send({ detail: "Cartão inválido ou não pertence a você." });
        }
      }

      const cat = await knex("categories")
        .where({ id: body.category_id, user_id: userId })
        .first();
      if (!cat) {
        return reply
          .status(403)
          .send({ detail: "Categoria inválida ou não pertence a você." });
      }

      const calculatedDueDay =
        body.due_day ||
        (body.next_billing_date
          ? parseInt(body.next_billing_date.split("-")[2] || "1", 10)
          : 1);

      await knex("subscriptions").insert({
        id: randomUUID(),
        user_id: userId,
        category_id: body.category_id,
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
      if (error instanceof z.ZodError) throw error;
      console.error("Erro ao criar assinatura:", error);
      return reply
        .status(500)
        .send({ message: "Erro interno ao criar assinatura." });
    }
  });

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

    try {
      await paySubscription({
        subscriptionId: id,
        userId,
        accountId: account_id,
      });
      return reply.status(201).send();
    } catch (error) {
      if (error instanceof SubscriptionPaymentError) {
        if (error.code === "SUBSCRIPTION_NOT_FOUND") {
          return reply
            .status(404)
            .send({ message: "Assinatura não encontrada." });
        }
        if (error.code === "ACCOUNT_NOT_FOUND") {
          return reply.status(403).send({
            message: "Operação negada. Conta inválida ou não pertence a você.",
          });
        }
        if (error.code === "ALREADY_PAID") {
          return reply
            .status(409)
            .send({ message: "Assinatura já paga nesta competência." });
        }
        return reply
          .status(400)
          .send({ message: "Assinatura não está disponível para pagamento." });
      }
      throw error;
    }
  });

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

  app.delete("/:id", async (request, reply) => {
    const deleteParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const { id } = deleteParamsSchema.parse(request.params);
    const userId = (request.user as AuthUser).sub;

    const deleted = await knex("subscriptions")
      .where({ id, user_id: userId })
      .delete();

    if (deleted === 0) {
      return reply.status(404).send({ message: "Assinatura não encontrada." });
    }

    return reply.status(204).send();
  });
}
