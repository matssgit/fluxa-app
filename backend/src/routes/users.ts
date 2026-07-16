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
    try {
      const updateProfileSchema = z.object({
        name: z.string().min(2),
        avatar_url: z
          .string()
          .refine((val) => !val.startsWith("data:image"), {
            message:
              "Imagens Base64 não são permitidas no banco. Utilize um serviço de Storage.",
          })
          .nullable()
          .optional(),
      });

      const { name, avatar_url } = updateProfileSchema.parse(request.body);

      const userId = request.user.sub;

      await db("users").where("id", userId).update({
        name,
        avatar_url,
      });

      return reply
        .status(200)
        .send({ message: "Perfil atualizado com sucesso" });
    } catch (error: any) {
      console.error("🔥 ERRO FATAL NO PUT /profile:", error);
      return reply.status(500).send({
        error: "Internal Server Error",
        details: error.message,
      });
    }
  });

  // ==========================================
  // 2. ATUALIZAR PREFERÊNCIAS (Deep Merge Seguro)
  // ==========================================
  app.put(
    "/preferences",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      try {
        const updatePreferencesSchema = z.object({
          theme: z.enum(["light", "dark", "system"]).optional(),
          privacy: z.object({ hide_balance: z.boolean() }).optional(),
          notifications: z
            .object({
              reminders_enabled: z.boolean(),
              subscriptions_enabled: z.boolean(),
            })
            .optional(),
        });

        const incomingPrefs = updatePreferencesSchema.parse(request.body);
        const user = await db("users").where("id", request.user.sub).first();
        if (!user)
          return reply.status(404).send({ error: "Usuário não encontrado" });

        let currentPrefs: any = {};
        try {
          currentPrefs =
            typeof user.preferences === "string"
              ? JSON.parse(user.preferences)
              : user.preferences || {};
        } catch (e) {
          /* fallback vazio */
        }

        const mergedPreferences = {
          ...currentPrefs,
          theme: incomingPrefs.theme ?? currentPrefs.theme ?? "system",
          privacy: {
            ...(currentPrefs.privacy || {}),
            ...(incomingPrefs.privacy || {}),
          },
          notifications: {
            ...(currentPrefs.notifications || {}),
            ...(incomingPrefs.notifications || {}),
          },
        };

        // 4. Salva no banco (Sem o JSON.stringify, deixamos o Knex/Driver lidar com o JSON nativamente)
        await db("users")
          .where("id", request.user.sub)
          .update({ preferences: mergedPreferences });

        return reply.status(200).send({
          message: "Preferências atualizadas",
          preferences: mergedPreferences,
        });
      } catch (error: any) {
        console.error("🔥 ERRO FATAL NO PUT /preferences:", error);
        return reply.status(500).send({
          error: "Internal Server Error",
          details: error.message,
        });
      }
    },
  );

  // ==========================================
  // 3. EXPORTAÇÃO DE DADOS (CSV de Transações)
  // ==========================================
  app.get("/export", { preHandler: [checkAuth] }, async (request, reply) => {
    try {
      const userId = request.user.sub;

      // Busca todas as transações do utilizador
      const transactions = await db("transactions")
        .where("user_id", userId)
        .orderBy("created_at", "desc");

      // Constrói o cabeçalho do ficheiro CSV
      let csvContent = "ID,Titulo,Valor,Tipo,Status,Data Criacao\n";

      // Mapeia os dados e constrói as linhas separadas por vírgula
      transactions.forEach((t) => {
        // Escapa aspas no título para evitar quebras no Excel
        const safeTitle = `"${(t.title || "").replace(/"/g, '""')}"`;
        const amount = Number(t.amount || 0).toFixed(2);

        csvContent += `${t.id},${safeTitle},${amount},${t.type},${t.status},${t.created_at}\n`;
      });

      // Configura os headers para forçar o browser a fazer o download do ficheiro
      reply.header("Content-Type", "text/csv; charset=utf-8");
      reply.header(
        "Content-Disposition",
        `attachment; filename="fluxa-transacoes-${new Date().toISOString().split("T")[0]}.csv"`,
      );

      return reply.send(csvContent);
    } catch (error: any) {
      console.error("🔥 ERRO FATAL NA EXPORTAÇÃO:", error);
      return reply.status(500).send({
        error: "Internal Server Error",
        details: error.message,
      });
    }
  });

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
