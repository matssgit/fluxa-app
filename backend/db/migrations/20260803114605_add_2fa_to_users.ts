import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.boolean("two_factor_enabled").defaultTo(false).notNullable();
    table.string("two_factor_secret").nullable();
    table.jsonb("recovery_codes").nullable();
    table.bigInteger("last_totp_step").nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.dropColumns(
      "two_factor_enabled",
      "two_factor_secret",
      "recovery_codes",
      "last_totp_step",
    );
  });
}
