import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("transactions", (table) => {
    table
      .uuid("subscription_id")
      .references("id")
      .inTable("subscriptions")
      .onDelete("SET NULL")
      .nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("transactions", (table) => {
    table.dropColumn("subscription_id");
  });
}
