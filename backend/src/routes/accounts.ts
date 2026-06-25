import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import { checkAuth } from "../middlewares/check-auto.js";

export async function accountsRoutes(app: FastifyInstance) {
   // Todas as rotas de contas exigem usuário logado
   app.addHook("preHandler", checkAuth);

   // ****** CRIAR CONTA ******
   app.post("/", async (request, reply) => {
      const createAccountSchema = z.object({
         name: z.string(),
         type: z.string(),
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

   // ****** LISTAR CONTAS ******
   app.get("/", async (request) => {
      const userId = (request.user as any).sub;

      const accounts = await db("accounts").where({ user_id: userId }).select();

      return { accounts };
   });
}
