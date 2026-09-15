import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { randomUUID } from "node:crypto";

import { app } from "../app.js";
import { db } from "../database/database.js";
import { getInstallmentDueDate } from "../utils/installment-dates.js";

describe.sequential("Sprint 3 - consistência estrutural", () => {
  let userId: string;
  let otherUserId: string;
  let categoryId: string;
  let otherCategoryId: string;
  let token: string;

  beforeEach(async () => {
    userId = randomUUID();
    otherUserId = randomUUID();
    categoryId = randomUUID();
    otherCategoryId = randomUUID();

    await db("users").insert([
      {
        id: userId,
        name: "Sprint 3",
        email: `sprint3-${userId}@fluxa.test`,
        password_hash: "hash",
        email_verified_at: new Date(),
      },
      {
        id: otherUserId,
        name: "Outro usuário",
        email: `sprint3-${otherUserId}@fluxa.test`,
        password_hash: "hash",
        email_verified_at: new Date(),
      },
    ]);
    await db("categories").insert([
      {
        id: categoryId,
        user_id: userId,
        name: "Categoria própria",
        type: "expense",
      },
      {
        id: otherCategoryId,
        user_id: otherUserId,
        name: "Categoria alheia",
        type: "expense",
      },
    ]);
    token = app.jwt.sign({
      sub: userId,
      type: "access",
      tokenVersion: 0,
    });
  });

  afterEach(async () => {
    await db("users").whereIn("id", [userId, otherUserId]).delete();
  });

  it("atualiza apenas categoria pertencente ao usuário", async () => {
    const own = await app.inject({
      method: "PUT",
      url: `/categories/${categoryId}`,
      headers: { authorization: `Bearer ${token}` },
      payload: { name: "Categoria atualizada", type: "income" },
    });
    const foreign = await app.inject({
      method: "PUT",
      url: `/categories/${otherCategoryId}`,
      headers: { authorization: `Bearer ${token}` },
      payload: { name: "Tentativa indevida" },
    });

    expect(own.statusCode).toBe(204);
    expect(foreign.statusCode).toBe(404);
    const updated = await db("categories").where({ id: categoryId }).first();
    const untouched = await db("categories")
      .where({ id: otherCategoryId })
      .first();
    expect(updated).toMatchObject({
      name: "Categoria atualizada",
      type: "income",
    });
    expect(untouched?.name).toBe("Categoria alheia");
  });

  it("exclui categoria livre e bloqueia categoria em uso", async () => {
    const subscriptionId = randomUUID();
    await db("subscriptions").insert({
      id: subscriptionId,
      user_id: userId,
      category_id: categoryId,
      title: "Assinatura",
      amount: 10,
      due_day: 10,
      frequency: "monthly",
      status: "active",
    });

    const inUse = await app.inject({
      method: "DELETE",
      url: `/categories/${categoryId}`,
      headers: { authorization: `Bearer ${token}` },
    });
    const foreign = await app.inject({
      method: "DELETE",
      url: `/categories/${otherCategoryId}`,
      headers: { authorization: `Bearer ${token}` },
    });
    const free = await app.inject({
      method: "DELETE",
      url: `/categories/${otherCategoryId}`,
      headers: {
        authorization: `Bearer ${app.jwt.sign({
          sub: otherUserId,
          type: "access",
          tokenVersion: 0,
        })}`,
      },
    });

    expect(inUse.statusCode).toBe(409);
    expect(foreign.statusCode).toBe(404);
    expect(free.statusCode).toBe(204);
  });

  it("exige category_id e rejeita o status inexistente deleted", async () => {
    const accountId = randomUUID();
    await db("accounts").insert({
      id: accountId,
      user_id: userId,
      name: "Conta",
      type: "checking",
    });
    const basePayload = {
      title: "Assinatura",
      amount: 10,
      due_day: 10,
      frequency: "monthly",
      account_id: accountId,
    };

    const missingCategory = await app.inject({
      method: "POST",
      url: "/subscriptions",
      headers: { authorization: `Bearer ${token}` },
      payload: basePayload,
    });
    const deletedStatus = await app.inject({
      method: "POST",
      url: "/subscriptions",
      headers: { authorization: `Bearer ${token}` },
      payload: { ...basePayload, category_id: categoryId, status: "deleted" },
    });

    expect(missingCategory.statusCode).toBe(400);
    expect(deletedStatus.statusCode).toBe(400);
  });

  it("cria e exclui fisicamente uma assinatura no contrato suportado", async () => {
    const accountId = randomUUID();
    await db("accounts").insert({
      id: accountId,
      user_id: userId,
      name: "Conta",
      type: "checking",
    });
    const created = await app.inject({
      method: "POST",
      url: "/subscriptions",
      headers: { authorization: `Bearer ${token}` },
      payload: {
        title: "Assinatura válida",
        amount: 10,
        due_day: 10,
        frequency: "monthly",
        account_id: accountId,
        category_id: categoryId,
      },
    });
    const subscription = await db("subscriptions")
      .where({ user_id: userId, title: "Assinatura válida" })
      .first();
    const deleted = await app.inject({
      method: "DELETE",
      url: `/subscriptions/${subscription.id}`,
      headers: { authorization: `Bearer ${token}` },
    });

    expect(created.statusCode).toBe(201);
    expect(deleted.statusCode).toBe(204);
    expect(
      await db("subscriptions").where({ id: subscription.id }).first(),
    ).toBeUndefined();
  });

  it("limita vencimentos 28-31 ao último dia válido de cada mês", () => {
    expect(getInstallmentDueDate("2027-01-15", 1, 28)).toBe("2027-02-28");
    expect(getInstallmentDueDate("2027-01-15", 1, 29)).toBe("2027-02-28");
    expect(getInstallmentDueDate("2027-01-15", 1, 30)).toBe("2027-02-28");
    expect(getInstallmentDueDate("2027-01-15", 1, 31)).toBe("2027-02-28");
    expect(getInstallmentDueDate("2028-01-15", 1, 29)).toBe("2028-02-29");
    expect(getInstallmentDueDate("2028-01-15", 1, 31)).toBe("2028-02-29");
    expect(getInstallmentDueDate("2027-03-15", 1, 31)).toBe("2027-04-30");
    expect(getInstallmentDueDate("2027-04-15", 1, 31)).toBe("2027-05-31");
  });
});
