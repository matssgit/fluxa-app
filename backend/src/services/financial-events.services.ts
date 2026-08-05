import { FinancialEventsRepository } from "../repositories/financial-events.repository.js";
import type {
  FinancialEventFilter,
  PaginatedResponse,
  FinancialEventDTO,
} from "../types/financial-events.js";

export class FinancialEventsService {
  private repository: FinancialEventsRepository;

  constructor() {
    this.repository = new FinancialEventsRepository();
  }

  async getEvents(
    userId: string,
    filters: FinancialEventFilter,
  ): Promise<PaginatedResponse<FinancialEventDTO>> {
    const safeFilters: FinancialEventFilter = {
      ...filters,
      page: filters.page && filters.page > 0 ? Number(filters.page) : 1,
      pageSize:
        filters.pageSize && filters.pageSize > 0
          ? Number(filters.pageSize)
          : 50,
    };

    const { items, total } = await this.repository.getEvents(
      userId,
      safeFilters,
    );

    const hasNextPage =
      (safeFilters.page as number) * (safeFilters.pageSize as number) < total;

    return {
      items: items as FinancialEventDTO[],
      total,
      page: safeFilters.page as number,
      pageSize: safeFilters.pageSize as number,
      hasNextPage,
    };
  }
}
