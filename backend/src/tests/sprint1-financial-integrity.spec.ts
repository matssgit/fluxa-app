import { randomUUID } from "node:crypto";
import { beforeEach, describe, expect, it } from "vitest";

import { app } from "../app.js";
import { db } from "../database/database.js";
import { currentCompetence } from "../services/subscription-payments.service.js";

describe("Sprint 1 - integridade financeira e concorrência", () => {
  let userId: string;
  let token: string;
  let accountId: string;
  let categoryId: string;

  const authorization = () => ({ authorization: "Bearer " + token });

  beforeEach(async () => {
    userId = randomUUID();
    accountId = randomUUID();
    categoryId = randomUUID();

    await db("users").insert({
      id: userId,
      name: "Sprint 1 User",
      email: randomUUID() + "@sprint1.test",
      password_hash: "not-used",
    });
    await db("accounts").insert({
      id: accountId,
      user_id: userId,
      name: "Conta Sprint 1",
      type: "checking",
    });
    await db("categories").insert({
      id: categoryId,
      user_id: userId,
      name: "Categoria Sprint 1",
      type: "expense",
      is_default: false,
    });

    token = app.jwt.sign({
      sub: userId,
      type: "access",
      tokenVersion: 0,
    });
  });

  it("reconcilia Accounts, Dashboard, Analytics e Financial Events", async () => {
    const secondAccountId = randomUUID();
    await db("accounts").insert({
      id: secondAccountId,
      user_id: userId,
      name: "Conta Receita",
      type: "checking",
    });

    const entries = [
      {
        title: "Saldo inicial A",
        amount: 1000,
        type: "income",
        account_id: accountId,
      },
      {
        title: "Despesa A",
        amount: 100,
        type: "expense",
        account_id: accountId,
      },
      {
        title: "Saldo inicial B",
        amount: 1000,
        type: "entrada",
        account_id: secondAccountId,
      },
      {
        title: "Receita B",
        amount: 200,
        type: "receita",
        account_id: secondAccountId,
      },
    ];

    for (const payload of entries) {
      const response = await app.inject({
        method: "POST",
        url: "/transactions",
        headers: authorization(),
        payload,
      });
      expect(response.statusCode).toBe(201);
    }

    const accountsResponse = await app.inject({
      method: "GET",
      url: "/accounts",
      headers: authorization(),
    });
    const accounts = accountsResponse.json().accounts;
    expect(accounts.find((account: any) => account.id === accountId).balance).toBe(
      900,
    );
    expect(
      accounts.find((account: any) => account.id === secondAccountId).balance,
    ).toBe(1200);

    const dashboardResponse = await app.inject({
      method: "GET",
      url: "/dashboard",
      headers: authorization(),
    });
    expect(dashboardResponse.statusCode).toBe(200);
    expect(dashboardResponse.json().summary).toEqual({
      currentBalance: 2100,
      totalIncome: 2200,
      totalExpenses: 100,
    });

    const analyticsResponse = await app.inject({
      method: "GET",
      url: "/analytics/dashboard",
      headers: authorization(),
    });
    expect(analyticsResponse.statusCode).toBe(200);
    expect(analyticsResponse.json().metrics.total_balance).toBe(2100);
    expect(analyticsResponse.json().metrics.monthly_income).toBe(2200);
    expect(analyticsResponse.json().metrics.monthly_expenses).toBe(100);

    const eventsResponse = await app.inject({
      method: "GET",
      url: "/financial-events?pageSize=20",
      headers: authorization(),
    });
    expect(eventsResponse.statusCode).toBe(200);
    const events = eventsResponse.json().items;
    expect(events.find((event: any) => event.title === "Despesa A")).toEqual(
      expect.objectContaining({ amount: "100.00", flow: "expense" }),
    );
    expect(events.find((event: any) => event.title === "Receita B")).toEqual(
      expect.objectContaining({ amount: "200.00", flow: "income" }),
    );

    const stored = await db("transactions").where({ user_id: userId });
    expect(stored.every((transaction) => Number(transaction.amount) >= 0)).toBe(
      true,
    );
    expect(new Set(stored.map((transaction) => transaction.type))).toEqual(
      new Set(["entrada", "saida"]),
    );
  });

  it("aceita somente uma de duas compras concorrentes acima do limite somado", async () => {
    const cardId = randomUUID();
    await db("cards").insert({
      id: cardId,
      user_id: userId,
      name: "Cartão Concorrente",
      brand: "Visa",
      total_limit: 100,
      available_limit: 100,
      due_day: 10,
    });

    const purchase = (title: string) =>
      app.inject({
        method: "POST",
        url: "/credit/purchases",
        headers: authorization(),
        payload: {
          card_id: cardId,
          category_id: categoryId,
          title,
          store: "Loja",
          total_amount: 80,
          total_installments: 1,
          purchase_date: new Date().toISOString().split("T")[0],
        },
      });

    const responses = await Promise.all([
      purchase("Compra A"),
      purchase("Compra B"),
    ]);

    expect(responses.filter((response) => response.statusCode === 201)).toHaveLength(
      1,
    );
    expect(responses.filter((response) => response.statusCode === 400)).toHaveLength(
      1,
    );

    const card = await db("cards").where({ id: cardId }).first();
    expect(Number(card?.available_limit)).toBe(20);
    expect(await db("credit_purchases").where({ card_id: cardId })).toHaveLength(
      1,
    );
  });

  it("restaura o limite uma única vez em cancelamentos concorrentes", async () => {
    const cardId = randomUUID();
    const purchaseId = randomUUID();
    await db("cards").insert({
      id: cardId,
      user_id: userId,
      name: "Cartão Cancelamento",
      brand: "Visa",
      total_limit: 100,
      available_limit: 20,
      due_day: 10,
    });
    await db("credit_purchases").insert({
      id: purchaseId,
      user_id: userId,
      card_id: cardId,
      category_id: categoryId,
      title: "Compra a cancelar",
      store: "Loja",
      total_amount: 80,
      total_installments: 1,
      purchase_date: new Date().toISOString().split("T")[0],
    });
    await db("installments").insert({
      id: randomUUID(),
      user_id: userId,
      purchase_id: purchaseId,
      installment_number: 1,
      total_installments: 1,
      amount: 80,
      expected_date: new Date().toISOString().split("T")[0],
      status: "pending",
    });

    const cancel = () =>
      app.inject({
        method: "PATCH",
        url: "/credit/purchases/" + purchaseId + "/cancel",
        headers: authorization(),
      });
    const responses = await Promise.all([cancel(), cancel()]);

    expect(responses.filter((response) => response.statusCode === 204)).toHaveLength(
      1,
    );
    expect(responses.filter((response) => response.statusCode === 400)).toHaveLength(
      1,
    );

    const card = await db("cards").where({ id: cardId }).first();
    expect(Number(card?.available_limit)).toBe(100);
    expect(Number(card?.available_limit)).toBeLessThanOrEqual(
      Number(card?.total_limit),
    );
  });

  it("mantém estado consistente na corrida entre pagar parcela e cancelar compra", async () => {
    const cardId = randomUUID();
    const purchaseId = randomUUID();
    const installmentId = randomUUID();
    await db("cards").insert({
      id: cardId,
      user_id: userId,
      name: "Cartão Pay Cancel",
      brand: "Visa",
      total_limit: 100,
      available_limit: 0,
      due_day: 10,
    });
    await db("credit_purchases").insert({
      id: purchaseId,
      user_id: userId,
      card_id: cardId,
      category_id: categoryId,
      title: "Compra Pay Cancel",
      store: "Loja",
      total_amount: 100,
      total_installments: 1,
      purchase_date: new Date().toISOString().split("T")[0],
    });
    await db("installments").insert({
      id: installmentId,
      user_id: userId,
      purchase_id: purchaseId,
      installment_number: 1,
      total_installments: 1,
      amount: 100,
      expected_date: new Date().toISOString().split("T")[0],
      status: "pending",
    });

    const [paymentResponse, cancelResponse] = await Promise.all([
      app.inject({
        method: "POST",
        url: "/credit/installments/" + installmentId + "/pay",
        headers: authorization(),
        payload: { account_id: accountId },
      }),
      app.inject({
        method: "PATCH",
        url: "/credit/purchases/" + purchaseId + "/cancel",
        headers: authorization(),
      }),
    ]);

    expect([204, 400]).toContain(paymentResponse.statusCode);
    expect(cancelResponse.statusCode).toBe(204);

    const card = await db("cards").where({ id: cardId }).first();
    const installment = await db("installments")
      .where({ id: installmentId })
      .first();
    const transactions = await db("transactions").where({
      user_id: userId,
      account_id: accountId,
    });

    expect(Number(card?.available_limit)).toBe(100);
    expect(["paid", "cancelled"]).toContain(installment?.status);
    expect(transactions.length).toBe(installment?.status === "paid" ? 1 : 0);
    if (transactions[0]) {
      expect(Number(transactions[0].amount)).toBe(100);
      expect(transactions[0].type).toBe("saida");
    }
  });

  it("preserva dois depósitos concorrentes e reconcilia o histórico da Wallet", async () => {
    const createResponse = await app.inject({
      method: "POST",
      url: "/wallets",
      headers: authorization(),
      payload: {
        title: "Meta concorrente",
        target_amount: 1000,
        current_amount: 100,
      },
    });
    expect(createResponse.statusCode).toBe(201);
    const walletId = (
      await db("wallets").where({ user_id: userId }).first()
    )?.id;
    expect(walletId).toBeDefined();

    const deposit = () =>
      app.inject({
        method: "POST",
        url: "/wallets/" + walletId + "/progress",
        headers: authorization(),
        payload: { type: "deposit", amount: 20 },
      });
    const responses = await Promise.all([deposit(), deposit()]);

    expect(responses.every((response) => response.statusCode === 200)).toBe(true);
    const wallet = await db("wallets").where({ id: walletId }).first();
    const history = await db("wallet_history").where({ wallet_id: walletId });
    expect(Number(wallet?.current_amount)).toBe(140);
    expect(
      history.reduce((total, entry) => total + Number(entry.amount), 0),
    ).toBe(140);
  });

  it("impede pagamento duplicado de assinatura entre comandos concorrentes", async () => {
    const subscriptionId = randomUUID();
    await db("subscriptions").insert({
      id: subscriptionId,
      user_id: userId,
      category_id: categoryId,
      account_id: accountId,
      title: "Assinatura idempotente",
      amount: 50,
      due_day: 10,
      frequency: "monthly",
      status: "active",
    });

    const responses = await Promise.all([
      app.inject({
        method: "POST",
        url: "/subscriptions/" + subscriptionId + "/pay",
        headers: authorization(),
        payload: { account_id: accountId },
      }),
      app.inject({
        method: "PATCH",
        url: "/financial-events/" + subscriptionId + "/pay",
        headers: authorization(),
        payload: { account_id: accountId },
      }),
    ]);

    expect(
      responses.filter((response) => [201, 204].includes(response.statusCode)),
    ).toHaveLength(1);
    expect(
      responses.filter((response) => [400, 409].includes(response.statusCode)),
    ).toHaveLength(1);

    const payments = await db("transactions").where({
      subscription_id: subscriptionId,
      competence: currentCompetence(),
    });
    expect(payments).toHaveLength(1);
    expect(Number(payments[0]?.amount)).toBe(50);
    expect(payments[0]?.type).toBe("saida");
  });

  it("não perde consumo concorrente durante a edição de limite", async () => {
    const cardId = randomUUID();
    await db("cards").insert({
      id: cardId,
      user_id: userId,
      name: "Cartão Limite",
      brand: "Visa",
      total_limit: 100,
      available_limit: 100,
      due_day: 10,
    });

    const [purchaseResponse, editResponse] = await Promise.all([
      app.inject({
        method: "POST",
        url: "/credit/purchases",
        headers: authorization(),
        payload: {
          card_id: cardId,
          category_id: categoryId,
          title: "Compra durante edição",
          store: "Loja",
          total_amount: 80,
          total_installments: 1,
          purchase_date: new Date().toISOString().split("T")[0],
        },
      }),
      app.inject({
        method: "PUT",
        url: "/credit/cards/" + cardId,
        headers: authorization(),
        payload: {
          name: "Cartão Limite",
          brand: "Visa",
          total_limit: 120,
          due_day: 10,
          color: "slate",
        },
      }),
    ]);

    expect(purchaseResponse.statusCode).toBe(201);
    expect(editResponse.statusCode).toBe(204);
    const card = await db("cards").where({ id: cardId }).first();
    expect(Number(card?.total_limit)).toBe(120);
    expect(Number(card?.available_limit)).toBe(40);
  });
});
