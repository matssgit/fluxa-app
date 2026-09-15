import { randomUUID } from "node:crypto";
import { db as knex } from "../database/database.js";

export type SubscriptionPaymentErrorCode =
  | "SUBSCRIPTION_NOT_FOUND"
  | "SUBSCRIPTION_INACTIVE"
  | "ACCOUNT_REQUIRED"
  | "ACCOUNT_NOT_FOUND"
  | "ALREADY_PAID";

export class SubscriptionPaymentError extends Error {
  constructor(public readonly code: SubscriptionPaymentErrorCode) {
    super(code);
  }
}

export function currentCompetence(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}

function isUniqueViolation(error: unknown): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    error.code === "23505"
  );
}

export async function paySubscription(input: {
  subscriptionId: string;
  userId: string;
  accountId?: string;
  competence?: string;
}): Promise<void> {
  const competence = input.competence || currentCompetence();

  try {
    await knex.transaction(async (trx) => {
      const subscription = await trx("subscriptions")
        .where({ id: input.subscriptionId, user_id: input.userId })
        .forUpdate()
        .first();

      if (!subscription) {
        throw new SubscriptionPaymentError("SUBSCRIPTION_NOT_FOUND");
      }
      if (subscription.status !== "active") {
        throw new SubscriptionPaymentError("SUBSCRIPTION_INACTIVE");
      }

      const accountId = input.accountId || subscription.account_id;
      if (!accountId) {
        throw new SubscriptionPaymentError("ACCOUNT_REQUIRED");
      }

      const account = await trx("accounts")
        .where({ id: accountId, user_id: input.userId })
        .first();
      if (!account) {
        throw new SubscriptionPaymentError("ACCOUNT_NOT_FOUND");
      }

      const alreadyPaid = await trx("transactions")
        .where({
          subscription_id: subscription.id,
          competence,
        })
        .first();
      if (alreadyPaid) {
        throw new SubscriptionPaymentError("ALREADY_PAID");
      }

      const today = new Date().toISOString().split("T")[0];

      await trx("transactions").insert({
        id: randomUUID(),
        user_id: input.userId,
        account_id: accountId,
        category_id: subscription.category_id,
        subscription_id: subscription.id,
        competence,
        title: "Pagamento: " + subscription.title,
        description: "Assinatura",
        amount: Math.abs(Number(subscription.amount)),
        type: "saida",
        status: "completed",
        expected_date: today,
        completed_date: today,
      });
    });
  } catch (error) {
    if (isUniqueViolation(error)) {
      throw new SubscriptionPaymentError("ALREADY_PAID");
    }
    throw error;
  }
}
