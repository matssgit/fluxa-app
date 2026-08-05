import { api } from "../api/client";
import type {
  Transaction,
  Summary,
  NewTransactionInput,
} from "../types/transaction";

export async function getTransactions(): Promise<Transaction[]> {
  const { data } = await api.get<{ transactions: Transaction[] }>(
    "/transactions",
  );
  return data.transactions;
}

export async function getSummary(): Promise<Summary> {
  const { data } = await api.get<{ summary: Summary }>("/transactions/summary");
  return data.summary;
}

export async function createTransaction(
  transaction: NewTransactionInput,
): Promise<void> {
  await api.post("/transactions", transaction);
}
