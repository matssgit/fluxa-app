import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("credit_purchases", (table) => {
      table.string("status", 20).defaultTo("active").notNullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("credit_purchases", (table) => {
      table.dropColumn("status");
   });
}
