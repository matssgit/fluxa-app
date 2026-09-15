import { randomUUID } from "node:crypto";
import { db as knex } from "../database/database.js";

export type InstallmentPaymentErrorCode =
  | "INSTALLMENT_NOT_FOUND"
  | "PURCHASE_NOT_FOUND"
  | "PURCHASE_CANCELLED"
  | "INSTALLMENT_CANCELLED"
  | "ALREADY_PAID"
  | "ACCOUNT_NOT_FOUND"
  | "CARD_NOT_FOUND";

export class InstallmentPaymentError extends Error {
  constructor(public readonly code: InstallmentPaymentErrorCode) {
    super(code);
  }
}

export async function payInstallment(input: {
  installmentId: string;
  userId: string;
  accountId: string;
}): Promise<void> {
  const reference = await knex("installments as i")
    .join("credit_purchases as p", "i.purchase_id", "p.id")
    .where("i.id", input.installmentId)
    .andWhere("p.user_id", input.userId)
    .select("i.purchase_id")
    .first();

  if (!reference) {
    throw new InstallmentPaymentError("INSTALLMENT_NOT_FOUND");
  }

  await knex.transaction(async (trx) => {
    // Ordem global de locks: compra -> parcela(s) -> cartão.
    const purchase = await trx("credit_purchases")
      .where({ id: reference.purchase_id, user_id: input.userId })
      .forUpdate()
      .first();

    if (!purchase) {
      throw new InstallmentPaymentError("PURCHASE_NOT_FOUND");
    }
    if (purchase.status === "cancelled") {
      throw new InstallmentPaymentError("PURCHASE_CANCELLED");
    }

    const installment = await trx("installments")
      .where({
        id: input.installmentId,
        purchase_id: purchase.id,
        user_id: input.userId,
      })
      .forUpdate()
      .first();

    if (!installment) {
      throw new InstallmentPaymentError("INSTALLMENT_NOT_FOUND");
    }
    if (installment.status === "paid") {
      throw new InstallmentPaymentError("ALREADY_PAID");
    }
    if (installment.status === "cancelled") {
      throw new InstallmentPaymentError("INSTALLMENT_CANCELLED");
    }

    const account = await trx("accounts")
      .where({ id: input.accountId, user_id: input.userId })
      .first();
    if (!account) {
      throw new InstallmentPaymentError("ACCOUNT_NOT_FOUND");
    }

    const card = await trx("cards")
      .where({ id: purchase.card_id, user_id: input.userId })
      .forUpdate()
      .first();
    if (!card) {
      throw new InstallmentPaymentError("CARD_NOT_FOUND");
    }

    const today = new Date().toISOString().split("T")[0];
    const amount = Math.abs(Number(installment.amount));

    await trx("installments").where({ id: installment.id }).update({
      status: "paid",
      completed_date: today,
    });

    await trx("transactions").insert({
      id: randomUUID(),
      user_id: input.userId,
      account_id: input.accountId,
      category_id: purchase.category_id,
      title:
        "Fatura: " +
        purchase.title +
        " (" +
        installment.installment_number +
        "/" +
        installment.total_installments +
        ")",
      amount,
      type: "saida",
      status: "completed",
      expected_date: installment.expected_date,
      completed_date: today,
    });

    await trx("cards")
      .where({ id: card.id })
      .update({
        available_limit: trx.raw(
          "LEAST(total_limit, available_limit + ?)",
          [amount],
        ),
      });
  });
}
