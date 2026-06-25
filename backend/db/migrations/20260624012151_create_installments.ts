import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("installments", (table) => {
      table.uuid("id").primary();
      table.uuid("user_id").notNullable();
      table
         .uuid("purchase_id")
         .notNullable()
         .references("id")
         .inTable("credit_purchases");
      table.integer("installment_number").notNullable();
      table.integer("total_installments").notNullable();
      table.decimal("amount", 10, 2).notNullable();
      table.date("expected_date").notNullable();
      table.date("completed_date");
      table.enum("status", ["pending", "paid"]).defaultTo("pending");
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.dropTable("installments");
}
