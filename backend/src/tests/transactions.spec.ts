import { app } from "../app.js";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import { expect, it, beforeAll, afterAll, describe, beforeEach } from "vitest";

describe("Transactions routes", () => {
  let token: string;
  let userId: string;
  let accountId: string;

  beforeAll(async () => {
    await app.ready();

    userId = randomUUID();

    await db("users").insert({
      id: userId,
      name: "Usuário das Transações",
      email: `trans-${randomUUID()}@finance.com`,
      password_hash: "hash_seguro_123",
    });

    token = app.jwt.sign({ sub: userId });
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(async () => {
    accountId = randomUUID();
    await db("accounts").insert({
      id: accountId,
      user_id: userId,
      name: "Conta Principal",
    });
  });

  it("should be able to create a new transaction", async () => {
    const response = await app.inject({
      method: "POST",
      url: "/transactions",
      headers: { Authorization: `Bearer ${token}` },
      payload: {
        title: "New transaction",
        amount: 5000,
        type: "income",
        account_id: accountId,
      },
    });

    expect(response.statusCode).toBe(201);
  });

  it("should be able to list all transactions", async () => {
    await app.inject({
      method: "POST",
      url: "/transactions",
      headers: { Authorization: `Bearer ${token}` },
      payload: {
        title: "New transaction",
        amount: 5000,
        type: "income",
        account_id: accountId,
      },
    });

    const listResponse = await app.inject({
      method: "GET",
      url: "/transactions",
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(listResponse.statusCode).toBe(200);
    const body = JSON.parse(listResponse.body);
    expect(body.transactions).toEqual([
      expect.objectContaining({
        title: "New transaction",
        amount: "5000.00",
      }),
    ]);
  });

  it("should be able to get the summary", async () => {
    await app.inject({
      method: "POST",
      url: "/transactions",
      headers: { Authorization: `Bearer ${token}` },
      payload: {
        title: "Income transaction",
        amount: 5000,
        type: "income",
        account_id: accountId,
      },
    });

    await app.inject({
      method: "POST",
      url: "/transactions",
      headers: { Authorization: `Bearer ${token}` },
      payload: {
        title: "Expense transaction",
        amount: 2000,
        type: "expense",
        account_id: accountId,
      },
    });

    const summaryResponse = await app.inject({
      method: "GET",
      url: "/transactions/summary",
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(summaryResponse.statusCode).toBe(200);
    const bodySummary = JSON.parse(summaryResponse.body);

    expect(bodySummary.summary).toEqual({
      amount: 3000,
      income: 5000,
      expense: 2000,
    });
  });
});
