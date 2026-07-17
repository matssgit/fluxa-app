import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  return knex.schema.alterTable("subscriptions", (table) => {
    // Adicionamos a coluna como nullable() porque as assinaturas
    // antigas (se existirem) não terão esse dado preenchido
    table.date("next_billing_date").nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  return knex.schema.alterTable("subscriptions", (table) => {
    table.dropColumn("next_billing_date");
  });
}
