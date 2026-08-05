import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db as knex } from "../database/database.js";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";

export async function walletsRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  app.get("/", async (request) => {
    const { sub: userId } = request.user;
    const wallets = await knex("wallets")
      .where({ user_id: userId })
      .orderBy("created_at", "desc");
    return { wallets };
  });

  app.get("/:id", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id } = paramsSchema.parse(request.params);

    const wallet = await knex("wallets").where({ id, user_id: userId }).first();

    if (!wallet) {
      return reply.status(404).send({ error: "Objetivo não encontrado." });
    }

    const history = await knex("wallet_history")
      .where({ wallet_id: id })
      .orderBy("created_at", "desc");

    return { wallet, history };
  });

  app.post("/", async (request, reply) => {
    const { sub: userId } = request.user;

    const createWalletSchema = z.object({
      title: z.string().min(2),
      description: z.string().optional(),
      target_amount: z.number().positive(),
      current_amount: z.number().min(0).default(0),
      deadline: z.string().nullable().optional(),
      color: z.string().default("brand"),
    });

    const data = createWalletSchema.parse(request.body);

    const initialAmount = Math.min(data.current_amount, data.target_amount);
    const initialStatus =
      initialAmount >= data.target_amount ? "completed" : "active";

    await knex("wallets").insert({
      id: randomUUID(),
      user_id: userId,
      title: data.title,
      description: data.description || null,
      target_amount: data.target_amount,
      current_amount: initialAmount,
      deadline: data.deadline ? new Date(data.deadline) : null,
      color: data.color,
      status: initialStatus,
    });

    return reply.status(201).send();
  });

  app.patch("/:id", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id } = paramsSchema.parse(request.params);

    const updateSchema = z.object({
      title: z.string().min(2).optional(),
      description: z.string().optional(),
      target_amount: z.number().positive().optional(),
      deadline: z.string().nullable().optional(),
      status: z.enum(["active", "completed", "paused"]).optional(),
      color: z.string().optional(),
    });

    const data = updateSchema.parse(request.body);

    const updatedRows = await knex("wallets")
      .where({ id, user_id: userId })
      .update({
        ...data,
        deadline:
          data.deadline !== undefined
            ? data.deadline
              ? new Date(data.deadline)
              : null
            : undefined,
        updated_at: knex.fn.now(),
      });

    if (updatedRows === 0) {
      return reply.status(404).send({ error: "Objetivo não encontrado." });
    }

    return reply.status(204).send();
  });

  app.delete("/:id", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id } = paramsSchema.parse(request.params);

    const deletedRows = await knex("wallets")
      .where({ id, user_id: userId })
      .delete();

    if (deletedRows === 0) {
      return reply.status(404).send({ error: "Objetivo não encontrado." });
    }

    return reply.status(204).send();
  });

  app.post("/:id/progress", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id: walletId } = paramsSchema.parse(request.params);

    const progressSchema = z.object({
      type: z.enum(["deposit", "withdraw"]),
      amount: z.number().positive(),
      observation: z.string().optional(),
    });

    const { type, amount, observation } = progressSchema.parse(request.body);

    try {
      await knex.transaction(async (trx) => {
        const wallet = await trx("wallets")
          .where({ id: walletId, user_id: userId })
          .first();

        if (!wallet) {
          throw new Error("Objetivo não encontrado.");
        }

        const currentWalletAmount = Number(wallet.current_amount);
        const targetAmount = Number(wallet.target_amount);

        let newWalletAmount = 0;
        let newStatus = wallet.status;

        if (type === "deposit") {
          newWalletAmount = Math.min(
            currentWalletAmount + amount,
            targetAmount,
          );
          if (newWalletAmount >= targetAmount) {
            newStatus = "completed";
          }
        } else {
          newWalletAmount = Math.max(currentWalletAmount - amount, 0);
          if (newWalletAmount < targetAmount && wallet.status === "completed") {
            newStatus = "active";
          }
        }

        await trx("wallets").where({ id: walletId }).update({
          current_amount: newWalletAmount,
          status: newStatus,
          updated_at: knex.fn.now(),
        });

        await trx("wallet_history").insert({
          id: randomUUID(),
          wallet_id: walletId,
          type: type,
          amount: amount,
          observation: observation || null,
          created_at: new Date(),
        });
      });

      return reply.status(200).send();
    } catch (error: unknown) {
      if (error instanceof Error) {
        return reply.status(400).send({ message: error.message });
      }
      return reply
        .status(500)
        .send({ message: "Erro ao atualizar progresso." });
    }
  });
}
