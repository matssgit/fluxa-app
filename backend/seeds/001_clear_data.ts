import { Knex } from "knex";

export async function seed(knex: Knex): Promise<void> {
  console.log("🔥 LIMPANDO DADOS (Reset Real)...");

  // Ordem de deleção respeitando FKs
  await knex("wallet_history").del();
  await knex("installments").del();
  await knex("credit_purchases").del();
  await knex("transactions").del();
  await knex("subscriptions").del();
  await knex("cards").del();
  await knex("wallets").del();
  await knex("categories").del();
  await knex("accounts").del();
  await knex("users").del();

  console.log("✅ Banco limpo!");
}
