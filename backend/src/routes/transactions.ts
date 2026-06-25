import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import { checkAuth } from "../middlewares/check-auto.js";

export async function transactionsRoutes(app: FastifyInstance) {
   // Protege todas as rotas
   app.addHook("preHandler", checkAuth);

   // ****** 1. CRIAR TRANSAÇÃO (O que tinha sumido) ******
   app.post("/", async (request, reply) => {
      const createTransactionSchema = z.object({
         title: z.string(),
         amount: z.number(),
         account_id: z.string().uuid("Conta é obrigatória"),
         category_id: z.string().uuid("Categoria é obrigatória").optional(),
         description: z.string().optional(),
         observation: z.string().optional(),
         status: z.enum(["pending", "completed"]).default("completed"),
         expected_date: z.string().optional(),
         completed_date: z.string().optional(),
      });

      const body = createTransactionSchema.parse(request.body);
      const userId = (request.user as any).sub;

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
         expected_date: body.expected_date,
         completed_date: body.completed_date,
      });

      return reply.status(201).send();
   });

   // ****** 2. LISTAR TRANSAÇÕES (Com o Join da Conta e Categoria) ******
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

   // ****** 3. RESUMO / SUMMARY (Soma apenas as concluídas) ******
   app.get("/summary", async (request) => {
      const userId = (request.user as any).sub;

      // Arquitetura: O saldo real soma apenas transações 'completed'
      const transactions = await db("transactions")
         .where({
            user_id: userId,
            status: "completed",
         })
         .select("amount");

      const summary = transactions.reduce(
         (acc, transaction) => {
            // Converte a string do Postgres para Número Decimal
            const amount = Number(transaction.amount);

            if (amount > 0) {
               acc.income += amount;
            } else {
               acc.expense += amount;
            }
            acc.amount += amount;
            return acc;
         },
         { amount: 0, income: 0, expense: 0 },
      );

      return { summary };
   });

   // ****** 4. DAR BAIXA EM UMA PENDÊNCIA ******
   app.patch("/:id/complete", async (request, reply) => {
      const updateParamsSchema = z.object({
         id: z.string().uuid("ID inválido"),
      });

      const { id } = updateParamsSchema.parse(request.params);
      const userId = (request.user as any).sub;

      // Garante que a transação pertence ao usuário logado
      const transaction = await db("transactions")
         .where({ id, user_id: userId })
         .first();

      if (!transaction) {
         return reply
            .status(404)
            .send({ message: "Lançamento não encontrado." });
      }

      const today = new Date().toISOString().split("T")[0];

      // Atualiza o status e a data de conclusão
      await db("transactions").where({ id }).update({
         status: "completed",
         completed_date: today,
      });

      return reply.status(204).send();
   });
}
