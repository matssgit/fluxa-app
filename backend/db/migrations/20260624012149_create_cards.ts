import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("cards", (table) => {
      table.uuid("id").primary();
      table.uuid("user_id").notNullable().references("id").inTable("users");
      table.string("name").notNullable();
      table.string("brand").notNullable();
      table.decimal("limit_amount", 10, 2).notNullable();
      table.integer("due_day").notNullable();
      table.timestamp("created_at").defaultTo(knex.fn.now());
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.dropTable("cards");
}
