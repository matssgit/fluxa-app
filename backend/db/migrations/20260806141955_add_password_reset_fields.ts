import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.text("password_reset_token").nullable();
    table.timestamp("password_reset_expires_at").nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.dropColumn("password_reset_token");
    table.dropColumn("password_reset_expires_at");
  });
}
