import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   // 1. Adiciona as novas colunas
   await knex.schema.alterTable("cards", (table) => {
      table.decimal("total_limit", 10, 2).defaultTo(0);
      table.decimal("available_limit", 10, 2).defaultTo(0);
   });

   // 2. Copia os dados da coluna antiga para as novas (preserva seus testes)
   await knex.raw(
      "UPDATE cards SET total_limit = limit_amount, available_limit = limit_amount",
   );

   // 3. Remove a coluna antiga
   await knex.schema.alterTable("cards", (table) => {
      table.dropColumn("limit_amount");
   });
}

export async function down(knex: Knex): Promise<void> {
   // Caso precise desfazer (rollback)
   await knex.schema.alterTable("cards", (table) => {
      table.decimal("limit_amount", 10, 2).defaultTo(0);
   });

   await knex.raw("UPDATE cards SET limit_amount = total_limit");

   await knex.schema.alterTable("cards", (table) => {
      table.dropColumn("total_limit");
      table.dropColumn("available_limit");
   });
}
