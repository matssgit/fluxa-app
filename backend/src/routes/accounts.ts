import { z } from "zod";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import { getSignedTransactionAmount } from "../domain/transaction-money.js";

export async function accountsRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  app.post("/", async (request, reply) => {
    const createAccountSchema = z.object({
      name: z.string().min(2),
      type: z.enum(["checking", "wallet", "savings"]),
    });

    const { name, type } = createAccountSchema.parse(request.body);
    const userId = (request.user as any).sub;

    await db("accounts").insert({
      id: randomUUID(),
      user_id: userId,
      name,
      type,
    });

    return reply.status(201).send();
  });

  app.get("/", async (request) => {
    const userId = (request.user as any).sub;

    const accounts = await db("accounts").where({ user_id: userId }).select();

    const transactions = await db("transactions")
      .where({ user_id: userId, status: "completed" })
      .select("account_id", "amount", "type", "subscription_id");

    const balances = new Map<string, number>();
    transactions.forEach((transaction) => {
      if (!transaction.account_id) return;
      balances.set(
        transaction.account_id,
        (balances.get(transaction.account_id) || 0) +
          getSignedTransactionAmount(transaction),
      );
    });

    const accountsWithBalance = accounts.map((acc) => {
      return {
        ...acc,
        balance: balances.get(acc.id) || 0,
      };
    });

    return { accounts: accountsWithBalance };
  });

  app.put("/:id", async (request, reply) => {
    const updateParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const updateBodySchema = z.object({
      name: z.string().min(2),
      type: z.enum(["checking", "wallet", "savings"]),
    });

    const { id } = updateParamsSchema.parse(request.params);
    const { name, type } = updateBodySchema.parse(request.body);
    const userId = (request.user as any).sub;

    const account = await db("accounts").where({ id, user_id: userId }).first();

    if (!account) {
      return reply.status(404).send({ message: "Conta não encontrada." });
    }

    await db("accounts").where({ id }).update({ name, type });
    return reply.status(204).send();
  });

  app.delete("/:id", async (request, reply) => {
    const deleteParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const { id } = deleteParamsSchema.parse(request.params);
    const userId = (request.user as any).sub;

    const account = await db("accounts").where({ id, user_id: userId }).first();

    if (!account) {
      return reply.status(404).send({ message: "Conta não encontrada." });
    }

    const [txCount, subCount] = await Promise.all([
      db("transactions").where({ account_id: id }).count("id as count").first(),
      db("subscriptions")
        .where({ account_id: id })
        .count("id as count")
        .first(),
    ]);

    const transactions = Number(txCount?.count || 0);
    const subscriptions = Number(subCount?.count || 0);

    // Fixado em zero temporariamente para o Frontend não quebrar o contrato do JSON
    const cards = 0;

    if (transactions > 0 || subscriptions > 0) {
      return reply.status(409).send({
        message: "Não foi possível remover esta conta.",
        conflicts: { transactions, cards, subscriptions },
      });
    }

    await db("accounts").where({ id }).delete();
    return reply.status(204).send();
  });
}
