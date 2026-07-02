import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   await knex.schema.alterTable("cards", (table) => {
      // Usaremos 'slate' (cinza) como cor padrão para não quebrar os cartões antigos
      table.string("color", 50).defaultTo("slate").notNullable();
   });
}

export async function down(knex: Knex): Promise<void> {
   await knex.schema.alterTable("cards", (table) => {
      table.dropColumn("color");
   });
}
