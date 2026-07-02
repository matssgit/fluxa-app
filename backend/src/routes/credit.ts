import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

export async function creditRoutes(app: FastifyInstance) {
   // 🔒 Middleware de Autenticação
   // Nenhuma rota de crédito confia em user_id enviado pelo body.
   // A identidade é sempre extraída do token JWT validado.
   app.addHook("preHandler", checkAuth);

   // ==========================================
   // 1. CRIAR CARTÃO DE CRÉDITO
   // ==========================================
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

         if (!userId)
            return reply
               .status(401)
               .send({ message: "Usuário não autenticado." });

         // O cartão nasce com o limite disponível idêntico ao limite total.
         // As duas colunas são separadas para mantermos o histórico do limite concedido.
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
         console.error("🔥 Erro ao cadastrar cartão:", error);
         return reply.status(500).send({ message: "Erro interno", error });
      }
   });

   // ==========================================
   // 2. LISTAR CARTÕES
   // ==========================================
   app.get("/cards", async (request, reply) => {
      const userId = (request.user as any)?.sub;
      if (!userId)
         return reply.status(401).send({ message: "Usuário não autenticado." });

      const cards = await db("cards")
         .select("*")
         .where("user_id", userId)
         .orderBy("created_at", "desc");

      return { cards };
   });

   // ==========================================
   // 2.5 EDITAR CARTÃO
   // ==========================================
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

         // Busca o cartão garantindo o isolamento do tenant (user_id)
         const card = await db("cards").where({ id, user_id: userId }).first();

         if (!card) {
            return reply
               .status(404)
               .send({ message: "Cartão não encontrado." });
         }

         // Lógica core da arquitetura: Preservação do limite consumido!
         // Se o usuário já gastou X, o novo limite total não pode ser menor que X.
         const consumedLimit =
            Number(card.total_limit) - Number(card.available_limit);
         const newAvailableLimit = body.total_limit - consumedLimit;

         if (newAvailableLimit < 0) {
            return reply.status(400).send({
               message:
                  "O novo limite não pode ser menor que o valor já consumido nas faturas.",
            });
         }

         await db("cards").where({ id }).update({
            name: body.name,
            brand: body.brand,
            due_day: body.due_day,
            total_limit: body.total_limit,
            available_limit: newAvailableLimit,
            color: body.color,
         });

         return reply.status(204).send();
      } catch (error) {
         console.error("🚨 Erro ao editar cartão:", error);
         return reply
            .status(500)
            .send({ message: "Erro ao editar cartão", error });
      }
   });

   // ==========================================
   // 3. CRIAR COMPRA E GERAR PARCELAS
   // ==========================================
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

         if (!userId)
            return reply.status(401).send({ message: "Não autenticado." });

         const card = await db("cards")
            .where({ id: body.card_id, user_id: userId })
            .first();

         if (!card)
            return reply
               .status(404)
               .send({ message: "Cartão não encontrado." });

         // Transação ACID: Garante que a compra, o abatimento do limite e a
         // geração das parcelas aconteçam juntos. Se uma falhar, o banco faz rollback.
         await db.transaction(async (trx) => {
            const purchaseId = randomUUID();

            // Passo A: Insere a compra com o valor total
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

            // Passo B: Subtrai o valor da compra EXCLUSIVAMENTE do limite disponível do cartão
            await trx("cards")
               .where({ id: body.card_id })
               .decrement("available_limit", body.total_amount);

            // Passo C: Motor matemático de parcelamento
            // Isola os centavos e dízimas para jogar o resto na última parcela
            const baseInstallmentAmount =
               Math.floor((body.total_amount / body.total_installments) * 100) /
               100;
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

               // Projeta os meses mantendo o dia de vencimento fixo do cartão
               const dateObj = new Date(body.purchase_date);
               dateObj.setUTCMonth(dateObj.getUTCMonth() + i);
               dateObj.setUTCDate(card.due_day);

               installmentsToInsert.push({
                  id: randomUUID(),
                  user_id: userId,
                  purchase_id: purchaseId,
                  installment_number: i,
                  total_installments: body.total_installments,
                  amount: currentAmount,
                  expected_date: dateObj.toISOString().split("T")[0],
                  status: "pending",
               });
            }

            // Passo D: Bulk insert para performance
            await trx("installments").insert(installmentsToInsert);
         });

         return reply.status(201).send();
      } catch (error) {
         console.error("🚨 ERRO AO LANÇAR COMPRA:", error);
         return reply
            .status(500)
            .send({ message: "Erro ao processar compra", error });
      }
   });

   // ==========================================
   // 3.5 LISTAR COMPRAS
   // ==========================================
   app.get("/purchases", async (request, reply) => {
      const userId = (request.user as any)?.sub;
      if (!userId)
         return reply.status(401).send({ message: "Não autenticado." });

      const purchases = await db("credit_purchases")
         .select("*")
         .where("user_id", userId)
         .orderBy("purchase_date", "desc");

      return { purchases };
   });

   // ==========================================
   // 4. LISTAR PARCELAS
   // ==========================================
   app.get("/installments", async (request, reply) => {
      const userId = (request.user as any)?.sub;
      if (!userId)
         return reply.status(401).send({ message: "Não autenticado." });

      // Join necessário para o frontend saber de qual compra é esta parcela
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

   // ==========================================
   // 5. PAGAR PARCELA (Ponte entre Crédito e Caixa)
   // ==========================================
   app.post("/installments/:id/pay", async (request, reply) => {
      const paramsSchema = z.object({ id: z.string().uuid() });
      const bodySchema = z.object({ account_id: z.string().uuid() });

      const { id } = paramsSchema.parse(request.params);
      const { account_id } = bodySchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      await db.transaction(async (trx) => {
         const installment = await trx("installments")
            .join(
               "credit_purchases",
               "installments.purchase_id",
               "credit_purchases.id",
            )
            .where("installments.id", id)
            .where("credit_purchases.user_id", userId)
            .select(
               "installments.*",
               "credit_purchases.category_id",
               "credit_purchases.title",
               "credit_purchases.card_id",
            )
            .first();

         if (!installment)
            return reply
               .status(404)
               .send({ message: "Parcela não encontrada." });
         if (installment.status === "paid")
            return reply.status(400).send({ message: "Já paga." });

         const today = new Date().toISOString().split("T")[0];

         // 1. Marca a parcela como paga
         await trx("installments").where({ id }).update({
            status: "paid",
            completed_date: today,
         });

         // 2. Regra Imutável: O pagamento afeta o saldo (transactions) da conta selecionada
         if (account_id) {
            await trx("transactions").insert({
               id: randomUUID(),
               user_id: userId,
               account_id: account_id,
               category_id: installment.category_id,
               title: `Fatura: ${installment.title} (${installment.installment_number}/${installment.total_installments})`,
               amount: -Math.abs(installment.amount),
               status: "completed",
            });
         }

         // 3. O limite da parcela paga é liberado de volta para o cartão
         await trx("cards")
            .where({ id: installment.card_id })
            .increment("available_limit", Number(installment.amount));
      });

      return reply.status(204).send();
   });

   // ==========================================
   // 6. EXCLUIR CARTÃO
   // ==========================================
   app.delete("/cards/:id", async (request, reply) => {
      const { id } = z.object({ id: z.string().uuid() }).parse(request.params);
      const userId = (request.user as any)?.sub;

      // Impede a exclusão se o cartão tiver obrigações pendentes.
      // Se tiver apenas compras canceladas ou pagas, o delete é permitido.
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

      await db("cards").where({ id, user_id: userId }).delete();
      return reply.status(204).send();
   });

   // ==========================================
   // 7. CANCELAMENTO LÓGICO DE COMPRA (Soft Delete)
   // ==========================================
   app.patch("/purchases/:id/cancel", async (request, reply) => {
      const paramsSchema = z.object({ id: z.string().uuid() });

      try {
         const { id } = paramsSchema.parse(request.params);
         const userId = (request.user as any)?.sub;

         // A regra de ouro: O sistema não reescreve o passado.
         await db.transaction(async (trx) => {
            const purchase = await trx("credit_purchases")
               .where({ id, user_id: userId })
               .first();

            if (!purchase) {
               return reply
                  .status(404)
                  .send({ message: "Compra não encontrada." });
            }

            if (purchase.status === "cancelled") {
               return reply
                  .status(400)
                  .send({ message: "Esta compra já está cancelada." });
            }

            const installments = await trx("installments").where({
               purchase_id: id,
            });

            // Identifica as parcelas que ainda não foram pagas
            const pendingInstallments = installments.filter(
               (inst) => inst.status === "pending",
            );

            // Só restauramos o limite daquilo que ainda estava bloqueado (pendente)
            const amountToRestore = pendingInstallments.reduce(
               (acc, inst) => acc + Number(inst.amount),
               0,
            );

            await trx("cards")
               .where({ id: purchase.card_id })
               .increment("available_limit", amountToRestore);

            await trx("credit_purchases")
               .where({ id })
               .update({ status: "cancelled" });

            // Cancela apenas as parcelas em aberto, preservando o histórico das já pagas.
            await trx("installments")
               .where({ purchase_id: id, status: "pending" })
               .update({ status: "cancelled" });
         });

         return reply.status(204).send();
      } catch (error) {
         console.error("🚨 Erro ao cancelar compra:", error);
         return reply
            .status(500)
            .send({ message: "Erro interno ao processar cancelamento." });
      }
   });
}
