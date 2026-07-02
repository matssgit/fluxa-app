import { app } from "./app.js"; // ou como quer que esteja a sua importação
import { db } from "./database.js"; // Adicione esta importação lá no topo

const start = async () => {
   try {
      console.log("⏳ Sincronizando banco de dados...");
      await db.migrate.latest();

      // ==========================================
      // O "TRATOR": Forçando a correção das colunas
      // ==========================================
      const hasAvailableLimit = await db.schema.hasColumn(
         "cards",
         "available_limit",
      );

      if (!hasAvailableLimit) {
         console.log(
            "🛠️ Banco inconsistente detectado! Forçando a criação das colunas...",
         );

         await db.schema.alterTable("cards", (table) => {
            table.decimal("total_limit", 10, 2).defaultTo(0);
            table.decimal("available_limit", 10, 2).defaultTo(0);
         });

         const hasOldLimit = await db.schema.hasColumn("cards", "limit_amount");
         if (hasOldLimit) {
            console.log("♻️ Resgatando dinheiro dos testes antigos...");
            await db.raw(
               "UPDATE cards SET total_limit = limit_amount, available_limit = limit_amount",
            );

            console.log("🗑️ Apagando coluna velha...");
            await db.schema.alterTable("cards", (table) => {
               table.dropColumn("limit_amount");
            });
         }
         console.log("✅ Colunas corrigidas com sucesso!");
      } else {
         console.log("✅ Estrutura de cartões está perfeita!");
      }
      // ==========================================

      await app.listen({ port: 3333, host: "0.0.0.0" });
      console.log("🚀 Servidor rodando na porta 3333");
   } catch (err) {
      console.error(err);
      process.exit(1);
   }
};

start();
