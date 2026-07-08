import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable("wallets", (table) => {
    table.uuid("id").primary();
    table
      .uuid("user_id")
      .references("id")
      .inTable("users")
      .onDelete("CASCADE")
      .notNullable()
      .index();

    table.string("title").notNullable();
    table.string("description").nullable();
    table.decimal("target_amount", 12, 2).notNullable();
    table.decimal("current_amount", 12, 2).defaultTo(0).notNullable();
    table.date("deadline").nullable();
    table.string("color").defaultTo("brand");

    // Status do cultivo: 'active' (em crescimento), 'completed' (pronta para colheita), 'paused' (em repouso)
    table.string("status").defaultTo("active").notNullable();

    table.timestamps(true, true);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTable("wallets");
}
