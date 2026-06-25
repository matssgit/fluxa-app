import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import { checkAuth } from "../middlewares/check-auto.js";

export async function creditRoutes(app: FastifyInstance) {
   // Protege todas as rotas deste domínio
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
      });

      const body = createCardSchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      if (!userId) {
         return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      // ATENÇÃO PARA ESTA PARTE: Adicionando o returning e salvando em uma variável
      const [novoCartao] = await db("cards")
         .insert({
            id: randomUUID(),
            user_id: userId,
            name: body.name,
            brand: body.brand,
            limit_amount: body.limit_amount,
            due_day: body.due_day,
         })
         .returning("*"); // O Knex manda o Postgres devolver a linha criada

      // Agora, em vez de enviar () vazio, enviamos o objeto que o banco devolveu!
      return reply.status(201).send(novoCartao);
   });

   // ==========================================
   // 2. LISTAR CARTÕES
   // ==========================================
   app.get("/cards", async (request, reply) => {
      const userId = (request.user as any)?.sub;

      if (!userId) {
         return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      const cards = await db("cards")
         .where("user_id", userId)
         .orderBy("created_at", "desc");
      return { cards };
   });

   // ==========================================
   // 3. CRIAR COMPRA E GERAR PARCELAS (O CÉREBRO)
   // ==========================================
   app.post("/purchases", async (request, reply) => {
      const createPurchaseSchema = z.object({
         card_id: z.string().uuid(),
         category_id: z.string().uuid(),
         title: z.string().min(2),
         store: z.string().min(2),
         observation: z.string().optional(),
         total_amount: z.number().positive(),
         total_installments: z.number().int().positive(),
         purchase_date: z
            .string()
            .regex(/^\d{4}-\d{2}-\d{2}$/, "Use o formato YYYY-MM-DD"),
      });

      const body = createPurchaseSchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      if (!userId) {
         return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      // 1. Pega o cartão para descobrir o due_day (Dia de Vencimento)
      const card = await db("cards")
         .where({ id: body.card_id, user_id: userId })
         .first();
      if (!card) {
         return reply.status(404).send({ message: "Cartão não encontrado." });
      }

      // 2. Abre a Transação ACID (Se falhar no meio, desfaz tudo)
      await db.transaction(async (trx) => {
         const purchaseId = randomUUID();

         // Salva a compra macro
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
            purchase_date: body.purchase_date,
         });

         // 3. Matemática Financeira (Evitando dízimas infinitas e erros de centavos)
         // Ex: 100 / 3 = 33.33333... -> Transforma em 33.33 fixos.
         const baseInstallmentAmount =
            Math.floor((body.total_amount / body.total_installments) * 100) /
            100;

         // Calcula o que sobrou de centavos. Ex: 100 - (33.33 * 3) = 0.01
         const remainder =
            Math.round(
               (body.total_amount -
                  baseInstallmentAmount * body.total_installments) *
                  100,
            ) / 100;

         // Array que vai guardar todas as parcelas para o Batch Insert
         const installmentsToInsert = [];

         // 4. O Loop de Geração das Parcelas
         for (let i = 1; i <= body.total_installments; i++) {
            // Se for a última parcela, adiciona os centavos de sobra para a conta fechar perfeita
            let currentAmount = baseInstallmentAmount;
            if (i === body.total_installments) {
               currentAmount = Number(
                  (baseInstallmentAmount + remainder).toFixed(2),
               );
            }

            // Lógica de Data: Adiciona 'i' meses à data da compra e trava no due_day
            const dateObj = new Date(body.purchase_date);
            dateObj.setUTCMonth(dateObj.getUTCMonth() + i); // Joga pro próximo mês (Ignorando Fuso)
            dateObj.setUTCDate(card.due_day); // Crava o dia de vencimento (Ignorando Fuso)

            const expectedDateStr = dateObj.toISOString().split("T")[0];

            installmentsToInsert.push({
               id: randomUUID(),
               user_id: userId,
               purchase_id: purchaseId,
               installment_number: i,
               total_installments: body.total_installments,
               amount: currentAmount,
               expected_date: expectedDateStr,
               status: "pending",
            });
         }

         // 5. Salva todas as parcelas de uma vezada só (Performance)
         await trx("installments").insert(installmentsToInsert);
      });

      return reply.status(201).send();
   });

   // ==========================================
   // 4. LISTAR PARCELAS (Filtro por status opcional)
   // ==========================================
   app.get("/installments", async (request, reply) => {
      const getInstallmentsSchema = z.object({
         status: z.enum(["pending", "paid"]).optional(),
      });

      const { status } = getInstallmentsSchema.parse(request.query);
      const userId = (request.user as any)?.sub;

      if (!userId) {
         return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      const query = db("installments").where("user_id", userId);

      if (status) {
         query.where({ status });
      }

      const installments = await query.orderBy("expected_date", "asc");
      return { installments };
   });

   // ==========================================
   // 5. PAGAR PARCELA (Gera transação real)
   // ==========================================
   app.post("/installments/:id/pay", async (request, reply) => {
      const paramsSchema = z.object({ id: z.string().uuid() });
      const bodySchema = z.object({ account_id: z.string().uuid() });

      const { id } = paramsSchema.parse(request.params);
      const { account_id } = bodySchema.parse(request.body);
      const userId = (request.user as any)?.sub;

      if (!userId) {
         return reply.status(401).send({ message: "Usuário não autenticado." });
      }

      await db.transaction(async (trx) => {
         // 1. Verifica se a parcela existe e pertence ao usuário
         const installment = await trx("installments")
            .where({ id, user_id: userId })
            .first();

         if (!installment) {
            return reply
               .status(404)
               .send({ message: "Parcela não encontrada." });
         }

         if (installment.status === "paid") {
            return reply
               .status(400)
               .send({ message: "Esta parcela já foi paga." });
         }

         // 2. Busca detalhes da compra original para herdar a categoria e o título
         const purchase = await trx("credit_purchases")
            .where({ id: installment.purchase_id })
            .first();

         if (!purchase) {
            throw new Error(
               "Erro de integridade: Compra original não encontrada.",
            );
         }

         const today = new Date().toISOString().split("T")[0];

         // 3. Altera o status da parcela para 'paid'
         await trx("installments").where({ id }).update({
            status: "paid",
            completed_date: today,
         });

         // 4. Gera o evento financeiro (A saída de dinheiro real da conta)
         await trx("transactions").insert({
            id: randomUUID(),
            user_id: userId,
            account_id: account_id,
            category_id: purchase.category_id,
            // Exemplo de título gerado: "Pgto Parcela: Amazon (1/3)"
            title: `Pgto Parcela: ${purchase.title} (${installment.installment_number}/${installment.total_installments})`,
            amount: -Math.abs(installment.amount), // Força a ser negativo para o cálculo dinâmico da summary
            // status: "completed", // Descomente se sua tabela transactions tiver uma coluna de status
            // completed_date: today, // Descomente se tiver coluna completed_date específica em transactions
         });
      });

      return reply.status(204).send();
   });
}
