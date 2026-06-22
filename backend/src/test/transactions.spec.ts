import { expect, it, beforeAll, afterAll, describe, beforeEach } from "vitest";
import { execSync } from "node:child_process";
import request from "supertest";
import { app } from "../app.js";

describe("Transactions routes", () => {
   // Antes de rodar qualquer teste, aguardo a aplicação (Fastify) terminar de inicializar
   beforeAll(async () => {
      await app.ready();
   });

   // Após todos os testes finalizarem, fecho a aplicação para não deixar processos pendentes
   afterAll(async () => {
      await app.close();
   });

   /**
    * Para garantir testes isolados (E2E) e confiáveis, limpo o banco de dados
    * e rodo as migrations do zero antes de cada teste. Isso evita que o estado
    * de um teste interfira no resultado do outro.
    */
   beforeEach(() => {
      execSync("npm run knex migrate:rollback --all");
      execSync("npm run knex migrate:latest");
   });

   it("should be able to create a new transaction", async () => {
      // Faço a chamada HTTP p/ criar uma nova transação e espero um status 201 (Created)
      await request(app.server)
         .post("/transactions")
         .send({
            title: "New transaction",
            amount: 5000,
            type: "credit",
         })
         .expect(201);
   });

   it("should be able to list all transactions", async () => {
      const createTransactionResponse = await request(app.server)
         .post("/transactions")
         .send({
            title: "New transaction",
            amount: 5000,
            type: "credit",
         });

      // Capturo o cookie gerado na criação para simular a sessão do usuário
      const cookies = createTransactionResponse.get("Set-Cookie") ?? [];

      const listTransactionsReponse = await request(app.server)
         .get("/transactions")
         .set("Cookie", cookies)
         .expect(200);

      // Verifico se a listagem retorna um array contendo o objeto criado
      expect(listTransactionsReponse.body.transactions).toEqual([
         expect.objectContaining({
            title: "New transaction",
            amount: 5000,
         }),
      ]);
   });

   it("should be able to get a specific transaction", async () => {
      const createTransactionResponse = await request(app.server)
         .post("/transactions")
         .send({
            title: "New transaction",
            amount: 5000,
            type: "credit",
         });

      const cookies = createTransactionResponse.get("Set-Cookie") ?? [];

      const listTransactionsReponse = await request(app.server)
         .get("/transactions")
         .set("Cookie", cookies)
         .expect(200);

      const transactionId = listTransactionsReponse.body.transactions[0].id;

      const getTransactionsReponse = await request(app.server)
         .get(`/transactions/${transactionId}`)
         .set("Cookie", cookies)
         .expect(200);

      // Garanto que a busca por ID traz exatamente a transação correta
      expect(getTransactionsReponse.body.transaction).toEqual(
         expect.objectContaining({
            title: "New transaction",
            amount: 5000,
         }),
      );
   });

   it("should be able to get the summary", async () => {
      const createTransactionResponse = await request(app.server)
         .post("/transactions")
         .send({
            title: "Credit transaction",
            amount: 5000,
            type: "credit",
         });

      const cookies = createTransactionResponse.get("Set-Cookie") ?? [];

      // Crio uma segunda transação (débito) usando a mesma sessão
      await request(app.server)
         .post("/transactions")
         .set("Cookie", cookies)
         .send({
            title: "Debit transaction",
            amount: 2000,
            type: "debit",
         });

      const summaryResponse = await request(app.server)
         .get("/transactions/summary")
         .set("Cookie", cookies)
         .expect(200);

      // O resumo deve ser a soma exata: 5000 (crédito) - 2000 (débito) = 3000
      expect(summaryResponse.body.summary).toEqual({
         amount: 3000,
      });
   });
});
