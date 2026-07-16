import { db as knex } from "../database.js";
import type {
  FinancialEventFilter,
  FinancialEventDTO,
} from "../@types/financial-events.js";

export class FinancialEventsRepository {
  async getEvents(
    userId: string,
    filters: FinancialEventFilter,
  ): Promise<{ items: FinancialEventDTO[]; total: number }> {
    // -------------------------------------------------------------------------
    // 1. DOMÍNIO: TRANSAÇÕES À VISTA
    // -------------------------------------------------------------------------
    const transactionsQuery = knex("transactions as t")
      .select(
        "t.id",
        "t.title",
        "t.amount",
        knex.raw(
          "CASE WHEN t.amount >= 0 THEN 'income' ELSE 'expense' END as flow",
        ),
        "t.status",
        "t.created_at as date",
        knex.raw("'transaction' as type"),
        "t.description as merchant",
        "t.observation as notes",
        "t.category_id as categoryId",
        "cat.name as category",
        "t.account_id as accountId",
        "acc.name as account",
        knex.raw("NULL::text as card_name"), // Cast explícito para UNION
        knex.raw("'{}'::jsonb as context"), // JSONB vazio para evitar erro no filtro context->>'cardId'
        "t.created_at as createdAt",
        knex.raw('NULL::timestamp as "updatedAt"'), // Correção: Transações não têm updated_at
      )
      .leftJoin("categories as cat", "t.category_id", "cat.id")
      .leftJoin("accounts as acc", "t.account_id", "acc.id")
      .where("t.user_id", userId);

    // -------------------------------------------------------------------------
    // 2. DOMÍNIO: PARCELAMENTOS
    // -------------------------------------------------------------------------
    const installmentsQuery = knex("installments as i")
      .select(
        "i.id",
        "p.title",
        "i.amount",
        knex.raw("'expense' as flow"),
        "i.status",
        knex.raw("i.expected_date::timestamp as date"), // Cast para equiparar com created_at
        knex.raw("'installment' as type"),
        "p.store as merchant",
        "p.observation as notes",
        "p.category_id as categoryId",
        "cat.name as category",
        knex.raw('NULL::uuid as "accountId"'), // Cast explícito para UNION
        knex.raw("NULL::text as account"),
        "c.name as card_name",
        knex.raw(`jsonb_build_object(
          'purchaseId', p.id,
          'installmentNumber', i.installment_number,
          'totalInstallments', p.total_installments,
          'cardId', p.card_id,
          'cardName', c.name
        ) as context`),
        knex.raw('p.purchase_date::timestamp as "createdAt"'), // Correção: Installments não têm created_at
        knex.raw('NULL::timestamp as "updatedAt"'), // Correção: Installments não têm updated_at
      )
      .join("credit_purchases as p", "i.purchase_id", "p.id")
      .leftJoin("categories as cat", "p.category_id", "cat.id")
      .leftJoin("cards as c", "p.card_id", "c.id")
      .where("p.user_id", userId);

    // -------------------------------------------------------------------------
    // 3. DOMÍNIO: ASSINATURAS
    // -------------------------------------------------------------------------
    const subscriptionsQuery = knex("subscriptions as s")
      .select(
        "s.id",
        "s.title",
        "s.amount",
        knex.raw("'expense' as flow"),
        "s.status",
        "s.created_at as date",
        knex.raw("'subscription' as type"),
        knex.raw("NULL::text as merchant"),
        knex.raw("NULL::text as notes"),
        "s.category_id as categoryId",
        "cat.name as category",
        "s.account_id as accountId",
        "acc.name as account",
        "c.name as card_name",
        knex.raw(`jsonb_build_object(
          'subscriptionId', s.id
        ) as context`),
        "s.created_at as createdAt",
        "s.updated_at as updatedAt",
      )
      .leftJoin("categories as cat", "s.category_id", "cat.id")
      .leftJoin("accounts as acc", "s.account_id", "acc.id") // Correção de JOIN: Faltava o relacionamento da conta
      .leftJoin("cards as c", "s.card_id", "c.id")
      .where("s.user_id", userId);

    // -------------------------------------------------------------------------
    // 4. THE MASTER QUERY
    // -------------------------------------------------------------------------
    const mainQuery = knex
      .with("transactions_events", transactionsQuery)
      .with("installment_events", installmentsQuery)
      .with("subscription_events", subscriptionsQuery)
      .from(
        knex.raw(`(
        SELECT * FROM transactions_events
        UNION ALL
        SELECT * FROM installment_events
        UNION ALL
        SELECT * FROM subscription_events
      ) as all_events`),
      );

    // -------------------------------------------------------------------------
    // 5. FILTROS
    // -------------------------------------------------------------------------
    if (filters.query) {
      const searchTerms = filters.query.trim().split(/\s+/);
      searchTerms.forEach((term) => {
        mainQuery.where((builder) => {
          builder
            .where("title", "ilike", `%${term}%`)
            .orWhere("merchant", "ilike", `%${term}%`)
            .orWhere("notes", "ilike", `%${term}%`)
            .orWhere("category", "ilike", `%${term}%`)
            .orWhere("account", "ilike", `%${term}%`)
            .orWhere("card_name", "ilike", `%${term}%`);
        });
      });
    }

    if (filters.type?.length) mainQuery.whereIn("type", filters.type);
    if (filters.flow?.length) mainQuery.whereIn("flow", filters.flow);
    if (filters.status?.length) mainQuery.whereIn("status", filters.status);
    if (filters.categoryIds?.length)
      mainQuery.whereIn("categoryId", filters.categoryIds);
    if (filters.accountIds?.length)
      mainQuery.whereIn("accountId", filters.accountIds);

    if (filters.cardIds?.length) {
      mainQuery.whereRaw(
        `context->>'cardId' IN (${filters.cardIds.map((id) => `'${id}'`).join(",")})`,
      );
    }

    if (filters.minAmount !== undefined)
      mainQuery.where("amount", ">=", filters.minAmount);
    if (filters.maxAmount !== undefined)
      mainQuery.where("amount", "<=", filters.maxAmount);
    if (filters.startDate) mainQuery.where("date", ">=", filters.startDate);
    if (filters.endDate) mainQuery.where("date", "<=", filters.endDate);

    // -------------------------------------------------------------------------
    // 6. EXECUÇÃO
    // -------------------------------------------------------------------------
    const page = filters.page || 1;
    const pageSize = filters.pageSize || 50;

    const countResult = (await mainQuery.clone().count("* as total")) as Array<{
      total: string | number;
    }>;
    const total = countResult[0]?.total || 0;

    let sortColumn = "date";
    let sortDirection = "desc";

    if (filters.sort) {
      switch (filters.sort) {
        case "date_asc":
          sortColumn = "date";
          sortDirection = "asc";
          break;
        case "amount_desc":
          sortColumn = "amount";
          sortDirection = "desc";
          break;
        case "amount_asc":
          sortColumn = "amount";
          sortDirection = "asc";
          break;
        case "name_asc":
          sortColumn = "title";
          sortDirection = "asc";
          break;
        case "updated_at_desc":
          sortColumn = '"updatedAt"';
          sortDirection = "desc";
          break;
      }
    }

    const items = await mainQuery
      .orderBy(sortColumn, sortDirection)
      .limit(pageSize)
      .offset((page - 1) * pageSize);

    // Remove campos que eram necessários no filtro/SQL mas que o DTO não pede na raiz (como card_name)
    const cleanedItems = items.map(({ card_name, ...rest }) => rest);

    return {
      items: cleanedItems as FinancialEventDTO[],
      total: Number(total),
    };
  }
}
