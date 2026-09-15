import { z } from "zod";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import {
  InstallmentPaymentError,
  payInstallment,
} from "../services/installment-payments.service.js";
import { getInstallmentDueDate } from "../utils/installment-dates.js";

export async function creditRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  app.post("/cards", async (request, reply) => {
    const createCardSchema = z.object({
      name: z.string().min(2),
      brand: z.string().min(2),
      limit_amount: z.number().positive(),
      due_day: z.number().int().min(1).max(31),
      color: z.string().optional().default("slate"),
    });

    try {
      const body = createCardSchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      if (!userId) {
        return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      const [novoCartao] = await db("cards")
        .insert({
          id: randomUUID(),
          user_id: userId,
          name: body.name,
          brand: body.brand,
          total_limit: body.limit_amount,
          available_limit: body.limit_amount,
          due_day: body.due_day,
          color: body.color,
        })
        .returning("*");

      return reply.status(201).send(novoCartao);
    } catch (error) {
      if (error instanceof z.ZodError) throw error;
      console.error("Erro ao cadastrar cartão:", error);
      return reply
        .status(500)
        .send({ message: "Erro interno ao cadastrar cartão." });
    }
  });

  app.get("/cards", async (request, reply) => {
    const userId = (request.user as any)?.sub;

    if (!userId) {
      return reply.status(401).send({ message: "Usuário não autenticado." });
    }

    const cards = await db("cards")
      .select("*")
      .where("user_id", userId)
      .orderBy("created_at", "desc");

    return { cards };
  });

  app.put("/cards/:id", async (request, reply) => {
    const paramsSchema = z.object({ id: z.string().uuid() });
    const updateCardSchema = z.object({
      name: z.string().min(2),
      brand: z.string().min(2),
      total_limit: z.number().positive(),
      due_day: z.number().int().min(1).max(31),
      color: z.string().optional().default("slate"),
    });

    try {
      const { id } = paramsSchema.parse(request.params);
      const body = updateCardSchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      const result = await db.transaction(async (trx) => {
        const card = await trx("cards")
          .where({ id, user_id: userId })
          .forUpdate()
          .first();

        if (!card) return "not_found" as const;

        const consumedLimit =
          Number(card.total_limit) - Number(card.available_limit);
        const newAvailableLimit = body.total_limit - consumedLimit;

        if (newAvailableLimit < 0) return "below_consumed" as const;

        await trx("cards").where({ id, user_id: userId }).update({
          name: body.name,
          brand: body.brand,
          due_day: body.due_day,
          total_limit: body.total_limit,
          available_limit: newAvailableLimit,
          color: body.color,
        });

        return "updated" as const;
      });

      if (result === "not_found") {
        return reply.status(404).send({ message: "Cartão não encontrado." });
      }
      if (result === "below_consumed") {
        return reply.status(400).send({
          message:
            "O novo limite não pode ser menor que o valor já consumido nas faturas.",
        });
      }

      return reply.status(204).send();
    } catch (error) {
      if (error instanceof z.ZodError) throw error;
      console.error("Erro ao editar cartão:", error);
      return reply
        .status(500)
        .send({ message: "Erro interno ao editar cartão." });
    }
  });

  app.post("/purchases", async (request, reply) => {
    const createPurchaseSchema = z.object({
      card_id: z.string().uuid(),
      category_id: z.string().uuid(),
      title: z.string().min(2),
      store: z.string().min(2),
      observation: z.string().optional().nullable(),
      total_amount: z.number().positive(),
      total_installments: z.number().int().positive(),
      purchase_date: z.string(),
    });

    try {
      const body = createPurchaseSchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      if (!userId) {
        return reply.status(401).send({ message: "Não autenticado." });
      }

      const category = await db("categories")
        .where({ id: body.category_id, user_id: userId })
        .first();

      if (!category) {
        return reply
          .status(403)
          .send({ message: "Categoria inválida ou não pertence a você." });
      }

      await db.transaction(async (trx) => {
        const card = await trx("cards")
          .where({ id: body.card_id, user_id: userId })
          .forUpdate()
          .first();

        if (!card) throw new Error("CARD_NOT_FOUND");
        if (Number(card.available_limit) < body.total_amount) {
          throw new Error("INSUFFICIENT_LIMIT");
        }

        const limitUpdated = await trx("cards")
          .where({ id: body.card_id, user_id: userId })
          .andWhere("available_limit", ">=", body.total_amount)
          .decrement("available_limit", body.total_amount);

        if (limitUpdated !== 1) throw new Error("INSUFFICIENT_LIMIT");

        const purchaseId = randomUUID();

        await trx("credit_purchases").insert({
          id: purchaseId,
          user_id: userId,
          card_id: body.card_id,
          category_id: body.category_id,
          title: body.title,
          store: body.store,
          observation: body.observation,
          total_amount: body.total_amount,
          total_installments: body.total_installments,
          purchase_date: body.purchase_date.split("T")[0],
        });

        const baseInstallmentAmount =
          Math.floor((body.total_amount / body.total_installments) * 100) / 100;

        // Direciona o resto da divisão para a última parcela para garantir o fechamento exato dos centavos
        const remainder =
          Math.round(
            (body.total_amount -
              baseInstallmentAmount * body.total_installments) *
              100,
          ) / 100;

        const installmentsToInsert = [];

        for (let i = 1; i <= body.total_installments; i++) {
          let currentAmount = baseInstallmentAmount;

          if (i === body.total_installments) {
            currentAmount = Number(
              (baseInstallmentAmount + remainder).toFixed(2),
            );
          }

          installmentsToInsert.push({
            id: randomUUID(),
            user_id: userId,
            purchase_id: purchaseId,
            installment_number: i,
            total_installments: body.total_installments,
            amount: currentAmount,
            expected_date: getInstallmentDueDate(
              body.purchase_date,
              i,
              card.due_day,
            ),
            status: "pending",
          });
        }

        await trx("installments").insert(installmentsToInsert);
      });

      return reply.status(201).send();
    } catch (error: unknown) {
      if (error instanceof Error && error.message === "CARD_NOT_FOUND") {
        return reply.status(404).send({ message: "Cartão não encontrado." });
      }
      if (error instanceof Error && error.message === "INSUFFICIENT_LIMIT") {
        return reply.status(400).send({
          message:
            "Verifique o limite do cartão. Limite insuficiente para esta compra.",
        });
      }
      console.error("Erro ao lançar compra:", error);
      return reply
        .status(500)
        .send({ message: "Erro ao processar compra", error });
    }
  });

  app.get("/purchases", async (request, reply) => {
    const userId = (request.user as any)?.sub;
    if (!userId) return reply.status(401).send({ message: "Não autenticado." });

    const purchases = await db("credit_purchases")
      .select("*")
      .where("user_id", userId)
      .orderBy("purchase_date", "desc");

    return { purchases };
  });

  app.get("/installments", async (request, reply) => {
    const userId = (request.user as any)?.sub;
    if (!userId) return reply.status(401).send({ message: "Não autenticado." });

    const installments = await db("installments")
      .join(
        "credit_purchases",
        "installments.purchase_id",
        "credit_purchases.id",
      )
      .where("credit_purchases.user_id", userId)
      .select("installments.*", "credit_purchases.title as purchase_title")
      .orderBy("installments.expected_date", "asc");

    return { installments };
  });

  app.post("/installments/:id/pay", async (request, reply) => {
    const paramsSchema = z.object({ id: z.string().uuid() });
    const bodySchema = z.object({ account_id: z.string().uuid() });

    try {
      const { id } = paramsSchema.parse(request.params);
      const { account_id } = bodySchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      await payInstallment({
        installmentId: id,
        userId,
        accountId: account_id,
      });

      return reply.status(204).send();
    } catch (error: unknown) {
      if (
        error instanceof InstallmentPaymentError &&
        error.code === "INSTALLMENT_NOT_FOUND"
      ) {
        return reply.status(404).send({ message: "Parcela não encontrada." });
      }
      if (
        error instanceof InstallmentPaymentError &&
        error.code === "PURCHASE_NOT_FOUND"
      ) {
        return reply
          .status(404)
          .send({ message: "Compra vinculada não encontrada." });
      }
      if (
        error instanceof InstallmentPaymentError &&
        error.code === "ACCOUNT_NOT_FOUND"
      ) {
        return reply.status(403).send({
          message: "Operação negada. A conta selecionada não pertence a você.",
        });
      }
      if (
        error instanceof InstallmentPaymentError &&
        ["ALREADY_PAID", "PURCHASE_CANCELLED", "INSTALLMENT_CANCELLED"].includes(
          error.code,
        )
      ) {
        return reply.status(400).send({ message: "Já paga." });
      }

      console.error("Erro no pagamento da fatura:", error);
      return reply.status(500).send({ message: "Erro interno", error });
    }
  });

  app.delete("/cards/:id", async (request, reply) => {
    const { id } = z.object({ id: z.string().uuid() }).parse(request.params);
    const userId = (request.user as any).sub;

    const card = await db("cards").where({ id, user_id: userId }).first();

    if (!card) {
      return reply.status(404).send({ message: "Cartão não encontrado." });
    }

    const pendingObligations = await db("installments")
      .join(
        "credit_purchases",
        "installments.purchase_id",
        "credit_purchases.id",
      )
      .where("credit_purchases.card_id", id)
      .where("installments.status", "pending")
      .first();

    if (pendingObligations) {
      return reply.status(409).send({
        message:
          "Não é possível excluir este cartão. Existem faturas pendentes vinculadas a ele.",
      });
    }

    await db("cards").where({ id }).delete();
    return reply.status(204).send();
  });

  app.patch("/purchases/:id/cancel", async (request, reply) => {
    const paramsSchema = z.object({ id: z.string().uuid() });

    try {
      const { id } = paramsSchema.parse(request.params);
      const userId = (request.user as any)?.sub;

      const result = await db.transaction(async (trx) => {
        const purchase = await trx("credit_purchases")
          .where({ id, user_id: userId })
          .forUpdate()
          .first();

        if (!purchase) return "not_found" as const;
        if (purchase.status === "cancelled") return "already_cancelled" as const;

        const installments = await trx("installments")
          .where({ purchase_id: id })
          .orderBy("id", "asc")
          .forUpdate();

        const pendingInstallments = installments.filter(
          (inst) => inst.status === "pending",
        );

        const amountToRestore = pendingInstallments.reduce(
          (acc, inst) => acc + Number(inst.amount),
          0,
        );

        const card = await trx("cards")
          .where({ id: purchase.card_id, user_id: userId })
          .forUpdate()
          .first();
        if (!card) throw new Error("CARD_NOT_FOUND");

        await trx("cards")
          .where({ id: card.id })
          .update({
            available_limit: trx.raw(
              "LEAST(total_limit, available_limit + ?)",
              [amountToRestore],
            ),
          });

        await trx("credit_purchases")
          .where({ id })
          .update({ status: "cancelled" });

        await trx("installments")
          .where({ purchase_id: id, status: "pending" })
          .update({ status: "cancelled" });

        return "cancelled" as const;
      });

      if (result === "not_found") {
        return reply.status(404).send({ message: "Compra não encontrada." });
      }
      if (result === "already_cancelled") {
        return reply
          .status(400)
          .send({ message: "Esta compra já está cancelada." });
      }

      return reply.status(204).send();
    } catch (error) {
      console.error("Erro ao cancelar compra:", error);
      return reply
        .status(500)
        .send({ message: "Erro interno ao processar cancelamento." });
    }
  });
}
