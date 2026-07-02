import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("subscriptions", (table) => {
      table.uuid("id").primary();

      // Relacionamentos Fortes
      table
         .uuid("user_id")
         .references("id")
         .inTable("users")
         .onDelete("CASCADE")
         .notNullable();
      table
         .uuid("category_id")
         .references("id")
         .inTable("categories")
         .onDelete("RESTRICT")
         .notNullable();

      // Onde a cobrança acontece
      table
         .uuid("account_id")
         .references("id")
         .inTable("accounts")
         .onDelete("RESTRICT")
         .nullable();
      table
         .uuid("card_id")
         .references("id")
         .inTable("cards")
         .onDelete("RESTRICT")
         .nullable();

      // Dados da Assinatura
      table.string("title").notNullable();
      table.decimal("amount", 10, 2).notNullable();
      table.integer("due_day").notNullable();
      table
         .enum("frequency", ["monthly", "yearly"])
         .defaultTo("monthly")
         .notNullable();

      // Controle de Estado
      table
         .enum("status", ["active", "paused", "cancelled"])
         .defaultTo("active")
         .notNullable();

      table.timestamp("created_at").defaultTo(knex.fn.now());
      table.timestamp("updated_at").defaultTo(knex.fn.now());
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.dropTable("subscriptions");
}
