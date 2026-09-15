import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.integer("token_version").notNullable().defaultTo(0);
  });

  await knex.schema.alterTable("users", (table) => {
    table.renameColumn("password_reset_token", "password_reset_token_hash");
  });

  // Tokens previously persisted in this column were plaintext. Invalidate them
  // instead of relabeling sensitive legacy values as hashes.
  await knex("users").update({
    password_reset_token_hash: null,
    password_reset_expires_at: null,
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.renameColumn("password_reset_token_hash", "password_reset_token");
  });

  await knex.schema.alterTable("users", (table) => {
    table.dropColumn("token_version");
  });
}
