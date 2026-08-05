import { app } from "../app.js";
import { db } from "../database/database.js";
import { randomUUID } from "node:crypto";
import { describe, it, expect, beforeAll } from "vitest";

describe("Domínio Financeiro - Ecossistema de Crédito", () => {
  let token: string;
  let userId: string;
  let defaultCategoryId: string;

  beforeAll(async () => {
    userId = randomUUID();
    defaultCategoryId = randomUUID();

    await db("users").insert({
      id: userId,
      name: "Matheus Teste",
      email: `teste-${randomUUID()}@finance.com`,
      password_hash: "hash_senha_segura",
    });

    await db("categories").insert({
      id: defaultCategoryId,
      user_id: userId,
      name: "Tecnologia",
      color: "purple",
      icon: "Laptop",
      is_default: false,
      type: "expense",
    });

    token = app.jwt.sign({ sub: userId });
  });

  describe("Sub-domínio: Cartões de Crédito", () => {
    it("Deve criar um cartão com limites total e disponível idênticos", async () => {
      const response = await app.inject({
        method: "POST",
        url: "/credit/cards",
        headers: { Authorization: `Bearer ${token}` },
        payload: {
          name: "PicPay Teste",
          brand: "Visa",
          limit_amount: 3000,
          due_day: 10,
          color: "green",
        },
      });

      expect(response.statusCode).toBe(201);
      const card = JSON.parse(response.body);

      expect(card.id).toBeDefined();
      expect(Number(card.total_limit)).toBe(3000);
      expect(Number(card.available_limit)).toBe(3000);
    });

    it("Não deve permitir reduzir o limite total abaixo do valor já consumido", async () => {
      const cardId = randomUUID();
      await db("cards").insert({
        id: cardId,
        user_id: userId,
        name: "Nubank Teste",
        brand: "Mastercard",
        total_limit: 1000,
        available_limit: 400,
        due_day: 5,
      });

      const response = await app.inject({
        method: "PUT",
        url: `/credit/cards/${cardId}`,
        headers: { Authorization: `Bearer ${token}` },
        payload: {
          name: "Nubank Teste",
          brand: "Mastercard",
          total_limit: 500,
          due_day: 5,
          color: "purple",
        },
      });

      expect(response.statusCode).toBe(400);
      const error = JSON.parse(response.body);
      expect(error.message).toContain("O novo limite não pode ser menor");
    });

    it("Deve bloquear a exclusão de cartões com parcelas pendentes", async () => {
      const cardId = randomUUID();
      const purchaseId = randomUUID();

      await db("cards").insert({
        id: cardId,
        user_id: userId,
        name: "Cartão Bloqueado",
        brand: "Elo",
        total_limit: 2000,
        available_limit: 2000,
        due_day: 15,
      });

      await db("credit_purchases").insert({
        id: purchaseId,
        user_id: userId,
        card_id: cardId,
        category_id: defaultCategoryId,
        title: "Compra Trava",
        store: "Loja",
        total_amount: 100,
        total_installments: 1,
        purchase_date: "2026-06-30",
      });

      await db("installments").insert({
        id: randomUUID(),
        user_id: userId,
        purchase_id: purchaseId,
        installment_number: 1,
        total_installments: 1,
        amount: 100,
        expected_date: "2026-07-15",
        status: "pending",
      });

      const response = await app.inject({
        method: "DELETE",
        url: `/credit/cards/${cardId}`,
        headers: { Authorization: `Bearer ${token}` },
      });

      expect(response.statusCode).toBe(409);
      const error = JSON.parse(response.body);
      expect(error.message).toContain("Não é possível excluir este cartão");
    });
  });

  describe("Sub-domínio: Compras e Divisão Matemática", () => {
    it("Deve abater o available_limit mantendo total_limit e distribuir centavos de dízima na última parcela", async () => {
      const cardId = randomUUID();
      await db("cards").insert({
        id: cardId,
        user_id: userId,
        name: "Itaú Teste",
        brand: "Mastercard",
        total_limit: 5000,
        available_limit: 5000,
        due_day: 20,
      });

      const response = await app.inject({
        method: "POST",
        url: "/credit/purchases",
        headers: { Authorization: `Bearer ${token}` },
        payload: {
          card_id: cardId,
          category_id: defaultCategoryId,
          title: "Curso de Go",
          store: "Plataforma de Ensino",
          total_amount: 100,
          total_installments: 3,
          purchase_date: "2026-06-30",
        },
      });

      expect(response.statusCode).toBe(201);

      const cardAfter = await db("cards").where({ id: cardId }).first();
      expect(Number(cardAfter?.total_limit)).toBe(5000);
      expect(Number(cardAfter?.available_limit)).toBe(4900);

      const purchase = await db("credit_purchases")
        .where({ card_id: cardId })
        .first();
      const installments = await db("installments")
        .where({ purchase_id: purchase?.id })
        .orderBy("installment_number", "asc");

      expect(installments.length).toBe(3);

      expect(Number(installments[0].amount)).toBe(33.33);
      expect(Number(installments[1].amount)).toBe(33.33);
      expect(Number(installments[2].amount)).toBe(33.34);

      const sum = installments.reduce(
        (acc, inst) => acc + Number(inst.amount),
        0,
      );
      expect(sum).toBe(100);

      const formatDate = (d: any) => new Date(d).toISOString().split("T")[0];

      expect(formatDate(installments[0].expected_date)).toBe("2026-07-20");
      expect(formatDate(installments[1].expected_date)).toBe("2026-08-20");
      expect(formatDate(installments[2].expected_date)).toBe("2026-09-20");
    });
  });

  describe("Sub-domínio: Fluxo de Caixa e Estornos", () => {
    it("Deve dar baixa em uma parcela, criar a transação na conta e restaurar o limite do cartão", async () => {
      const accountId = randomUUID();
      await db("accounts").insert({
        id: accountId,
        user_id: userId,
        name: "Conta Corrente Teste",
      });

      const cardId = randomUUID();
      await db("cards").insert({
        id: cardId,
        user_id: userId,
        name: "Mastercard Teste",
        brand: "Mastercard",
        total_limit: 1000,
        available_limit: 900,
        due_day: 10,
      });

      const purchaseId = randomUUID();
      await db("credit_purchases").insert({
        id: purchaseId,
        user_id: userId,
        card_id: cardId,
        category_id: defaultCategoryId,
        title: "Compra Teste Pagamento",
        store: "Loja",
        total_amount: 100,
        total_installments: 1,
        purchase_date: "2026-06-30",
      });

      const installmentId = randomUUID();
      await db("installments").insert({
        id: installmentId,
        user_id: userId,
        purchase_id: purchaseId,
        installment_number: 1,
        total_installments: 1,
        amount: 100,
        expected_date: "2026-07-10",
        status: "pending",
      });

      const response = await app.inject({
        method: "POST",
        url: `/credit/installments/${installmentId}/pay`,
        headers: { Authorization: `Bearer ${token}` },
        payload: { account_id: accountId },
      });

      expect(response.statusCode).toBe(204);

      const paidInstallment = await db("installments")
        .where({ id: installmentId })
        .first();
      expect(paidInstallment?.status).toBe("paid");

      const restoredCard = await db("cards").where({ id: cardId }).first();
      expect(Number(restoredCard?.available_limit)).toBe(1000);

      const transaction = await db("transactions")
        .where({ account_id: accountId })
        .first();
      expect(transaction).toBeTruthy();
      expect(Number(transaction?.amount)).toBe(-100);
    });

    it("Deve cancelar uma compra, estornar o limite das parcelas pendentes e preservar as pagas", async () => {
      const cardId = randomUUID();
      await db("cards").insert({
        id: cardId,
        user_id: userId,
        name: "Visa Cancelamento",
        brand: "Visa",
        total_limit: 1000,
        available_limit: 500,
        due_day: 10,
      });

      const purchaseId = randomUUID();
      await db("credit_purchases").insert({
        id: purchaseId,
        user_id: userId,
        card_id: cardId,
        category_id: defaultCategoryId,
        title: "Compra Errada",
        store: "Loja",
        total_amount: 500,
        total_installments: 2,
        purchase_date: "2026-06-30",
      });

      await db("installments").insert({
        id: randomUUID(),
        user_id: userId,
        purchase_id: purchaseId,
        installment_number: 1,
        total_installments: 2,
        amount: 250,
        expected_date: "2026-07-10",
        status: "paid",
      });

      await db("installments").insert({
        id: randomUUID(),
        user_id: userId,
        purchase_id: purchaseId,
        installment_number: 2,
        total_installments: 2,
        amount: 250,
        expected_date: "2026-08-10",
        status: "pending",
      });

      const response = await app.inject({
        method: "PATCH",
        url: `/credit/purchases/${purchaseId}/cancel`,
        headers: { Authorization: `Bearer ${token}` },
      });

      expect(response.statusCode).toBe(204);

      const cancelledPurchase = await db("credit_purchases")
        .where({ id: purchaseId })
        .first();
      expect(cancelledPurchase?.status).toBe("cancelled");

      const card = await db("cards").where({ id: cardId }).first();
      expect(Number(card?.available_limit)).toBe(750);
    });
  });
});
