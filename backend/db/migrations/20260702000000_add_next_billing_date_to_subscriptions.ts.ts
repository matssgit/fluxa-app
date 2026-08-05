import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  // Verifica se a coluna já existe antes de tentar criar
  const hasColumn = await knex.schema.hasColumn(
    "subscriptions",
    "next_billing_date",
  );

  if (!hasColumn) {
    return knex.schema.alterTable("subscriptions", (table) => {
      table.date("next_billing_date").nullable();
    });
  }
}

export async function down(knex: Knex): Promise<void> {
  // Verifica se a coluna existe antes de tentar dropar
  const hasColumn = await knex.schema.hasColumn(
    "subscriptions",
    "next_billing_date",
  );

  if (hasColumn) {
    return knex.schema.alterTable("subscriptions", (table) => {
      table.dropColumn("next_billing_date");
    });
  }
}
