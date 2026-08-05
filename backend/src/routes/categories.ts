import { z } from "zod";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";

export async function categoriesRoutes(app: FastifyInstance) {
  app.addHook("preHandler", checkAuth);

  app.post("/", async (request, reply) => {
    const createCategorySchema = z.object({
      name: z.string(),
      type: z.enum(["income", "expense"]),
      color: z.string().optional().nullable(),
      icon: z.string().optional().nullable(),
    });

    const { name, type, color, icon } = createCategorySchema.parse(
      request.body,
    );
    const userId = (request.user as any).sub;

    // Fallback para `null` previne o "Undefined binding(s)" do Knex
    await db("categories").insert({
      id: randomUUID(),
      user_id: userId,
      name,
      type,
      color: color || null,
      icon: icon || null,
    });

    return reply.status(201).send();
  });

  app.get("/", async (request) => {
    const userId = (request.user as any).sub;

    const categories = await db("categories")
      .where({ user_id: userId })
      .select();

    return { categories };
  });
}
