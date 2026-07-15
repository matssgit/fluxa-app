// Define a natureza do evento financeiro
export type FinancialEventType = "transaction" | "installment" | "subscription";
export type FinancialEventStatus = "pending" | "completed" | "future";
export type FinancialEventFlow = "income" | "expense" | "transfer";

// A fundação que TODOS os eventos partilham (Garante que a listagem base nunca quebra)
export interface BaseFinancialEvent {
  id: string;
  title: string;
  amount: number;
  flow: FinancialEventFlow;
  status: FinancialEventStatus;
  date: string; // Data unificada (pode vir de created_at, due_date, etc.)
  category_name?: string;
  account_name?: string;
  eventType: FinancialEventType;
}

// 1. Transação Comum (Pix, Débito, Dinheiro)
export interface TransactionEvent extends BaseFinancialEvent {
  eventType: "transaction";
}

// 2. Parcela de Cartão de Crédito
export interface InstallmentEvent extends BaseFinancialEvent {
  eventType: "installment";
  purchase_id: string;
  installment_number: number;
  total_installments: number;
  card_name: string;
}

// 3. Assinatura Recorrente
export interface SubscriptionEvent extends BaseFinancialEvent {
  eventType: "subscription";
  subscription_id: string;
  next_billing_date?: string;
}

// O Tipo União que a nossa Listagem Global vai consumir
export type FinancialEvent =
  | TransactionEvent
  | InstallmentEvent
  | SubscriptionEvent;
