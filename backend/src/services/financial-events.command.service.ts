import { db as knex } from "../database/database.js";

export class FinancialEventCommandService {
  async markAsPaid(eventId: string, userId: string): Promise<void> {
    const eventMeta = await this.resolveEventType(eventId, userId);

    if (!eventMeta) {
      throw new Error("Lançamento financeiro não encontrado ou acesso negado.");
    }

    await knex.transaction(async (trx) => {
      switch (eventMeta.type) {
        case "transaction":
          await trx("transactions")
            .where({ id: eventId })
            .update({ status: "completed", updated_at: knex.fn.now() });
          break;

        case "installment":
          await trx("installments")
            .where({ id: eventId })
            .update({ status: "completed", updated_at: knex.fn.now() });
          break;

        case "subscription":
          await trx("subscriptions")
            .where({ id: eventId })
            .update({ status: "completed", updated_at: knex.fn.now() });
          break;

        default:
          throw new Error("Tipo de evento não suportado para esta operação.");
      }
    });
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
