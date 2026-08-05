import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  const hasEnabled = await knex.schema.hasColumn("users", "two_factor_enabled");
  const hasSecret = await knex.schema.hasColumn("users", "two_factor_secret");
  const hasRecovery = await knex.schema.hasColumn("users", "recovery_codes");
  const hasTotpStep = await knex.schema.hasColumn("users", "last_totp_step");

  await knex.schema.alterTable("users", (table) => {
    if (!hasEnabled) table.boolean("two_factor_enabled").defaultTo(false);
    if (!hasSecret) table.string("two_factor_secret").nullable();
    if (!hasRecovery) table.text("recovery_codes").nullable();
    if (!hasTotpStep) table.integer("last_totp_step").nullable();
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.dropColumn("two_factor_enabled");
    table.dropColumn("two_factor_secret");
    table.dropColumn("recovery_codes");
    table.dropColumn("last_totp_step");
  });
}
