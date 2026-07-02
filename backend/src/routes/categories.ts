import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import { checkAuth } from "../middlewares/check-auth.js";

export async function categoriesRoutes(app: FastifyInstance) {
   app.addHook("preHandler", checkAuth);

   // ****** CRIAR CATEGORIA ******
   app.post("/", async (request, reply) => {
      const createCategorySchema = z.object({
         name: z.string(),
         type: z.enum(["income", "expense"]),
         color: z.string().optional(),
         icon: z.string().optional(),
      });

      const { name, type, color, icon } = createCategorySchema.parse(
         request.body,
      );
      const userId = (request.user as any).sub;

      await db("categories").insert({
         id: randomUUID(),
         user_id: userId,
         name,
         type,
         color,
         icon,
      });

      return reply.status(201).send();
   });

   // ****** LISTAR CATEGORIAS ******
   app.get("/", async (request) => {
      const userId = (request.user as any).sub;

      const categories = await db("categories")
         .where({ user_id: userId })
         .select();

      return { categories };
   });
}
