import { app } from "../app.js";
import { describe, it, expect, beforeAll, afterAll } from "vitest";

describe("RC1.2 - Hardening HTTP & Infraestrutura", () => {
  beforeAll(async () => {
    await app.ready();
  });

  afterAll(async () => {
    await app.close();
  });

  it("Deve injetar os cabeçalhos de segurança do Helmet", async () => {
    const response = await app.inject({
      method: "GET",
      url: "/rota-inexistente-para-testar-headers",
    });

    expect(response.headers["x-xss-protection"]).toBeDefined();
    expect(response.headers["x-content-type-options"]).toBe("nosniff");
    expect(response.headers["strict-transport-security"]).toBeDefined();
    expect(response.headers["x-powered-by"]).toBeUndefined();
  });

  it("Deve bloquear requisições excedentes (HTTP 429) via Rate Limit", async () => {
    let lastResponse;

    for (let i = 0; i <= 100; i++) {
      lastResponse = await app.inject({
        method: "POST",
        url: "/transactions",
        payload: {},
      });
    }

    expect(lastResponse?.statusCode).toBe(429);

    const body = JSON.parse(lastResponse?.payload || "{}");
    expect(body.message).toBe(
      "Limite de requisições excedido. Tente novamente em 1 minuto.",
    );
    expect(body.stack).toBeUndefined();
  });
});
