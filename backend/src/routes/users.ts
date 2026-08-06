import { z } from "zod";
import bcrypt from "bcrypt";
import { db } from "../database/database.js";
import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import { totpService } from "../services/totp.service.js";
import { emailService } from "../services/email.service.js";
import { randomUUID, randomBytes, createHash } from "node:crypto";

export async function usersRoutes(app: FastifyInstance) {
  app.put("/profile", { preHandler: [checkAuth] }, async (request, reply) => {
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

    // Rota temporária para diagnóstico isolado
    app.get("/test-email", async (request, reply) => {
      console.log("[TEST-EMAIL] Rota de diagnóstico acionada.");
      try {
        await emailService.sendVerificationEmail(
          "aronxmattheus@gmail.com",
          "token_de_teste_123",
        );
        return reply
          .status(200)
          .send({
            message: "Comando de envio executado. Verifique os logs do Render.",
          });
      } catch (error) {
        console.error("[TEST-EMAIL] Falha capturada na rota de teste:", error);
        return reply
          .status(500)
          .send({ error: "Falha no envio", details: String(error) });
      }
    });

    const { name, avatar_url } = updateProfileSchema.parse(request.body);
    const userId = request.user.sub;

    await db("users").where("id", userId).update({
      name,
      avatar_url,
    });

    return reply.status(200).send({ message: "Perfil atualizado com sucesso" });
  });

  app.put(
    "/preferences",
    { preHandler: [checkAuth] },
    async (request, reply) => {
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

      if (!user) {
        return reply.status(404).send({ error: "Usuário não encontrado" });
      }

      let currentPrefs: Record<string, unknown> = {};
      try {
        currentPrefs =
          typeof user.preferences === "string"
            ? JSON.parse(user.preferences)
            : user.preferences || {};
      } catch (e) {}

      const mergedPreferences = {
        ...currentPrefs,
        theme: incomingPrefs.theme ?? currentPrefs.theme ?? "system",
        privacy: {
          ...((currentPrefs.privacy as Record<string, unknown>) || {}),
          ...(incomingPrefs.privacy || {}),
        },
        notifications: {
          ...((currentPrefs.notifications as Record<string, unknown>) || {}),
          ...(incomingPrefs.notifications || {}),
        },
      };

      await db("users")
        .where("id", request.user.sub)
        .update({ preferences: JSON.stringify(mergedPreferences) });

      return reply.status(200).send({
        message: "Preferências atualizadas",
        preferences: mergedPreferences,
      });
    },
  );

  app.get("/export", { preHandler: [checkAuth] }, async (request, reply) => {
    const userId = request.user.sub;

    const transactions = await db("transactions")
      .where("user_id", userId)
      .orderBy("created_at", "desc");

    let csvContent = "ID,Titulo,Valor,Tipo,Status,Data Criacao\n";

    transactions.forEach((t) => {
      const safeTitle = `"${(t.title || "").replace(/"/g, '""')}"`;
      const amount = Number(t.amount || 0).toFixed(2);
      csvContent += `${t.id},${safeTitle},${amount},${t.type},${t.status},${t.created_at}\n`;
    });

    reply.header("Content-Type", "text/csv; charset=utf-8");
    reply.header(
      "Content-Disposition",
      `attachment; filename="fluxa-transacoes-${new Date().toISOString().split("T")[0]}.csv"`,
    );

    return reply.send(csvContent);
  });

  app.post("/register", async (request, reply) => {
    const registerSchema = z.object({
      name: z.string(),
      email: z.string().email(),
      password: z.string().min(6),
    });

    const { name, email, password } = registerSchema.parse(request.body);

    const userExists = await db("users").where({ email }).first();
    if (userExists) {
      return reply.status(400).send({ error: "User already exists" });
    }

    const password_hash = await bcrypt.hash(password, 8);
    const userId = randomUUID();

    await db("users").insert({
      id: userId,
      name,
      email,
      password_hash,
      email_verified_at: null,
    });

    const rawToken = randomBytes(32).toString("hex");
    const tokenHash = createHash("sha256").update(rawToken).digest("hex");
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);

    await db("email_verification_tokens").insert({
      id: randomUUID(),
      user_id: userId,
      token_hash: tokenHash,
      expires_at: expiresAt,
    });

    emailService.sendVerificationEmail(email, rawToken).catch(console.error);

    return reply.status(201).send();
  });

  app.post(
    "/login",
    { config: { rateLimit: { max: 5, timeWindow: "1 minute" } } },
    async (request, reply) => {
      const loginSchema = z.object({
        email: z.string().email(),
        password: z.string(),
      });

      const { email, password } = loginSchema.parse(request.body);

      const user = await db("users").where({ email }).first();

      if (!user || !user.password_hash) {
        return reply.status(400).send({ error: "Invalid credentials" });
      }

      const isPasswordValid = await bcrypt.compare(
        password,
        user.password_hash,
      );
      if (!isPasswordValid) {
        return reply.status(400).send({ error: "Invalid credentials" });
      }

      if (user.email_verified_at === null) {
        return reply.status(403).send({
          error: "E-mail não verificado.",
          requiresEmailVerification: true,
        });
      }

      if (user.two_factor_enabled) {
        const tempToken = app.jwt.sign(
          { sub: user.id, type: "2fa_partial" },
          { expiresIn: "5m" },
        );

        return reply.status(200).send({
          requiresTwoFactor: true,
          tempToken,
        });
      }

      const token = app.jwt.sign(
        { sub: user.id, type: "access" },
        { expiresIn: "7d" },
      );

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
    },
  );

  app.get("/verify-email", async (request, reply) => {
    const verifySchema = z.object({ token: z.string() });

    const parsed = verifySchema.safeParse(request.query);
    if (!parsed.success) {
      return reply.status(400).send({ error: "Token inválido." });
    }

    const { token } = parsed.data;
    const tokenHash = createHash("sha256").update(token).digest("hex");
    const now = new Date();

    const updatedRows = await db("email_verification_tokens")
      .where("token_hash", tokenHash)
      .whereNull("used_at")
      .where("expires_at", ">", now)
      .update({ used_at: now });

    if (updatedRows === 0) {
      return reply.status(400).send({
        error: "O link de verificação é inválido, expirou ou já foi utilizado.",
      });
    }

    const tokenRecord = await db("email_verification_tokens")
      .where({ token_hash: tokenHash })
      .first();
    if (tokenRecord) {
      await db("users")
        .where({ id: tokenRecord.user_id })
        .update({ email_verified_at: now });
    }

    return reply
      .status(200)
      .send({ message: "E-mail verificado com sucesso." });
  });

  app.post(
    "/resend-verification",
    { config: { rateLimit: { max: 3, timeWindow: "15 minutes" } } },
    async (request, reply) => {
      const resendSchema = z.object({ email: z.string().email() });
      const { email } = resendSchema.parse(request.body);

      const user = await db("users").where({ email }).first();

      if (!user || user.email_verified_at !== null) {
        return reply.status(200).send({
          message:
            "Se o endereço estiver associado a uma conta pendente, enviaremos um novo e-mail.",
        });
      }

      await db("email_verification_tokens")
        .where({ user_id: user.id })
        .whereNull("used_at")
        .update({ used_at: new Date() });

      const rawToken = randomBytes(32).toString("hex");
      const tokenHash = createHash("sha256").update(rawToken).digest("hex");
      const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);

      await db("email_verification_tokens").insert({
        id: randomUUID(),
        user_id: user.id,
        token_hash: tokenHash,
        expires_at: expiresAt,
      });

      emailService.sendVerificationEmail(email, rawToken).catch(console.error);

      return reply.status(200).send({
        message:
          "Se o endereço estiver associado a uma conta pendente, enviaremos um novo e-mail.",
      });
    },
  );

  app.post(
    "/2fa/verify",
    { config: { rateLimit: { max: 5, timeWindow: "1 minute" } } },
    async (request, reply) => {
      const verifySchema = z.object({
        token: z.string().length(6),
      });

      const { token } = verifySchema.parse(request.body);

      let payload;
      try {
        payload = await request.jwtVerify<{ sub: string; type?: string }>();
      } catch (err) {
        return reply
          .status(401)
          .send({ error: "Unauthorized or expired token." });
      }

      if (payload.type !== "2fa_partial") {
        return reply.status(401).send({ error: "Invalid token type." });
      }

      const user = await db("users").where("id", payload.sub).first();
      if (!user || !user.two_factor_enabled || !user.two_factor_secret) {
        return reply
          .status(400)
          .send({ error: "2FA is not enabled for this user." });
      }

      const currentStep = Math.floor(Date.now() / 30000);
      if (user.last_totp_step === currentStep) {
        return reply
          .status(400)
          .send({ error: "Code already used. Please wait for a new code." });
      }

      const isValid = totpService.verifyToken(token, user.two_factor_secret);
      if (!isValid) {
        return reply.status(401).send({ error: "Invalid 2FA code." });
      }

      const updatedRows = await db("users")
        .where("id", user.id)
        .where(function () {
          this.whereNull("last_totp_step").orWhereNot(
            "last_totp_step",
            currentStep,
          );
        })
        .update({
          last_totp_step: currentStep,
        });

      if (updatedRows === 0) {
        return reply
          .status(400)
          .send({ error: "Code already used. Please wait for a new code." });
      }

      const accessToken = app.jwt.sign(
        { sub: user.id, type: "access" },
        { expiresIn: "7d" },
      );

      const userPreferences =
        typeof user.preferences === "string"
          ? JSON.parse(user.preferences)
          : user.preferences || {};

      return reply.status(200).send({
        token: accessToken,
        user: {
          id: user.id,
          name: user.name,
          email: user.email,
          avatar_url: user.avatar_url,
          preferences: userPreferences,
        },
      });
    },
  );

  app.post(
    "/2fa/recovery",
    { config: { rateLimit: { max: 5, timeWindow: "1 minute" } } },
    async (request, reply) => {
      const recoverySchema = z.object({
        recoveryCode: z.string().min(8),
      });

      const { recoveryCode } = recoverySchema.parse(request.body);

      let payload;
      try {
        payload = await request.jwtVerify<{ sub: string; type?: string }>();
      } catch (err) {
        return reply
          .status(401)
          .send({ error: "Unauthorized or expired token." });
      }

      if (payload.type !== "2fa_partial") {
        return reply.status(401).send({ error: "Invalid token type." });
      }

      const user = await db("users").where("id", payload.sub).first();
      if (!user || !user.two_factor_enabled || !user.recovery_codes) {
        return reply
          .status(400)
          .send({ error: "2FA or recovery codes not found." });
      }

      let storedHashes: string[] = [];
      try {
        const parsed =
          typeof user.recovery_codes === "string"
            ? JSON.parse(user.recovery_codes)
            : user.recovery_codes;

        storedHashes = [...parsed];
      } catch (e) {
        return reply
          .status(500)
          .send({ error: "Error parsing recovery codes." });
      }

      let matchedIndex = -1;
      for (let i = 0; i < storedHashes.length; i++) {
        const isMatch = await bcrypt.compare(
          recoveryCode,
          String(storedHashes[i]),
        );
        if (isMatch) {
          matchedIndex = i;
          break;
        }
      }

      if (matchedIndex === -1) {
        return reply.status(401).send({ error: "Invalid recovery code." });
      }

      storedHashes.splice(matchedIndex, 1);
      const newRecoveryCodes = JSON.stringify(storedHashes);

      const originalRecoveryCodesString =
        typeof user.recovery_codes === "string"
          ? user.recovery_codes
          : JSON.stringify(user.recovery_codes);

      const updatedRows = await db("users")
        .where("id", user.id)
        .whereRaw("recovery_codes::jsonb = ?::jsonb", [
          originalRecoveryCodesString,
        ])
        .update({
          recovery_codes: newRecoveryCodes,
        });

      if (updatedRows === 0) {
        return reply
          .status(400)
          .send({ error: "Recovery code already consumed or invalid state." });
      }

      const accessToken = app.jwt.sign(
        { sub: user.id, type: "access" },
        { expiresIn: "7d" },
      );

      const userPreferences =
        typeof user.preferences === "string"
          ? JSON.parse(user.preferences)
          : user.preferences || {};

      return reply.status(200).send({
        token: accessToken,
        user: {
          id: user.id,
          name: user.name,
          email: user.email,
          avatar_url: user.avatar_url,
          preferences: userPreferences,
        },
      });
    },
  );

  app.post(
    "/2fa/generate",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      const userId = request.user.sub;

      const user = await db("users").where("id", userId).first();

      if (!user || !user.email) {
        return reply.status(404).send({ error: "User not found or invalid." });
      }

      if (user.two_factor_enabled) {
        return reply.status(400).send({ error: "2FA is already enabled." });
      }

      const secret = totpService.generateSecret();
      const qrCodeUrl = await totpService.generateQRCode(user.email, secret);

      await db("users").where("id", userId).update({
        two_factor_secret: secret,
      });

      return reply.status(200).send({
        qrCodeUrl,
        secret,
      });
    },
  );

  app.post(
    "/2fa/enable",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      const enableBodySchema = z.object({
        token: z.string().length(6),
      });

      const { token } = enableBodySchema.parse(request.body);
      const userId = request.user.sub;

      const user = await db("users").where("id", userId).first();

      if (!user || !user.two_factor_secret) {
        return reply
          .status(400)
          .send({ error: "2FA generation not initiated." });
      }

      if (user.two_factor_enabled) {
        return reply.status(400).send({ error: "2FA is already enabled." });
      }

      const isValid = totpService.verifyToken(token, user.two_factor_secret);
      if (!isValid) {
        return reply.status(400).send({ error: "Invalid 2FA code." });
      }

      const rawRecoveryCodes = totpService.generateRecoveryCodes();

      const hashedRecoveryCodes = await Promise.all(
        rawRecoveryCodes.map((code) => bcrypt.hash(code, 10)),
      );
      const recoveryCodesPayload = JSON.stringify(hashedRecoveryCodes);

      await db("users").where("id", userId).update({
        two_factor_enabled: true,
        recovery_codes: recoveryCodesPayload,
      });

      return reply.status(200).send({
        message: "2FA enabled successfully.",
        recoveryCodes: rawRecoveryCodes,
      });
    },
  );

  app.post(
    "/2fa/disable",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      const disableBodySchema = z.object({
        password: z.string(),
      });

      const { password } = disableBodySchema.parse(request.body);
      const userId = request.user.sub;

      const user = await db("users").where("id", userId).first();

      if (!user || !user.password_hash) {
        return reply.status(404).send({ error: "User not found." });
      }

      const isPasswordValid = await bcrypt.compare(
        password,
        user.password_hash,
      );
      if (!isPasswordValid) {
        return reply.status(401).send({ error: "Invalid password." });
      }

      await db("users").where("id", userId).update({
        two_factor_enabled: false,
        two_factor_secret: null,
        recovery_codes: null,
        last_totp_step: null,
      });

      return reply.status(200).send({ message: "2FA disabled successfully." });
    },
  );

  app.post("/forgot-password", async (request, reply) => {
    const forgotPasswordSchema = z.object({
      email: z.string().email(),
    });

    const { email } = forgotPasswordSchema.parse(request.body);
    const user = await db("users").where({ email }).first();

    if (user) {
      const token = randomBytes(32).toString("hex");
      const expiresAt = new Date(Date.now() + 30 * 60 * 1000);

      await db("users").where({ id: user.id }).update({
        password_reset_token: token,
        password_reset_expires_at: expiresAt,
      });

      if (emailService?.sendPasswordReset) {
        emailService.sendPasswordReset(email, token).catch(console.error);
      }
    }

    return reply.status(200).send({
      message:
        "Se existir uma conta para este e-mail, enviaremos um link de redefinição.",
    });
  });

  app.post("/reset-password", async (request, reply) => {
    const resetPasswordSchema = z.object({
      token: z.string(),
      password: z.string().min(6),
    });

    const { token, password } = resetPasswordSchema.parse(request.body);

    const user = await db("users")
      .where({ password_reset_token: token })
      .andWhere("password_reset_expires_at", ">", new Date())
      .first();

    if (!user) {
      return reply
        .status(400)
        .send({ error: "Token de recuperação inválido ou expirado." });
    }

    const password_hash = await bcrypt.hash(password, 8);

    await db("users").where({ id: user.id }).update({
      password_hash,
      password_reset_token: null,
      password_reset_expires_at: null,
    });

    return reply.status(204).send();
  });

  app.put(
    "/change-password",
    { preHandler: [checkAuth] },
    async (request, reply) => {
      const changePasswordSchema = z.object({
        currentPassword: z.string().min(1),
        newPassword: z.string().min(6),
      });

      const { currentPassword, newPassword } = changePasswordSchema.parse(
        request.body,
      );
      const userId = request.user.sub;

      const user = await db("users").where({ id: userId }).first();

      if (!user || !user.password_hash) {
        return reply.status(404).send({ error: "Usuário não encontrado." });
      }

      const isPasswordValid = await bcrypt.compare(
        currentPassword,
        user.password_hash,
      );

      if (!isPasswordValid) {
        return reply
          .status(401)
          .send({ error: "A senha atual está incorreta." });
      }

      const password_hash = await bcrypt.hash(newPassword, 8);

      await db("users").where({ id: userId }).update({
        password_hash,
      });

      return reply.status(204).send();
    },
  );
}
