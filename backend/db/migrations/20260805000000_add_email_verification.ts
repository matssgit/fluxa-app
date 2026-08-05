import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.timestamp("email_verified_at").nullable().defaultTo(null);
  });

  await knex.schema.createTable("email_verification_tokens", (table) => {
    table.uuid("id").primary();
    table
      .uuid("user_id")
      .notNullable()
      .references("id")
      .inTable("users")
      .onDelete("CASCADE");
    table.string("token_hash").notNullable().unique();
    table.timestamp("expires_at").notNullable();
    table.timestamp("created_at").defaultTo(knex.fn.now());
    table.timestamp("used_at").nullable().defaultTo(null);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists("email_verification_tokens");
  await knex.schema.alterTable("users", (table) => {
    table.dropColumn("email_verified_at");
  });
}
