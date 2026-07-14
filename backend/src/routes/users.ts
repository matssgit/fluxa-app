import { checkAuth } from "../middlewares/check-auth.js";
import type { FastifyInstance } from "fastify";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";
import bcrypt from "bcrypt";
import { z } from "zod";

export async function usersRoutes(app: FastifyInstance) {
  // ==========================================
  // 1. ATUALIZAR PERFIL (Identidade)
  // ==========================================
  app.put("/profile", { preHandler: [checkAuth] }, async (request, reply) => {
    // Zod blindando a entrada: O email sequer existe no schema.
    // Qualquer tentativa de enviar "email" no payload será ignorada.
    const updateProfileSchema = z.object({
      name: z.string().min(2, "O nome deve ter pelo menos 2 caracteres"),
      avatar_url: z.string().url().nullable().optional(),
    });

    const { name, avatar_url } = updateProfileSchema.parse(request.body);

    // O ID vem de forma segura do token JWT decodificado pelo checkAuth
    const userId = request.user.sub;

    await db("users").where("id", userId).update({
      name,
      avatar_url,
      updated_at: db.fn.now(), // Caso tenha essa coluna
    });

    return reply.status(200).send({ message: "Perfil atualizado com sucesso" });
  });

  // ==========================================
  // 2. ATUALIZAR PREFERÊNCIAS (JSON Merge)
  // ==========================================
  app.put(
    "/preferences",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      try {
        const updatePreferencesSchema = z.object({
          theme: z.enum(["light", "dark", "system"]).optional(),
          privacy: z
            .object({
              hide_balance: z.boolean(),
            })
            .optional(),
          notifications: z
            .object({
              reminders_enabled: z.boolean(),
              subscriptions_enabled: z.boolean(),
            })
            .optional(),
        });

        const newPreferences = updatePreferencesSchema.parse(request.body);
        const userId = request.user.sub;

        // 1. Busca o usuário atual
        const user = await db("users").where("id", userId).first();

        if (!user) {
          return reply.status(404).send({ error: "Usuário não encontrado" });
        }

        // 2. Parse ultra-seguro (Evita crashes se a coluna vier null ou mal formatada)
        let currentPreferences: any = {};
        try {
          currentPreferences =
            typeof user.preferences === "string"
              ? JSON.parse(user.preferences)
              : user.preferences || {};
        } catch (parseError) {
          console.warn(
            "Aviso: Falha ao fazer parse das preferências antigas. Assumindo objeto vazio.",
          );
        }

        // 3. Deep Merge blindado contra "undefined"
        const mergedPreferences = {
          ...currentPreferences,
          theme: newPreferences.theme ?? currentPreferences.theme ?? "system",
          privacy: {
            ...(currentPreferences.privacy || {}),
            ...(newPreferences.privacy || {}),
          },
          notifications: {
            ...(currentPreferences.notifications || {}),
            ...(newPreferences.notifications || {}),
          },
        };

        // 4. Salva no banco (Sem o JSON.stringify, deixamos o Knex/Driver lidar com o JSON nativamente)
        await db("users").where("id", userId).update({
          // Se você usa SQLite e mesmo assim der erro, mude para: JSON.stringify(mergedPreferences)
          preferences: mergedPreferences,
        });

        return reply.status(200).send({
          message: "Preferências atualizadas",
          preferences: mergedPreferences,
        });
      } catch (error: any) {
        // 🔥 LOG FATAL: Vai mostrar exatamente onde o banco reclamou no terminal do Backend!
        console.error("🔥 ERRO FATAL NO PUT /preferences:", error);
        return reply.status(500).send({
          error: "Internal Server Error",
          details: error.message,
        });
      }
    },
  );

  // ****** CADASTRO ******
  app.post("/register", async (request, reply) => {
    const registerSchema = z.object({
      name: z.string(),
      email: z.string().email(),
      password: z.string().min(6),
    });

    const { name, email, password } = registerSchema.parse(request.body);

    const userExists = await db("users").where({ email }).first();
    if (userExists)
      return reply.status(400).send({ error: "User already exists" });

    const password_hash = await bcrypt.hash(password, 8);

    await db("users").insert({
      id: randomUUID(),
      name,
      email,
      password_hash,
    });

    return reply.status(201).send();
  });

  // ****** LOGIN ******
  app.post("/login", async (request, reply) => {
    const loginSchema = z.object({
      email: z.string().email(),
      password: z.string(),
    });

    const { email, password } = loginSchema.parse(request.body);

    const user = await db("users").where({ email }).first();
    if (!user) return reply.status(400).send({ error: "Invalid credentials" });

    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid)
      return reply.status(400).send({ error: "Invalid credentials" });

    const token = app.jwt.sign({ sub: user.id }, { expiresIn: "7d" });

    const userPreferences =
      typeof user.preferences === "string"
        ? JSON.parse(user.preferences)
        : user.preferences || {};

    return reply.status(200).send({
      token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        avatar_url: user.avatar_url,
        preferences: userPreferences,
      },
    });
  });
}
