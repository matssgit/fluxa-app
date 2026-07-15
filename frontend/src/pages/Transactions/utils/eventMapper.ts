import type { Transaction } from "../../../types/transaction";
import type { FinancialEvent, FinancialEventType } from "../types";

export function mapToFinancialEvents(
  transactions: Transaction[],
): FinancialEvent[] {
  return transactions.map((tx) => {
    // 🧪 Heurística temporária para simular o polimorfismo
    // Na Etapa 7, o backend fará isto de forma nativa e precisa.
    let eventType: FinancialEventType = "transaction";
    let extraProps = {};

    const titleLower = tx.title.toLowerCase();

    // Simula uma parcela se encontrar "x/y" ou a palavra "parcela"
    if (titleLower.includes("parcela") || titleLower.match(/\d+\/\d+/)) {
      eventType = "installment";
      extraProps = {
        purchase_id: `purchase-${tx.id}`,
        installment_number: 1, // Simulado
        total_installments: 12, // Simulado
        card_name: tx.account_name || "Cartão",
      };
    }
    // Simula uma assinatura para serviços conhecidos
    else if (
      titleLower.includes("netflix") ||
      titleLower.includes("spotify") ||
      titleLower.includes("amazon")
    ) {
      eventType = "subscription";
      extraProps = {
        subscription_id: `sub-${tx.id}`,
        next_billing_date: new Date(
          Date.now() + 30 * 24 * 60 * 60 * 1000,
        ).toISOString(),
      };
    }

    return {
      id: tx.id,
      title: tx.title,
      amount: tx.amount,
      flow: tx.amount >= 0 ? "income" : "expense",
      status: tx.status === "pending" ? "pending" : "completed",
      date: tx.created_at || new Date().toISOString(),
      category_name: tx.category_name,
      account_name: tx.account_name,
      eventType,
      ...extraProps,
    } as FinancialEvent;
  });
}
