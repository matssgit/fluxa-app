import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("transactions", (table) => {
    table.index(["user_id"]);
    table.index(["account_id"]);
    table.index(["category_id"]);
  });

  await knex.schema.alterTable("cards", (table) => {
    table.index(["user_id"]);
  });

  await knex.schema.alterTable("credit_purchases", (table) => {
    table.index(["user_id"]);
    table.index(["card_id"]);
  });

  await knex.schema.alterTable("installments", (table) => {
    table.index(["purchase_id"]);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("transactions", (table) => {
    table.dropIndex(["user_id"]);
    table.dropIndex(["account_id"]);
    table.dropIndex(["category_id"]);
  });

  await knex.schema.alterTable("cards", (table) => {
    table.dropIndex(["user_id"]);
  });

  await knex.schema.alterTable("credit_purchases", (table) => {
    table.dropIndex(["user_id"]);
    table.dropIndex(["card_id"]);
  });

  await knex.schema.alterTable("installments", (table) => {
    table.dropIndex(["purchase_id"]);
  });
}
