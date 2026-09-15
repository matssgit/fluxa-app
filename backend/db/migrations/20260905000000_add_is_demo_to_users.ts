import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.boolean("is_demo").notNullable().defaultTo(false);
    table.index(["is_demo", "created_at"], "users_is_demo_created_at_index");
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.dropIndex(["is_demo", "created_at"], "users_is_demo_created_at_index");
    table.dropColumn("is_demo");
  });
}
