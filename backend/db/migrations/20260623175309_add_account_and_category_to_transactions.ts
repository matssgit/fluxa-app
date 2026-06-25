import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      table
         .uuid("account_id")
         .references("id")
         .inTable("accounts")
         .onDelete("SET NULL")
         .nullable();
      table
         .uuid("category_id")
         .references("id")
         .inTable("categories")
         .onDelete("SET NULL")
         .nullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      table.dropColumn("account_id");
      table.dropColumn("category_id");
   });
}
