import { z } from "zod";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";

export async function transactionsRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  app.post("/", async (request, reply) => {
    const createTransactionSchema = z
      .object({
        title: z.string(),
        amount: z.number(),
        account_id: z.string().uuid("Conta é obrigatória"),
        category_id: z.string().uuid("Categoria é obrigatória").optional(),
        description: z.string().optional(),
        observation: z.string().optional(),
        status: z.enum(["pending", "completed"]).default("completed"),
        type: z.string().optional(),
        expected_date: z.string().optional(),
        completed_date: z.string().optional(),
        date: z.string().optional(),
      })
      .transform((data) => {
        let finalType = data.type ? String(data.type).toLowerCase() : "";

        // Infere o tipo da transação pelo sinal matemático caso não seja enviado explicitamente
        if (!finalType) {
          finalType = data.amount >= 0 ? "entrada" : "saida";
        } else if (finalType === "income" || finalType === "receita") {
          finalType = "entrada";
        } else if (finalType === "expense" || finalType === "despesa") {
          finalType = "saida";
        }

        const todayStr = new Date().toISOString().split("T")[0];
        const effectiveDate =
          data.completed_date || data.expected_date || data.date || todayStr;

        return {
          ...data,
          type: finalType,
          amount: Math.abs(data.amount),
          expected_date: data.expected_date || effectiveDate,
          completed_date:
            data.status === "completed"
              ? effectiveDate
              : data.completed_date || null,
        };
      });

    const body = createTransactionSchema.parse(request.body);
    const userId = (request.user as any).sub;

    const account = await db("accounts")
      .where({ id: body.account_id, user_id: userId })
      .first();

    if (!account) {
      return reply.status(403).send({
        error: "Acesso negado. Conta inválida ou não pertence ao usuário.",
      });
    }

    if (body.category_id) {
      const category = await db("categories")
        .where({ id: body.category_id, user_id: userId })
        .first();

      if (!category) {
        return reply.status(403).send({
          error:
            "Acesso negado. Categoria inválida ou não pertence ao usuário.",
        });
      }
    }

    await db("transactions").insert({
      id: randomUUID(),
      user_id: userId,
      title: body.title,
      amount: body.amount,
      account_id: body.account_id,
      category_id: body.category_id,
      description: body.description,
      observation: body.observation,
      status: body.status,
      type: body.type,
      expected_date: body.expected_date,
      completed_date: body.completed_date,
    });

    return reply.status(201).send();
  });

  app.get("/", async (request) => {
    const userId = (request.user as any).sub;

    const transactions = await db("transactions")
      .leftJoin("accounts", "transactions.account_id", "accounts.id")
      .leftJoin("categories", "transactions.category_id", "categories.id")
      .where("transactions.user_id", userId)
      .select(
        "transactions.*",
        "accounts.name as account_name",
        "categories.name as category_name",
      )
      .orderBy("transactions.created_at", "desc");

    return { transactions };
  });

  app.get("/summary", async (request) => {
    const userId = (request.user as any).sub;

    const transactions = await db("transactions")
      .where({ user_id: userId, status: "completed" })
      .select("amount", "type");

    const summary = transactions.reduce(
      (acc, transaction) => {
        const amount = Number(transaction.amount);
        const typeStr = String(transaction.type || "").toLowerCase();

        if (
          typeStr === "entrada" ||
          typeStr === "income" ||
          typeStr === "receita" ||
          (typeStr === "" && amount > 0)
        ) {
          acc.income += Math.abs(amount);
          acc.amount += Math.abs(amount);
        } else {
          acc.expense += Math.abs(amount);
          acc.amount -= Math.abs(amount);
        }
        return acc;
      },
      { amount: 0, income: 0, expense: 0 },
    );

    return { summary };
  });

  app.patch("/:id/complete", async (request, reply) => {
    const updateParamsSchema = z.object({
      id: z.string().uuid("ID inválido"),
    });

    const { id } = updateParamsSchema.parse(request.params);
    const userId = (request.user as any).sub;

    const transaction = await db("transactions")
      .where({ id, user_id: userId })
      .first();

    if (!transaction) {
      return reply.status(404).send({ message: "Lançamento não encontrado." });
    }

    const today = new Date().toISOString().split("T")[0];

    await db("transactions").where({ id }).update({
      status: "completed",
      completed_date: today,
    });

    return reply.status(204).send();
  });
}
