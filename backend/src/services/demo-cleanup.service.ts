import { db } from "../database/database.js";
import { env } from "../env/index.js";

export class DemoCleanupService {
  static async runCleanup() {
    if (!env.DEMO_CLEANUP_ENABLED) {
      return;
    }

    console.log(
      "[DEMO CLEANUP] Iniciando rotina de limpeza de dados expirados...",
    );

    try {
      const expirationDate = new Date();
      expirationDate.setDate(
        expirationDate.getDate() - env.DEMO_DATA_RETENTION_DAYS,
      );

      await db.transaction(async (trx) => {
        const usersToDelete = await trx("users")
          .where("created_at", "<=", expirationDate)
          .select("id");

        if (usersToDelete.length === 0) {
          console.log("[DEMO CLEANUP] Nenhum usuário expirado encontrado.");
          return;
        }

        const userIds = usersToDelete.map((u) => u.id);
        console.log(
          `[DEMO CLEANUP] ${userIds.length} usuário(s) encontrado(s) para remoção.`,
        );

        await trx("transactions").whereIn("user_id", userIds).del();
        await trx("subscriptions").whereIn("user_id", userIds).del();

        const wallets = await trx("wallets")
          .whereIn("user_id", userIds)
          .select("id");
        if (wallets.length > 0) {
          const walletIds = wallets.map((w) => w.id);
          await trx("wallet_history").whereIn("wallet_id", walletIds).del();
          await trx("wallets").whereIn("id", walletIds).del();
        }

        const cards = await trx("cards")
          .whereIn("user_id", userIds)
          .select("id");
        if (cards.length > 0) {
          const cardIds = cards.map((c) => c.id);
          const purchases = await trx("credit_purchases")
            .whereIn("card_id", cardIds)
            .select("id");

          if (purchases.length > 0) {
            const purchaseIds = purchases.map((p) => p.id);
            await trx("installments").whereIn("purchase_id", purchaseIds).del();
            await trx("credit_purchases").whereIn("id", purchaseIds).del();
          }
          await trx("cards").whereIn("id", cardIds).del();
        }

        await trx("categories").whereIn("user_id", userIds).del();
        await trx("accounts").whereIn("user_id", userIds).del();

        const hasTokensTable = await trx.schema.hasTable(
          "email_verification_tokens",
        );
        if (hasTokensTable) {
          await trx("email_verification_tokens")
            .whereIn("user_id", userIds)
            .del();
        }

        await trx("users").whereIn("id", userIds).del();

        console.log(
          `[DEMO CLEANUP] Limpeza concluída. ${userIds.length} conta(s) removida(s).`,
        );
      });
    } catch (error) {
      console.error(
        "[DEMO CLEANUP] ERRO CRÍTICO durante a limpeza. Operação cancelada (Rollback).",
      );
      console.error(error);
    }
  }
}
