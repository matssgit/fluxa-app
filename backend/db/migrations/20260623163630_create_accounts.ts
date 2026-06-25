import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.createTable("accounts", (table) => {
      table.uuid("id").primary();
      table.string("name").notNullable(); // Ex: "Nubank", "Carteira", "Itaú"

      // Isolamento: liga a conta ao usuário logado
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
   await knex.schema.dropTable("accounts");
}
