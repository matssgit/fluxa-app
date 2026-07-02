import { Knex } from "knex";

export async function up(knex: Knex): Promise<void> {
   // 1. Remove a trava de segurança antiga
   await knex.raw(
      "ALTER TABLE installments DROP CONSTRAINT installments_status_check",
   );

   // 2. Cria a nova trava permitindo o 'cancelled'
   await knex.raw(
      `ALTER TABLE installments ADD CONSTRAINT installments_status_check CHECK (status IN ('pending', 'paid', 'cancelled'))`,
   );
}

export async function down(knex: Knex): Promise<void> {
   // Caso precise desfazer (rollback)
   await knex.raw(
      "ALTER TABLE installments DROP CONSTRAINT installments_status_check",
   );
   await knex.raw(
      `ALTER TABLE installments ADD CONSTRAINT installments_status_check CHECK (status IN ('pending', 'paid'))`,
   );
}
