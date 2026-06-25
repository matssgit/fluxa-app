import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("categories", (table) => {
      table.boolean("is_default").defaultTo(false);
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("categories", (table) => {
      table.dropColumn("is_default");
   });
}
