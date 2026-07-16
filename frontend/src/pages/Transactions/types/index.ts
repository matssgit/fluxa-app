// 1. DOMÍNIOS LITERAIS
export type FinancialEventStatus =
  | "pending"
  | "completed"
  | "future"
  | "cancelled";
export type FinancialEventFlow = "income" | "expense" | "transfer";
export type FinancialEventType =
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

// 2. FINANCIAL EVENT FILTERS (O Contrato da Super Omnibox)
export interface FinancialEventFilters {
  query?: string;

  status?: FinancialEventStatus[];
  flow?: FinancialEventFlow[];
  type?: FinancialEventType[];

  accountIds?: string[];
  cardIds?: string[];
  categoryIds?: string[];

  minAmount?: number;
  maxAmount?: number;

  startDate?: string;
  endDate?: string;

  sort?: EventSort | string;
  page?: number;
  pageSize?: number;
}

// 3. FINANCIAL EVENT DTO (O Contrato da UI)
export interface FinancialEventDTO {
  id: string;
  title: string;
  amount: number;
  flow: FinancialEventFlow;
  status: FinancialEventStatus;
  date: string;
  type: FinancialEventType;

  merchant?: string;
  notes?: string;
  categoryId?: string;
  category?: string;
  accountId?: string;
  account?: string;

  context?: {
    purchaseId?: string;
    installmentNumber?: number;
    totalInstallments?: number;
    cardId?: string;
    cardName?: string;
    subscriptionId?: string;
    nextBillingDate?: string;
    destinationAccountId?: string;
    destinationAccountName?: string;
  };

  createdAt?: string;
  updatedAt?: string;
}

// 4. PAGINAÇÃO
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNextPage: boolean;
}
