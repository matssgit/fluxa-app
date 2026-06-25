import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("categories", (table) => {
      table.uuid("id").primary();
      table.string("name").notNullable(); // Ex: "Supermercado", "Salário"
      table.string("type").notNullable(); // Vai receber 'income' ou 'expense'

      // Novamente, isolando os dados por usuário
      table
         .uuid("user_id")
         .references("id")
         .inTable("users")
         .onDelete("CASCADE")
         .notNullable();

      table.timestamp("created_at").defaultTo(knex.fn.now()).notNullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.dropTable("categories");
}
