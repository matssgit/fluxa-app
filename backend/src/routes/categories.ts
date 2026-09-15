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

  app.put("/:id", async (request, reply) => {
    const { id } = z.object({ id: z.string().uuid() }).parse(request.params);
    const body = z
      .object({
        name: z.string().trim().min(2).optional(),
        type: z.enum(["income", "expense"]).optional(),
        color: z.string().nullable().optional(),
        icon: z.string().nullable().optional(),
      })
      .refine((value) => Object.keys(value).length > 0, {
        message: "Informe ao menos um campo para atualizar.",
      })
      .parse(request.body);
    const userId = request.user.sub;

    const updated = await db("categories")
      .where({ id, user_id: userId })
      .update(body);

    if (updated === 0) {
      return reply.status(404).send({ message: "Categoria não encontrada." });
    }

    return reply.status(204).send();
  });

  app.delete("/:id", async (request, reply) => {
    const { id } = z.object({ id: z.string().uuid() }).parse(request.params);
    const userId = request.user.sub;

    try {
      const deleted = await db("categories")
        .where({ id, user_id: userId })
        .delete();

      if (deleted === 0) {
        return reply.status(404).send({ message: "Categoria não encontrada." });
      }

      return reply.status(204).send();
    } catch (error: unknown) {
      if (
        typeof error === "object" &&
        error !== null &&
        "code" in error &&
        error.code === "23503"
      ) {
        return reply.status(409).send({
          message: "Categoria em uso e não pode ser excluída.",
        });
      }
      throw error;
    }
  });
}
