import type { Transaction } from "../../../types/transaction";
import type { FinancialEventDTO, FinancialEventType } from "../types";

type MasterQueryPayload = Transaction & Partial<FinancialEventDTO>;

export function mapToFinancialEvents(
  transactions: MasterQueryPayload[],
): FinancialEventDTO[] {
  return transactions.map((tx) => {
    // Captura os dados reais e precisos vindos da Master Query
    const realType = tx.type;
    const realContext = tx.context;

    if (realType && realContext) {
      return {
        id: tx.id,
        title: tx.title,
        amount: tx.amount,
        flow: tx.flow || (tx.amount >= 0 ? "income" : "expense"),
        status: tx.status === "completed" ? "completed" : "pending",
        date: tx.date || tx.created_at || new Date().toISOString(),
        type: realType,
        category: tx.category || tx.category_name,
        account: tx.account || tx.account_name,
        context: realContext,
      } as FinancialEventDTO;
    }

    let eventType: FinancialEventType = "transaction";
    let contextProps: FinancialEventDTO["context"] = {};

    const titleLower = tx.title?.toLowerCase() || "";

    if (titleLower.includes("parcela") || titleLower.match(/\d+\/\d+/)) {
      eventType = "installment";
      contextProps = {
        purchaseId: `purchase-${tx.id}`,
        installmentNumber: 1,
        totalInstallments: 12,
        cardName: tx.account_name || "Cartão",
      };
    } else if (
      titleLower.includes("netflix") ||
      titleLower.includes("spotify") ||
      titleLower.includes("amazon")
    ) {
      eventType = "subscription";
      contextProps = {
        subscriptionId: `sub-${tx.id}`,
        nextBillingDate: new Date(
          Date.now() + 30 * 24 * 60 * 60 * 1000,
        ).toISOString(),
      };
    }

    return {
      id: tx.id,
      title: tx.title,
      amount: tx.amount,
      flow: tx.amount >= 0 ? "income" : "expense",
      status: tx.status === "completed" ? "completed" : "pending",
      date: tx.created_at || new Date().toISOString(),
      type: eventType,
      category: tx.category_name,
      account: tx.account_name,
      context: contextProps,
    } as FinancialEventDTO;
  });
}
