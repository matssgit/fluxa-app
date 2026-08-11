import { afterAll, beforeAll, describe, expect, it } from "vitest";

import { app } from "../app.js";
import { authenticator } from "otplib";
import bcrypt from "bcrypt";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";

describe("Fase 5 - Auditoria de Segurança 2FA", () => {
  let userId: string;
  let userEmail = "2fa_audit@fluxa.com";
  let userPass = "StrongPass@2026";
  let partialToken: string;
  let rawRecoveryCodes: string[] = [];
  let userSecret: string;

  beforeAll(async () => {
    await app.ready();
    userId = randomUUID();
    await db("users").insert({
      id: userId,
      name: "2FA Auditor",
      email: userEmail,
      password_hash: await bcrypt.hash(userPass, 8),
      email_verified_at: new Date(),
    });
  });

  afterAll(async () => {
    await db("users").where({ id: userId }).delete();
  });

  describe("1. Auditoria do Fluxo Base (Sem 2FA)", () => {
    it("Deve realizar login normalmente sem 2FA e emitir type: 'access'", async () => {
      const res = await app.inject({
        method: "POST",
        url: "/users/login",
        payload: { email: userEmail, password: userPass },
      });
      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.payload);
      expect(body.token).toBeDefined();
      expect(body.user).toBeDefined();

      const decoded = app.jwt.verify<{ type: string }>(body.token);
      expect(decoded.type).toBe("access");
    });
  });

  describe("2. Geração e Ativação do 2FA", () => {
    let accessToken: string;

    beforeAll(async () => {
      const res = await app.inject({
        method: "POST",
        url: "/users/login",
        payload: { email: userEmail, password: userPass },
      });
      accessToken = JSON.parse(res.payload).token;
    });

    it("Deve gerar o secret do 2FA", async () => {
      const res = await app.inject({
        method: "POST",
        url: "/users/2fa/generate",
        headers: { authorization: `Bearer ${accessToken}` },
      });
      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.payload);
      expect(body.qrCodeUrl).toBeDefined();
      expect(body.secret).toBeDefined();
      userSecret = body.secret;
    });

    it("Deve ativar o 2FA e devolver Recovery Codes", async () => {
      const validCode = authenticator.generate(userSecret);
      const res = await app.inject({
        method: "POST",
        url: "/users/2fa/enable",
        headers: { authorization: `Bearer ${accessToken}` },
        payload: { token: validCode },
      });
      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.payload);
      expect(body.recoveryCodes).toHaveLength(10);
      rawRecoveryCodes = body.recoveryCodes;
    });

    it("Validação de Armazenamento: Não deve armazenar Recovery Codes em texto puro", async () => {
      const user = await db("users").where({ id: userId }).first();

      expect(
        user?.two_factor_enabled === 1 || user?.two_factor_enabled === true,
      ).toBe(true);

      const savedCodes =
        typeof user?.recovery_codes === "string"
          ? JSON.parse(user.recovery_codes)
          : user?.recovery_codes;
      expect(savedCodes[0]).not.toBe(rawRecoveryCodes[0]);

      const isMatch = await bcrypt.compare(
        rawRecoveryCodes[0] as string,
        savedCodes[0] as string,
      );
      expect(isMatch).toBe(true);
    });
  });

  describe("3. Fluxo de Login com 2FA", () => {
    it("Não deve retornar token de acesso, e sim tempToken", async () => {
      const res = await app.inject({
        method: "POST",
        url: "/users/login",
        payload: { email: userEmail, password: userPass },
      });
      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.payload);
      expect(body.requiresTwoFactor).toBe(true);
      expect(body.tempToken).toBeDefined();
      expect(body.user).toBeUndefined();
      partialToken = body.tempToken;

      const decoded = app.jwt.verify<{ type: string }>(partialToken);
      expect(decoded.type).toBe("2fa_partial");
    });

    it("Bypass Prevention: token parcial não pode acessar rota privada", async () => {
      const res = await app.inject({
        method: "GET",
        url: "/users/export",
        headers: { authorization: `Bearer ${partialToken}` },
      });
      expect(res.statusCode).toBe(401);
    });
  });

  describe("4. Race Conditions e Replay do TOTP", () => {
    it("Deve blindar concorrência no /2fa/verify", async () => {
      const validCode = authenticator.generate(userSecret);

      const req1 = app.inject({
        method: "POST",
        url: "/users/2fa/verify",
        headers: { authorization: `Bearer ${partialToken}` },
        payload: { token: validCode },
      });
      const req2 = app.inject({
        method: "POST",
        url: "/users/2fa/verify",
        headers: { authorization: `Bearer ${partialToken}` },
        payload: { token: validCode },
      });

      const [res1, res2] = await Promise.all([req1, req2]);

      const statuses = [res1.statusCode, res2.statusCode];
      expect(statuses).toContain(200);
      expect(statuses).toContain(400);
    });
  });

  describe("5. Race Conditions e Replay de Recovery Codes", () => {
    it("Deve blindar concorrência no /2fa/recovery", async () => {
      const resLogin = await app.inject({
        method: "POST",
        url: "/users/login",
        payload: { email: userEmail, password: userPass },
      });
      const newPartial = JSON.parse(resLogin.payload).tempToken;

      const codeToUse = rawRecoveryCodes[1] as string;

      const req1 = app.inject({
        method: "POST",
        url: "/users/2fa/recovery",
        headers: { authorization: `Bearer ${newPartial}` },
        payload: { recoveryCode: codeToUse },
      });
      const req2 = app.inject({
        method: "POST",
        url: "/users/2fa/recovery",
        headers: { authorization: `Bearer ${newPartial}` },
        payload: { recoveryCode: codeToUse },
      });

      const [res1, res2] = await Promise.all([req1, req2]);

      const statuses = [res1.statusCode, res2.statusCode];
      expect(statuses).toContain(200);
      expect(statuses).toContain(400);
    });
  });

  describe("6. Rate Limit Test", () => {
    it("Deve retornar 429 Too Many Requests após 5 tentativas no login", async () => {
      let lastStatus = 0;
      for (let i = 0; i < 6; i++) {
        const res = await app.inject({
          method: "POST",
          url: "/users/login",
          payload: { email: "wrong@email.com", password: "wrong" },
        });
        lastStatus = res.statusCode;
      }
      expect(lastStatus).toBe(429);
    });
  });
});
