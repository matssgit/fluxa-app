// -----------------------------------------------------------------------------
// 1. DOMÍNIOS LITERAIS (Imutáveis)
// -----------------------------------------------------------------------------
export type EventStatus = "pending" | "completed" | "future" | "cancelled";
export type EventFlow = "income" | "expense" | "transfer";
export type EventType =
  | "transaction"
  | "installment"
  | "subscription"
  | "transfer";

// Padronização rigorosa da Ordenação
export type EventSort =
  | "date_desc"
  | "date_asc"
  | "amount_desc"
  | "amount_asc"
  | "name_asc"
  | "next_billing"
  | "updated_at_desc";

// -----------------------------------------------------------------------------
// 2. FINANCIAL EVENT FILTER (O "SearchCriteria" da Omnibox)
// -----------------------------------------------------------------------------
// O Backend não recebe mais pedaços de queries. Recebe um objeto consolidado.
export interface FinancialEventFilter {
  query?: string; // O input cru da Super Omnibox (texto livre)

  // Agrupamentos (Divulgação Progressiva)
  status?: EventStatus[];
  flow?: EventFlow[];
  type?: EventType[];

  // Entidades (Para seleção múltipla nos Filtros Avançados)
  accountIds?: string[];
  cardIds?: string[];
  categoryIds?: string[];

  // Range de Valores
  minAmount?: number;
  maxAmount?: number;

  // Range de Período
  startDate?: string; // Formato ISO: YYYY-MM-DD
  endDate?: string; // Formato ISO: YYYY-MM-DD

  // Comportamento
  sort?: EventSort;
  page?: number;
  pageSize?: number;
}

// -----------------------------------------------------------------------------
// 3. FINANCIAL EVENT DTO (O Único Idioma do Frontend)
// -----------------------------------------------------------------------------
// Nenhuma tabela do banco de dados vaza para a interface. O DTO consolida tudo.
export interface FinancialEventDTO {
  id: string; // O ID universal do evento
  title: string; // Título original (Nome da compra, da conta, etc)
  amount: number; // Valor nominal
  flow: EventFlow; // income | expense | transfer
  status: EventStatus; // pending | completed | future
  date: string; // ISO Date (Vencimento, data da compra ou próxima cobrança)
  type: EventType; // O tipo que define como a UI e o CommandService vão agir

  // Metadata para Analytics, Dashboard e Super Omnibox
  merchant?: string; // Estabelecimento comercial (ex: "Mercado Livre")
  notes?: string; // Observações do utilizador
  categoryId?: string;
  category?: string; // Nome da categoria para UI
  accountId?: string;
  account?: string; // Nome da conta para UI

  // O Contexto Isolado:
  // O Frontend só usa o que for pertinente ao EventType, mantendo o objeto limpo.
  context?: {
    //  Se for Installment (Parcela)
    purchaseId?: string;
    installmentNumber?: number;
    totalInstallments?: number;
    cardId?: string;
    cardName?: string;

    //  Se for Subscription (Assinatura)
    subscriptionId?: string;
    nextBillingDate?: string;

    //  Se for Transfer (Preparação Futura)
    destinationAccountId?: string;
    destinationAccountName?: string;
  };

  // Auditoria Básica
  createdAt: string;
  updatedAt: string;
}

// -----------------------------------------------------------------------------
// 4. PAGINAÇÃO IMUTÁVEL
// -----------------------------------------------------------------------------
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNextPage: boolean;
}
