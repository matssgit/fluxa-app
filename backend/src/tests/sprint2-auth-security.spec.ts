import bcrypt from "bcrypt";
import { authenticator } from "otplib";
import { createHash, randomBytes, randomUUID } from "node:crypto";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { app } from "../app.js";
import { db } from "../database/database.js";
import { emailService } from "../services/email.service.js";

const password = "StrongPass@2026";

describe.sequential("Sprint 2 - segurança de sessão e autenticação", () => {
  let userId: string;
  let email: string;

  beforeEach(async () => {
    userId = randomUUID();
    email = `sprint2-${userId}@fluxa.test`;
    await db("users").insert({
      id: userId,
      name: "Sprint 2",
      email,
      password_hash: await bcrypt.hash(password, 8),
      email_verified_at: new Date(),
    });
  });

  function accessToken(tokenVersion = 0, expiresIn: string | number = "7d") {
    return app.jwt.sign(
      { sub: userId, type: "access", tokenVersion },
      { expiresIn },
    );
  }

  async function getMe(token: string) {
    return app.inject({
      method: "GET",
      url: "/users/me",
      headers: { authorization: `Bearer ${token}` },
    });
  }

  it("aceita JWT atual e rejeita versão antiga, ausente e malformada", async () => {
    expect((await getMe(accessToken())).statusCode).toBe(200);
    expect((await getMe(accessToken(1))).statusCode).toBe(401);

    const missingVersion = app.jwt.sign({
      sub: userId,
      type: "access",
    });
    expect((await getMe(missingVersion)).statusCode).toBe(401);

    const malformedVersion = app.jwt.sign({
      sub: userId,
      type: "access",
      tokenVersion: "0" as unknown as number,
    });
    expect((await getMe(malformedVersion)).statusCode).toBe(401);
  });

  it("rejeita JWT expirado", async () => {
    const expiredPayload = {
      sub: userId,
      type: "access" as const,
      tokenVersion: 0,
      exp: Math.floor(Date.now() / 1000) - 60,
    };
    const expiredToken = app.jwt.sign(expiredPayload);
    expect((await getMe(expiredToken)).statusCode).toBe(401);
  });

  it("rejeita filtros financeiros inválidos antes de chegar ao repositório", async () => {
    const token = accessToken();
    const queries = [
      "page=0",
      "pageSize=101",
      "page=NaN",
      "startDate=2026-99-99",
      "startDate=2026-02-31",
      "status=unknown",
      "accountIds=not-a-uuid",
      "minAmount=20&maxAmount=10",
      "startDate=2026-09-20&endDate=2026-09-01",
    ];

    const responses = await Promise.all(
      queries.map((query) =>
        app.inject({
          method: "GET",
          url: `/financial-events?${query}`,
          headers: { authorization: `Bearer ${token}` },
        }),
      ),
    );

    expect(responses.every(({ statusCode }) => statusCode === 400)).toBe(true);
  });

  it("alteração de senha incrementa a versão e revoga o JWT anterior", async () => {
    const oldToken = accessToken();
    const response = await app.inject({
      method: "PUT",
      url: "/users/change-password",
      headers: { authorization: `Bearer ${oldToken}` },
      payload: {
        currentPassword: password,
        newPassword: "AnotherStrong@2026",
      },
    });

    expect(response.statusCode).toBe(204);
    expect((await getMe(oldToken)).statusCode).toBe(401);
    const user = await db("users").where({ id: userId }).first();
    expect(user?.token_version).toBe(1);
  });

  it("armazena somente o SHA-256 do token de reset", async () => {
    let rawToken = "";
    vi.spyOn(emailService, "sendPasswordReset").mockImplementation(
      async (_email, token) => {
        rawToken = token;
      },
    );

    const response = await app.inject({
      method: "POST",
      url: "/users/forgot-password",
      payload: { email },
    });
    expect(response.statusCode).toBe(200);

    await vi.waitFor(() => expect(rawToken).not.toBe(""));
    const user = await db("users").where({ id: userId }).first();
    expect(user?.password_reset_token_hash).not.toBe(rawToken);
    expect(user?.password_reset_token_hash).toBe(
      createHash("sha256").update(rawToken).digest("hex"),
    );
  });

  it("reset correto troca a senha, revoga sessões e não permite reuso", async () => {
    const rawToken = randomBytes(32).toString("hex");
    await db("users").where({ id: userId }).update({
      password_reset_token_hash: createHash("sha256")
        .update(rawToken)
        .digest("hex"),
      password_reset_expires_at: new Date(Date.now() + 60_000),
    });
    const oldToken = accessToken();
    const payload = { token: rawToken, password: "ResetStrong@2026" };

    const first = await app.inject({
      method: "POST",
      url: "/users/reset-password",
      payload,
    });
    const second = await app.inject({
      method: "POST",
      url: "/users/reset-password",
      payload,
    });

    expect(first.statusCode).toBe(204);
    expect(second.statusCode).toBe(400);
    expect((await getMe(oldToken)).statusCode).toBe(401);
    const user = await db("users").where({ id: userId }).first();
    expect(user?.token_version).toBe(1);
    expect(await bcrypt.compare(payload.password, user!.password_hash)).toBe(
      true,
    );
  });

  it("rejeita token de reset inválido e expirado", async () => {
    const rawToken = randomBytes(32).toString("hex");
    await db("users").where({ id: userId }).update({
      password_reset_token_hash: createHash("sha256")
        .update(rawToken)
        .digest("hex"),
      password_reset_expires_at: new Date(Date.now() - 1_000),
    });

    const invalid = await app.inject({
      method: "POST",
      url: "/users/reset-password",
      payload: { token: "invalid", password: "ResetStrong@2026" },
    });
    const expired = await app.inject({
      method: "POST",
      url: "/users/reset-password",
      payload: { token: rawToken, password: "ResetStrong@2026" },
    });
    expect(invalid.statusCode).toBe(400);
    expect(expired.statusCode).toBe(400);
  });

  it("permite somente um reset em duas requisições concorrentes", async () => {
    const rawToken = randomBytes(32).toString("hex");
    await db("users").where({ id: userId }).update({
      password_reset_token_hash: createHash("sha256")
        .update(rawToken)
        .digest("hex"),
      password_reset_expires_at: new Date(Date.now() + 60_000),
    });
    const payload = { token: rawToken, password: "ResetStrong@2026" };

    const responses = await Promise.all([
      app.inject({ method: "POST", url: "/users/reset-password", payload }),
      app.inject({ method: "POST", url: "/users/reset-password", payload }),
    ]);

    expect(responses.map(({ statusCode }) => statusCode).sort()).toEqual([
      204, 400,
    ]);
  });

  it("ativar 2FA incrementa a versão e revoga o JWT anterior", async () => {
    const secret = authenticator.generateSecret();
    await db("users").where({ id: userId }).update({
      two_factor_secret: secret,
    });
    const oldToken = accessToken();
    const response = await app.inject({
      method: "POST",
      url: "/users/2fa/enable",
      headers: { authorization: `Bearer ${oldToken}` },
      payload: { token: authenticator.generate(secret) },
    });

    expect(response.statusCode).toBe(200);
    expect((await getMe(oldToken)).statusCode).toBe(401);
  });

  it("desativar 2FA incrementa a versão e revoga o JWT anterior", async () => {
    await db("users").where({ id: userId }).update({
      two_factor_enabled: true,
      two_factor_secret: authenticator.generateSecret(),
      recovery_codes: JSON.stringify([]),
    });
    const oldToken = accessToken();
    const response = await app.inject({
      method: "POST",
      url: "/users/2fa/disable",
      headers: { authorization: `Bearer ${oldToken}` },
      payload: { password },
    });

    expect(response.statusCode).toBe(200);
    expect((await getMe(oldToken)).statusCode).toBe(401);
  });

  it("consome token e confirma e-mail na mesma operação protegida", async () => {
    await db("users").where({ id: userId }).update({
      email_verified_at: null,
    });
    const rawToken = randomBytes(32).toString("hex");
    const tokenHash = createHash("sha256").update(rawToken).digest("hex");
    await db("email_verification_tokens").insert({
      id: randomUUID(),
      user_id: userId,
      token_hash: tokenHash,
      expires_at: new Date(Date.now() + 60_000),
    });

    const responses = await Promise.all([
      app.inject({
        method: "GET",
        url: `/users/verify-email?token=${rawToken}`,
      }),
      app.inject({
        method: "GET",
        url: `/users/verify-email?token=${rawToken}`,
      }),
    ]);

    expect(responses.map(({ statusCode }) => statusCode).sort()).toEqual([
      200, 400,
    ]);
    const token = await db("email_verification_tokens")
      .where({ token_hash: tokenHash })
      .first();
    const user = await db("users").where({ id: userId }).first();
    expect(token?.used_at).not.toBeNull();
    expect(user?.email_verified_at).not.toBeNull();
  });
});
