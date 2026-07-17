import type { Transaction } from "../../../types/transaction";
import type { FinancialEventDTO, FinancialEventType } from "../types"; // ✨ Corrigido o import

export function mapToFinancialEvents(
  transactions: Transaction[],
): FinancialEventDTO[] {
  // ✨ Corrigido o retorno
  return transactions.map((tx) => {
    // 🧪 Heurística temporária para simular o polimorfismo
    // Na Etapa 7, o backend fará isto de forma nativa e precisa.
    let eventType: FinancialEventType = "transaction";
    let contextProps: FinancialEventDTO["context"] = {}; // ✨ Usando o context do DTO

    const titleLower = tx.title.toLowerCase();

    // Simula uma parcela se encontrar "x/y" ou a palavra "parcela"
    if (titleLower.includes("parcela") || titleLower.match(/\d+\/\d+/)) {
      eventType = "installment";
      contextProps = {
        purchaseId: `purchase-${tx.id}`,
        installmentNumber: 1, // Simulado
        totalInstallments: 12, // Simulado
        cardName: tx.account_name || "Cartão",
      };
    }
    // Simula uma assinatura para serviços conhecidos
    else if (
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
      status: tx.status === "completed" ? "completed" : "pending", // ✨ A nossa correção do Bug 2
      date: tx.created_at || new Date().toISOString(),
      type: eventType, // ✨ O DTO espera 'type' em vez de 'eventType'
      category: tx.category_name, // ✨ O DTO espera 'category'
      account: tx.account_name, // ✨ O DTO espera 'account'
      context: contextProps, // ✨ O DTO isola os dados extras aqui
    } as FinancialEventDTO;
  });
}
