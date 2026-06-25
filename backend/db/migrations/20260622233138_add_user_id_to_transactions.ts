import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      table
         .uuid("user_id")
         .references("id")
         .inTable("users")
         .onDelete("CASCADE")
         .nullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      table.dropColumn("user_id");
   });
}
