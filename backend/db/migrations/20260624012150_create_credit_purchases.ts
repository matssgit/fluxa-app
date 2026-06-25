import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("credit_purchases", (table) => {
      table.uuid("id").primary();
      table.uuid("user_id").notNullable();
      table.uuid("card_id").notNullable().references("id").inTable("cards");
      table.uuid("category_id").notNullable();
      table.string("title").notNullable();
      table.string("store").notNullable();
      table.string("observation");
      table.decimal("total_amount", 10, 2).notNullable();
      table.integer("total_installments").notNullable();
      table.date("purchase_date").notNullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.dropTable("credit_purchases");
}
