import type { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      // Adicionado a coluna type como texto (aceita 'entrada' ou 'saida')
      table.string("type").nullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("transactions", (table) => {
      table.dropColumn("type");
   });
}
