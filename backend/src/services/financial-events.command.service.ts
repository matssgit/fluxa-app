import { db as knex } from "../database/database.js";
import { payInstallment } from "./installment-payments.service.js";
import { paySubscription } from "./subscription-payments.service.js";

export class FinancialEventCommandService {
  async markAsPaid(
    eventId: string,
    userId: string,
    accountId?: string,
  ): Promise<void> {
    const eventMeta = await this.resolveEventType(eventId, userId);

    if (!eventMeta) {
      throw new Error("Lançamento financeiro não encontrado ou acesso negado.");
    }

    switch (eventMeta.type) {
      case "transaction":
        await knex.transaction(async (trx) => {
          const transaction = await trx("transactions")
            .where({ id: eventId, user_id: userId })
            .forUpdate()
            .first();
          if (!transaction) {
            throw new Error(
              "Lançamento financeiro não encontrado ou acesso negado.",
            );
          }
          if (transaction.status === "completed") return;

          await trx("transactions")
            .where({ id: eventId, user_id: userId })
            .update({
              status: "completed",
              completed_date: new Date().toISOString().split("T")[0],
            });
        });
        return;

      case "installment":
        if (!accountId) {
          throw new Error("Selecione uma conta para pagar esta parcela.");
        }
        await payInstallment({
          installmentId: eventId,
          userId,
          accountId,
        });
        return;

      case "subscription":
        await paySubscription({
          subscriptionId: eventId,
          userId,
          ...(accountId ? { accountId } : {}),
        });
        return;

      default:
        throw new Error("Tipo de evento não suportado para esta operação.");
    }
  }

  private async resolveEventType(
    eventId: string,
    userId: string,
  ): Promise<{ type: string } | null> {
    const isTx = await knex("transactions")
      .select("id")
      .where({ id: eventId, user_id: userId })
      .first();
    if (isTx) return { type: "transaction" };

    const isInst = await knex("installments as i")
      .join("credit_purchases as p", "i.purchase_id", "p.id")
      .select("i.id")
      .where("i.id", eventId)
      .andWhere("p.user_id", userId)
      .first();
    if (isInst) return { type: "installment" };

    const isSub = await knex("subscriptions")
      .select("id")
      .where({ id: eventId, user_id: userId })
      .first();
    if (isSub) return { type: "subscription" };

    return null;
  }
}
