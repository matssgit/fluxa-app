import { beforeAll, afterAll, beforeEach } from "vitest";
import { app } from "../app.js";
import { db } from "../database.js";

// Antes de todos os testes, inicializa os plugins do Fastify
beforeAll(async () => {
   await app.ready();
});

// Ao final, encerra o servidor e as conexões do Knex
afterAll(async () => {
   await app.close();
   await db.destroy();
});

// Antes de CADA teste (it), apaga APENAS as tabelas de domínio.
// Não apagamos users e categories, pois eles são criados no beforeAll
// e devem persistir por toda a suíte de testes.
beforeEach(async () => {
   await db("transactions").del();
   await db("installments").del();
   await db("credit_purchases").del();
   await db("cards").del();
   await db("accounts").del();
});
