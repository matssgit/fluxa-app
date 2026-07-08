import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db as knex } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

export async function walletsRoutes(app: FastifyInstance) {
  // Todas as rotas de caixinhas exigem usuário autenticado
  app.addHook("preHandler", checkAuth);

  // 1. LISTAR TODAS AS ESTUFAS DO USUÁRIO
  app.get("/", async (request) => {
    const { sub: userId } = request.user;

    const wallets = await knex("wallets")
      .where({ user_id: userId })
      .orderBy("created_at", "desc");

    return { wallets };
  });

  // 2. BUSCAR ESTUFA POR ID
  app.get("/:id", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id } = paramsSchema.parse(request.params);

    const wallet = await knex("wallets").where({ id, user_id: userId }).first();

    if (!wallet) {
      return reply
        .status(404)
        .send({ error: "Estufa não encontrada no seu ecossistema." });
    }

    return { wallet };
  });

  // 3. CRIAR NOVA ESTUFA
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

    // Se já nascer com valor maior ou igual à meta, já inicia como completed
    const initialStatus =
      data.current_amount >= data.target_amount ? "completed" : "active";

    await knex("wallets").insert({
      id: randomUUID(),
      user_id: userId,
      title: data.title,
      description: data.description || null,
      target_amount: data.target_amount,
      current_amount: data.current_amount,
      deadline: data.deadline ? new Date(data.deadline) : null,
      color: data.color,
      status: initialStatus,
    });

    return reply.status(201).send();
  });

  // 4. ATUALIZAR ESTUFA / STATUS
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
      return reply.status(404).send({ error: "Estufa não encontrada." });
    }

    return reply.status(204).send();
  });

  // 5. EXCLUIR ESTUFA
  app.delete("/:id", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id } = paramsSchema.parse(request.params);

    const deletedRows = await knex("wallets")
      .where({ id, user_id: userId })
      .delete();

    if (deletedRows === 0) {
      return reply.status(404).send({ error: "Estufa não encontrada." });
    }

    return reply.status(204).send();
  });

  // 6. APORTAR (NUTRIR) OU RESGATAR (COLHER)
  app.post("/:id/transfer", async (request, reply) => {
    const { sub: userId } = request.user;
    const paramsSchema = z.object({ id: z.string().uuid() });
    const { id: walletId } = paramsSchema.parse(request.params);

    const transferSchema = z.object({
      account_id: z.string().uuid(),
      amount: z.number().positive(),
      type: z.enum(["deposit", "withdraw"]),
    });

    const { account_id, amount, type } = transferSchema.parse(request.body);

    // Utilizando Transação (ACID) para garantir integridade financeira absoluta
    await knex.transaction(async (trx) => {
      const wallet = await trx("wallets")
        .where({ id: walletId, user_id: userId })
        .first();
      const account = await trx("accounts")
        .where({ id: account_id, user_id: userId })
        .first();

      if (!wallet || !account) {
        throw new Error("Estufa ou conta bancária não encontrada.");
      }

      const currentWalletAmount = Number(wallet.current_amount);
      const targetAmount = Number(wallet.target_amount);

      if (type === "deposit") {
        // Nutrir: Sai da conta, entra na caixinha
        const newWalletAmount = currentWalletAmount + amount;
        const newStatus =
          newWalletAmount >= targetAmount ? "completed" : wallet.status;

        await trx("wallets").where({ id: walletId }).update({
          current_amount: newWalletAmount,
          status: newStatus,
          updated_at: knex.fn.now(),
        });

        // Deduz do saldo da conta
        await trx("accounts")
          .where({ id: account_id })
          .decrement("balance", amount);

        // Registra a movimentação no histórico geral
        await trx("transactions").insert({
          id: randomUUID(),
          user_id: userId,
          account_id: account_id,
          category_id: null, // Transferência interna
          title: `Aporte em Estufa: ${wallet.title}`,
          amount: amount,
          type: "expense", // Saída da liquidez imediata
          date: new Date(),
        });
      } else {
        // Resgatar: Sai da caixinha, volta para a conta
        if (currentWalletAmount < amount) {
          throw new Error(
            "O valor de resgate excede o saldo disponível na estufa.",
          );
        }

        const newWalletAmount = currentWalletAmount - amount;
        const newStatus =
          newWalletAmount < targetAmount && wallet.status === "completed"
            ? "active"
            : wallet.status;

        await trx("wallets").where({ id: walletId }).update({
          current_amount: newWalletAmount,
          status: newStatus,
          updated_at: knex.fn.now(),
        });

        // Credita no saldo da conta
        await trx("accounts")
          .where({ id: account_id })
          .increment("balance", amount);

        // Registra no histórico geral
        await trx("transactions").insert({
          id: randomUUID(),
          user_id: userId,
          account_id: account_id,
          category_id: null,
          title: `Resgate de Estufa: ${wallet.title}`,
          amount: amount,
          type: "income", // Retorno para liquidez imediata
          date: new Date(),
        });
      }
    });

    return reply.status(200).send();
  });
}
