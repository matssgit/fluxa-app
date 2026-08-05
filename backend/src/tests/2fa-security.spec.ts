import { app } from "../app.js";
import { randomUUID } from "node:crypto";
import { describe, it, expect, beforeAll } from "vitest";
import { generateTestToken } from "./utils/auth-helper.js";

describe("Segurança 2FA - Bypass Prevention", () => {
  beforeAll(async () => {
    await app.ready();
  });

  it("NÃO deve permitir que um token '2fa_partial' acesse rotas privadas", async () => {
    const fakeUserId = randomUUID();

    const partialToken = generateTestToken(fakeUserId, "2fa_partial");

    const response = await app.inject({
      method: "GET",
      url: "/dashboard",
      headers: { authorization: `Bearer ${partialToken}` },
    });

    expect(response.statusCode).toBe(401);
    expect(JSON.parse(response.payload).error).toBe(
      "Unauthorized. 2FA verification required.",
    );
  });

  it("DEVE permitir que um token 'access' acesse rotas privadas", async () => {
    const fakeUserId = randomUUID();
    const accessToken = generateTestToken(fakeUserId, "access");

    const response = await app.inject({
      method: "GET",
      url: "/dashboard",
      headers: { authorization: `Bearer ${accessToken}` },
    });

    expect(response.statusCode).not.toBe(401);
  });
});
