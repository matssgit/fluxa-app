export type EventStatus = "pending" | "completed" | "future" | "cancelled";
export type EventFlow = "income" | "expense" | "transfer";
export type EventType =
  | "transaction"
  | "installment"
  | "subscription"
  | "transfer";

export type EventSort =
  | "date_desc"
  | "date_asc"
  | "amount_desc"
  | "amount_asc"
  | "name_asc"
  | "next_billing"
  | "updated_at_desc";

export interface FinancialEventFilter {
  query?: string;
  status?: EventStatus[];
  flow?: EventFlow[];
  type?: EventType[];

  accountIds?: string[];
  cardIds?: string[];
  categoryIds?: string[];

  minAmount?: number;
  maxAmount?: number;

  startDate?: string; // Formato ISO: YYYY-MM-DD
  endDate?: string; // Formato ISO: YYYY-MM-DD

  sort?: EventSort;
  page?: number;
  pageSize?: number;
}

export interface FinancialEventDTO {
  id: string;
  title: string;
  amount: number;
  flow: EventFlow;
  status: EventStatus;
  date: string;
  type: EventType;

  merchant?: string;
  notes?: string;
  categoryId?: string;
  category?: string;
  accountId?: string;
  account?: string;

  context?: {
    // Se for Installment (Parcela)
    purchaseId?: string;
    installmentNumber?: number;
    totalInstallments?: number;
    cardId?: string;
    cardName?: string;

    // Se for Subscription (Assinatura)
    subscriptionId?: string;
    nextBillingDate?: string;

    // Se for Transfer (Preparação Futura)
    destinationAccountId?: string;
    destinationAccountName?: string;
  };

  // Auditoria Básica
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNextPage: boolean;
}
