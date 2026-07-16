import { api } from "../../../api/client";
import type {
  FinancialEventDTO,
  FinancialEventFilters,
  PaginatedResponse,
} from "../types";

export async function fetchFinancialEvents(
  filters: FinancialEventFilters & { page: number; pageSize: number },
): Promise<PaginatedResponse<FinancialEventDTO>> {
  const queryParams: Record<string, string | number | undefined> = {
    query: filters.query,
    minAmount: filters.minAmount,
    maxAmount: filters.maxAmount,
    startDate: filters.startDate,
    endDate: filters.endDate,
    sort: filters.sort as string | undefined,
    page: filters.page,
    pageSize: filters.pageSize,
  };

  if (filters.status?.length) queryParams.status = filters.status.join(",");
  if (filters.flow?.length) queryParams.flow = filters.flow.join(",");
  if (filters.type?.length) queryParams.type = filters.type.join(",");
  if (filters.accountIds?.length)
    queryParams.accountIds = filters.accountIds.join(",");
  if (filters.cardIds?.length) queryParams.cardIds = filters.cardIds.join(",");
  if (filters.categoryIds?.length)
    queryParams.categoryIds = filters.categoryIds.join(",");

  const cleanParams = Object.fromEntries(
    Object.entries(queryParams).filter(([, value]) => {
      if (value === undefined || value === null || value === "") return false;
      return true;
    }),
  );

  const { data } = await api.get<PaginatedResponse<FinancialEventDTO>>(
    "/financial-events",
    {
      params: cleanParams,
    },
  );

  return data;
}
