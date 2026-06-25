import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   // 1. Atualizando Contas
   await knex.schema.alterTable("accounts", (table) => {
      table.string("type").notNullable().defaultTo("checking"); // ex: checking, wallet, savings
   });

   // 2. Atualizando Categorias
   await knex.schema.alterTable("categories", (table) => {
      table.string("color").nullable();
      table.string("icon").nullable();
   });

   // 3. Atualizando Transações (Preparando para o fluxo de caixa real)
   await knex.schema.alterTable("transactions", (table) => {
      table.string("description").nullable();
      table.string("observation").nullable();
      table.string("status").notNullable().defaultTo("completed"); // pending ou completed
      table.date("expected_date").nullable();
      table.date("completed_date").nullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("accounts", (table) => {
      table.dropColumn("type");
   });
   await knex.schema.alterTable("categories", (table) => {
      table.dropColumn("color");
      table.dropColumn("icon");
   });
   await knex.schema.alterTable("transactions", (table) => {
      table.dropColumn("description");
      table.dropColumn("observation");
      table.dropColumn("status");
      table.dropColumn("expected_date");
      table.dropColumn("completed_date");
   });
}
