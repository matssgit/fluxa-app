export type TransactionDirection = "entrada" | "saida";

type TransactionLike = {
  amount: string | number | null | undefined;
  type?: string | null;
  subscription_id?: string | null;
};

const INCOME_TYPES = new Set(["entrada", "income", "receita"]);
const EXPENSE_TYPES = new Set(["saida", "expense", "despesa"]);

export function normalizeTransactionType(
  value: string | null | undefined,
): TransactionDirection | null {
  const normalized = String(value || "").trim().toLowerCase();

  if (INCOME_TYPES.has(normalized)) return "entrada";
  if (EXPENSE_TYPES.has(normalized)) return "saida";
  return null;
}

export function canonicalizeTransaction(
  amount: number,
  type?: string | null,
): { amount: number; type: TransactionDirection } {
  const normalizedType = normalizeTransactionType(type);

  return {
    amount: Math.abs(amount),
    type: normalizedType || (amount < 0 ? "saida" : "entrada"),
  };
}

export function getTransactionDirection(
  transaction: TransactionLike,
): TransactionDirection {
  if (transaction.subscription_id) return "saida";

  const normalizedType = normalizeTransactionType(transaction.type);
  if (normalizedType) return normalizedType;

  const rawType = String(transaction.type || "").trim();
  if (rawType === "") {
    // Compatibilidade isolada para registros anteriores à coluna type.
    return Number(transaction.amount || 0) < 0 ? "saida" : "entrada";
  }

  // Tipo legado desconhecido falha de forma conservadora como saída, sem usar sinal.
  return "saida";
}

export function getTransactionMagnitude(transaction: TransactionLike): number {
  return Math.abs(Number(transaction.amount || 0));
}

export function getSignedTransactionAmount(
  transaction: TransactionLike,
): number {
  const magnitude = getTransactionMagnitude(transaction);
  return getTransactionDirection(transaction) === "entrada"
    ? magnitude
    : -magnitude;
}

export function transactionFlowSql(alias = "t"): string {
  return (
    "CASE " +
    "WHEN " + alias + ".subscription_id IS NOT NULL THEN 'expense' " +
    "WHEN LOWER(COALESCE(" + alias + ".type, '')) IN ('entrada', 'income', 'receita') THEN 'income' " +
    "WHEN LOWER(COALESCE(" + alias + ".type, '')) IN ('saida', 'expense', 'despesa') THEN 'expense' " +
    "WHEN TRIM(COALESCE(" + alias + ".type, '')) = '' AND " + alias + ".amount < 0 THEN 'expense' " +
    "WHEN TRIM(COALESCE(" + alias + ".type, '')) = '' THEN 'income' " +
    "ELSE 'expense' END"
  );
}
