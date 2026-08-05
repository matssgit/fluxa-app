import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable("wallet_history", (table) => {
    table.uuid("id").primary();
    table
      .uuid("wallet_id")
      .references("id")
      .inTable("wallets")
      .onDelete("CASCADE");
    table.enum("type", ["deposit", "withdraw"]).notNullable();
    table.decimal("amount", 10, 2).notNullable();
    table.text("observation").nullable();
    table.timestamp("created_at").defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable("wallet_history");
}
