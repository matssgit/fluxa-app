import type { Knex } from "knex";

export async function seed(knex: Knex): Promise<void> {
   console.log("🔥 LIMPANDO DADOS...");

   // A ordem é importante devido às chaves estrangeiras (Foreign Keys)
   await knex("installments").del();
   await knex("credit_purchases").del();
   await knex("transactions").del();
   await knex("accounts").del(); // Limpa as contas também para garantir

   console.log("✅ Dados limpos!");
}
