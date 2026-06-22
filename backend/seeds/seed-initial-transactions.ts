import { Knex } from "knex";

export async function seed(knex: Knex): Promise<void> {
   // Deleta TODAS as transações existentes
   await knex("transactions").del();

   // Insere dados fictícios para o seu portfólio não ficar vazio
   await knex("transactions").insert([
      {
         id: "p1",
         title: "Salário",
         amount: 5000.0,
         type: "credit",
         session_id: "portfolio-demo-session",
      },
      {
         id: "p2",
         title: "Aluguel",
         amount: -1200.0,
         type: "debit",
         session_id: "portfolio-demo-session",
      },
      {
         id: "p3",
         title: "Supermercado",
         amount: -450.0,
         type: "debit",
         session_id: "portfolio-demo-session",
      },
   ]);
}
