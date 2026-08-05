import bcrypt from "bcrypt";
import { app } from "../app.js";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import { describe, it, expect, beforeAll } from "vitest";

describe("Segurança & Isolamento Multiusuário (BOLA/IDOR)", () => {
  let tokenA: string;
  let tokenB: string;
  let userAId: string;
  let userBId: string;

  let accountBId: string;
  let categoryBId: string;
  let cardBId: string;

  beforeAll(async () => {
    await app.ready();

    await db("installments").del();
    await db("credit_purchases").del();
    await db("transactions").del();
    await db("subscriptions").del();
    await db("wallet_history").del();
    await db("wallets").del();
    await db("cards").del();
    await db("categories").del();
    await db("accounts").del();
    await db("users").del();

    userAId = randomUUID();
    await db("users").insert({
      id: userAId,
      name: "Matheus",
      email: "matheus@test.com",
      password_hash: await bcrypt.hash("password123", 8),
    });

    userBId = randomUUID();
    await db("users").insert({
      id: userBId,
      name: "Paloma",
      email: "paloma@test.com",
      password_hash: await bcrypt.hash("password123", 8),
    });

    const loginA = await app.inject({
      method: "POST",
      url: "/users/login",
      payload: { email: "matheus@test.com", password: "password123" },
    });
    tokenA = loginA.json().token;

    const loginB = await app.inject({
      method: "POST",
      url: "/users/login",
      payload: { email: "paloma@test.com", password: "password123" },
    });
    tokenB = loginB.json().token;

    accountBId = randomUUID();
    await db("accounts").insert({
      id: accountBId,
      user_id: userBId,
      name: "Conta da Paloma",
      type: "checking",
    });

    categoryBId = randomUUID();
    await db("categories").insert({
      id: categoryBId,
      user_id: userBId,
      name: "Categoria da Paloma",
      type: "expense",
    });

    cardBId = randomUUID();
    await db("cards").insert({
      id: cardBId,
      user_id: userBId,
      name: "Cartão da Paloma",
      brand: "Mastercard",
      total_limit: 5000,
      available_limit: 5000,
      due_day: 10,
    });
  });

  describe("Suíte 1: Resiliência de Autenticação (Middleware)", () => {
    it("Deve bloquear acesso sem token", async () => {
      const response = await app.inject({
        method: "GET",
        url: "/dashboard",
      });
      expect(response.statusCode).toBe(401);
    });

    it("Deve bloquear acesso com token malformado", async () => {
      const response = await app.inject({
        method: "GET",
        url: "/dashboard",
        headers: { authorization: "Bearer token-invalido-123" },
      });
      expect(response.statusCode).toBe(401);
    });

    it("Deve bloquear acesso se token for inválido, mesmo possuindo sessionId (Bypass de Middleware)", async () => {
      const response = await app.inject({
        method: "GET",
        url: "/dashboard",
        headers: { authorization: "Bearer token-invalido-123" },
        cookies: { sessionId: "fake-session-cookie" },
      });
      expect(response.statusCode).toBe(401);
    });
  });

  describe("Suíte 2: Isolamento Básico Multiusuário", () => {
    it("Usuário A NÃO deve conseguir deletar a Conta do Usuário B", async () => {
      const response = await app.inject({
        method: "DELETE",
        url: `/accounts/${accountBId}`,
        headers: { authorization: `Bearer ${tokenA}` },
      });
      expect(response.statusCode).toBe(404);
    });

    it("Usuário A NÃO deve conseguir descobrir se o Cartão do Usuário B possui faturas (Vazamento BOLA)", async () => {
      await app.inject({
        method: "POST",
        url: "/credit/purchases",
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          card_id: cardBId,
          category_id: categoryBId,
          title: "Compra Teste",
          store: "Loja",
          total_amount: 100,
          total_installments: 1,
          purchase_date: new Date().toISOString().split("T")[0],
        },
      });

      const response = await app.inject({
        method: "DELETE",
        url: `/credit/cards/${cardBId}`,
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(response.statusCode).toBe(404);
    });
  });

  describe("Suíte 3: Relacionamentos Cruzados (IDOR/BOLA)", () => {
    it("Usuário A NÃO deve conseguir atrelar uma Transação à Conta do Usuário B", async () => {
      const response = await app.inject({
        method: "POST",
        url: "/transactions",
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          title: "Ataque Transacional",
          amount: 50,
          account_id: accountBId,
          type: "saida",
        },
      });
      expect(response.statusCode).not.toBe(201);
    });

    it("Usuário A NÃO deve conseguir criar uma Compra de Crédito na Categoria do Usuário B", async () => {
      const cardAId = randomUUID();
      await db("cards").insert({
        id: cardAId,
        user_id: userAId,
        name: "Cartão do Matheus",
        brand: "Visa",
        total_limit: 1000,
        available_limit: 1000,
        due_day: 5,
      });

      const response = await app.inject({
        method: "POST",
        url: "/credit/purchases",
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          card_id: cardAId,
          category_id: categoryBId,
          title: "Ataque na Categoria",
          store: "Hack",
          total_amount: 100,
          total_installments: 1,
          purchase_date: new Date().toISOString().split("T")[0],
        },
      });
      expect(response.statusCode).not.toBe(201);
    });

    it("CRÍTICO: Usuário A NÃO deve conseguir pagar sua Assinatura deduzindo o saldo da Conta do Usuário B", async () => {
      const categoryAId = randomUUID();
      await db("categories").insert({
        id: categoryAId,
        user_id: userAId,
        name: "Categoria do Matheus",
        type: "expense",
      });

      const subAId = randomUUID();
      await db("subscriptions").insert({
        id: subAId,
        user_id: userAId,
        category_id: categoryAId,
        title: "Spotify do Matheus",
        amount: 21.9,
        frequency: "monthly",
        status: "active",
        due_day: 15,
      });

      const response = await app.inject({
        method: "POST",
        url: `/subscriptions/${subAId}/pay`,
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          account_id: accountBId,
        },
      });
      expect(response.statusCode).not.toBe(201);
    });
  });
});
