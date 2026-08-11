import { beforeEach, describe, expect, it } from "vitest";

import { app } from "../app.js";
import { db } from "../database/database.js";

describe("RC1.1 - Concorrência & Integridade Financeira (Race Conditions)", () => {
  let userToken: string;
  let testAccountId: string;
  let testInstallmentId: string;

  beforeEach(async () => {
    const userPayload = {
      name: "User Race Condition",
      email: `race-${Date.now()}@test.com`,
      password: "StrongPass@2026",
    };
    const createUserResponse = await app.inject({
      method: "POST",
      url: "/users/register",
      payload: userPayload,
    });
    if (createUserResponse.statusCode > 201)
      throw new Error(`Erro ao criar usuário: ${createUserResponse.body}`);

    await db("users")
      .where({ email: userPayload.email })
      .update({ email_verified_at: new Date() });

    const loginResponse = await app.inject({
      method: "POST",
      url: "/users/login",
      payload: { email: userPayload.email, password: userPayload.password },
    });
    if (loginResponse.statusCode !== 200)
      throw new Error(`Erro no login: ${loginResponse.body}`);
    userToken = loginResponse.json().token;

    const accountResponse = await app.inject({
      method: "POST",
      url: "/accounts",
      headers: { authorization: `Bearer ${userToken}` },
      payload: { name: "Conta Teste", balance: 1000, type: "checking" },
    });
    if (accountResponse.statusCode > 201)
      throw new Error(`Erro ao criar conta: ${accountResponse.body}`);

    const getAccountsResponse = await app.inject({
      method: "GET",
      url: "/accounts",
      headers: { authorization: `Bearer ${userToken}` },
    });
    testAccountId = getAccountsResponse
      .json()
      .accounts.find((a: any) => a.name === "Conta Teste").id;

    const categoryResponse = await app.inject({
      method: "POST",
      url: "/categories",
      headers: { authorization: `Bearer ${userToken}` },
      payload: {
        name: "Categoria Teste",
        type: "expense",
        color: "#FF0000",
        icon: "shopping-cart",
      },
    });
    if (categoryResponse.statusCode > 201)
      throw new Error(`Erro ao criar categoria: ${categoryResponse.body}`);

    const getCategoriesResponse = await app.inject({
      method: "GET",
      url: "/categories",
      headers: { authorization: `Bearer ${userToken}` },
    });
    const testCategoryId = getCategoriesResponse
      .json()
      .categories.find((c: any) => c.name === "Categoria Teste").id;

    const cardResponse = await app.inject({
      method: "POST",
      url: "/credit/cards",
      headers: { authorization: `Bearer ${userToken}` },
      payload: {
        name: "Cartão Teste",
        brand: "Mastercard",
        limit_amount: 500,
        due_day: 10,
      },
    });
    if (cardResponse.statusCode > 201)
      throw new Error(`Erro ao criar cartão: ${cardResponse.body}`);

    const getCardsResponse = await app.inject({
      method: "GET",
      url: "/credit/cards",
      headers: { authorization: `Bearer ${userToken}` },
    });
    const cardId = getCardsResponse
      .json()
      .cards.find((c: any) => c.name === "Cartão Teste").id;

    const purchaseResponse = await app.inject({
      method: "POST",
      url: "/credit/purchases",
      headers: { authorization: `Bearer ${userToken}` },
      payload: {
        card_id: cardId,
        category_id: testCategoryId,
        title: "Compra Concorrência",
        store: "Loja Teste",
        total_amount: 100,
        total_installments: 1,
        purchase_date: new Date().toISOString().split("T")[0],
      },
    });
    if (purchaseResponse.statusCode > 201)
      throw new Error(`Erro ao criar compra: ${purchaseResponse.body}`);

    const installmentsResponse = await app.inject({
      method: "GET",
      url: "/credit/installments",
      headers: { authorization: `Bearer ${userToken}` },
    });

    const installment = installmentsResponse
      .json()
      .installments.find((i: any) => i.status === "pending");
    if (!installment) throw new Error("Nenhuma parcela pendente encontrada!");
    testInstallmentId = installment.id;
  });

  it("Deve blindar /installments/:id/pay contra Race Conditions (Pagamento Múltiplo)", async () => {
    const concurrentRequests = Array.from({ length: 5 }).map(() =>
      app.inject({
        method: "POST",
        url: `/credit/installments/${testInstallmentId}/pay`,
        headers: { authorization: `Bearer ${userToken}` },
        payload: { account_id: testAccountId },
      }),
    );

    const responses = await Promise.all(concurrentRequests);

    const successfulResponses = responses.filter(
      (r) => r.statusCode === 204 || r.statusCode === 200,
    );
    const errorResponses = responses.filter((r) => r.statusCode === 400);

    expect(successfulResponses.length).toBe(1);
    expect(errorResponses.length).toBe(4);

    const checkAccount = await app.inject({
      method: "GET",
      url: "/accounts",
      headers: { authorization: `Bearer ${userToken}` },
    });

    const accounts = checkAccount.json().accounts;
    const account = accounts.find((a: any) => a.id === testAccountId);

    expect(account.balance).toBe(-100);
  });
});
