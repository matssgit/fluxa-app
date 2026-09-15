import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("transactions", (table) => {
    table.string("competence", 7).nullable();
  });

  await knex.raw(
    "CREATE UNIQUE INDEX transactions_subscription_competence_unique " +
      "ON transactions (subscription_id, competence) " +
      "WHERE subscription_id IS NOT NULL AND competence IS NOT NULL",
  );
}

export async function down(knex: Knex): Promise<void> {
  await knex.raw(
    "DROP INDEX IF EXISTS transactions_subscription_competence_unique",
  );

  await knex.schema.alterTable("transactions", (table) => {
    table.dropColumn("competence");
  });
}
