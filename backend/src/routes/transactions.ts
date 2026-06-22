import type { FastifyInstance } from "fastify";
import { db } from "../database.js";
import { randomUUID } from "node:crypto";
import { z } from "zod";
import { checkSessionIdExists } from "../middlewares/check-session-id-exists.js";

// Cookies <--> Formas da gente manter contexto entre requisições

export async function transactionsRoutes(app: FastifyInstance) {
   /**
    * NOTA ARQUITETURAL:
    * Optei por não implementar rotas de edição (PUT) ou exclusão (DELETE).
    * Em sistemas financeiros e ledgers de transações, a regra de ouro é a imutabilidade.
    * Se uma transação foi registrada errada, a correção deve ser feita através de um
    * estorno (uma nova transação inversa), garantindo a auditoria e o histórico da conta.
    */

   // ****** GET: Listar todas as transações  ******
   app.get(
      "/",
      {
         preHandler: [checkSessionIdExists],
      },
      async (request, reply) => {
         // Recupero o cookie do usuário para garantir que ele só veja os próprios dados
         const { sessionId } = request.cookies;

         const transactions = await db("transactions")
            .where("session_id", sessionId)
            .select();

         return { transactions };
      },
   );

   // ****** GET: Buscar uma transação específica  ******
   app.get(
      "/:id",
      {
         preHandler: [checkSessionIdExists],
      },
      async (request, reply) => {
         const getTransactionsParamsSchema = z.object({
            id: z.string().uuid(),
         });

         try {
            // Valido se o ID passado na rota é realmente um UUID válido
            const { id } = getTransactionsParamsSchema.parse(request.params);
            const { sessionId } = request.cookies;

            const transaction = await db("transactions")
               .where({
                  session_id: sessionId,
                  id,
               })
               .first();

            if (!transaction) {
               return reply
                  .status(404)
                  .send({ error: "Transaction not found." });
            }

            return { transaction };
         } catch (error) {
            return reply
               .status(400)
               .send({ error: "Invalid transaction ID format." });
         }
      },
   );

   // ****** GET: Resumo (Saldo) da conta  ******
   app.get(
      "/summary",
      {
         preHandler: [checkSessionIdExists],
      },
      async (request) => {
         const { sessionId } = request.cookies;

         const summary = await db("transactions")
            .where("session_id", sessionId)
            .sum("amount", {
               as: "amount",
            })
            .first();

         return { summary };
      },
   );

   // ****** POST: Criar nova transação  ******
   app.post("/", async (request, reply) => {
      const createTransactionBodySchema = z.object({
         title: z.string(),
         amount: z.number(),
         type: z.enum(["credit", "debit"]),
      });

      try {
         // Valido o corpo da requisição. Se falhar, cai no catch retornando 400
         const { title, amount, type } = createTransactionBodySchema.parse(
            request.body,
         );

         // Verifico se o usuário já tem um cookie de sessão
         let sessionId = request.cookies.sessionId;

         // Se não tiver, crio um novo cookie que vai durar 7 dias
         if (!sessionId) {
            sessionId = randomUUID();

            reply.cookie("sessionId", sessionId, {
               path: "/",
               maxAge: 1000 * 60 * 60 * 24 * 7, // 7 dias em milissegundos
               sameSite: "none", // Permite envio entre domínios diferentes (Localhost <-> Render)
               secure: true, // Obrigatório para sameSite: "none" (Funciona em HTTPS)
            });
         }

         // Insiro a transação. Se for débito, salvo como valor negativo para facilitar a soma no summary
         await db("transactions").insert({
            id: randomUUID(),
            title,
            amount: type === "credit" ? amount : amount * -1,
            session_id: sessionId,
         });

         return reply.status(201).send();
      } catch (error) {
         // Tratamento amigável para o front-end consumir
         return reply.status(400).send({ error: "Invalid request body." });
      }
   });
}
