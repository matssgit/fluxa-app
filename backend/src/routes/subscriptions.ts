import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db as knex } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

export async function subscriptionsRoutes(app: FastifyInstance) {
   // Protege todas as rotas de assinaturas
   app.addHook("preHandler", checkAuth);

   // ****** 1. CRIAR UMA ASSINATURA ******
   app.post("/", async (request, reply) => {
      const createSubscriptionSchema = z
         .object({
            title: z.string().min(1, "O nome é obrigatório"),
            amount: z.number().positive("O valor deve ser positivo"),
            due_day: z.number().min(1).max(31, "Dia inválido"),
            frequency: z.enum(["monthly", "yearly"]).default("monthly"),
            category_id: z.string().uuid("Categoria inválida"),
            account_id: z.string().uuid().optional().nullable(),
            card_id: z.string().uuid().optional().nullable(),
         })
         .refine((data) => data.account_id || data.card_id, {
            message:
               "A assinatura deve estar vinculada a uma Conta ou a um Cartão",
            path: ["account_id"],
         });

      const body = createSubscriptionSchema.parse(request.body);
      const userId = (request.user as any).sub;

      await knex("subscriptions").insert({
         id: randomUUID(),
         user_id: userId,
         category_id: body.category_id,
         account_id: body.account_id,
         card_id: body.card_id,
         title: body.title,
         amount: body.amount,
         due_day: body.due_day,
         frequency: body.frequency,
         status: "active",
      });

      return reply.status(201).send();
   });

   // ****** 2. LISTAR ASSINATURAS (Com nomes da Categoria e Método de Pagamento) ******
   app.get("/", async (request) => {
      const userId = (request.user as any).sub;

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

   // ****** 3. ALTERAR STATUS DA ASSINATURA (Ativar, Pausar, Cancelar) ******
   app.patch("/:id/status", async (request, reply) => {
      const updateParamsSchema = z.object({
         id: z.string().uuid("ID inválido"),
      });

      const updateBodySchema = z.object({
         status: z.enum(["active", "paused", "cancelled"]),
      });

      const { id } = updateParamsSchema.parse(request.params);
      const { status } = updateBodySchema.parse(request.body);
      const userId = (request.user as any).sub;

      const subscription = await knex("subscriptions")
         .where({ id, user_id: userId })
         .first();

      if (!subscription) {
         return reply
            .status(404)
            .send({ message: "Assinatura não encontrada." });
      }

      await knex("subscriptions").where({ id }).update({ status });

      return reply.status(204).send();
   });

   // ****** 4. EXCLUIR ASSINATURA ******
   app.delete("/:id", async (request, reply) => {
      const deleteParamsSchema = z.object({
         id: z.string().uuid("ID inválido"),
      });

      const { id } = deleteParamsSchema.parse(request.params);
      const userId = (request.user as any).sub;

      const subscription = await knex("subscriptions")
         .where({ id, user_id: userId })
         .first();

      if (!subscription) {
         return reply
            .status(404)
            .send({ message: "Assinatura não encontrada." });
      }

      await knex("subscriptions").where({ id }).delete();

      return reply.status(204).send();
   });

   // ****** 5. PAGAR/BAIXAR ASSINATURA DO MÊS ******
   app.post("/:id/pay", async (request, reply) => {
      const payParamsSchema = z.object({
         id: z.string().uuid("ID inválido"),
      });

      const payBodySchema = z.object({
         account_id: z.string().uuid("Selecione a conta para o débito"),
      });

      const { id } = payParamsSchema.parse(request.params);
      const { account_id } = payBodySchema.parse(request.body);
      const userId = (request.user as any).sub;

      // 1. Busca a assinatura para pegar valor, nome e categoria
      const subscription = await knex("subscriptions")
         .where({ id, user_id: userId })
         .first();

      if (!subscription) {
         return reply
            .status(404)
            .send({ message: "Assinatura não encontrada." });
      }

      // 2. Registra a despesa real no Fluxo de Caixa vinculando o ID da assinatura
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
