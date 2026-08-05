import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchFinancialEvents } from "../api/financial-events.api";
import type { FinancialEventFilters } from "../types";

export function useFinancialEvents(filters: FinancialEventFilters) {
  return useInfiniteQuery({
    queryKey: ["financial-events", filters],
    queryFn: ({ pageParam = 1 }) =>
      fetchFinancialEvents({
        ...filters,
        page: pageParam as number,
        pageSize: 20,
      }),
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      // Se o backend diz que há próxima página, pedimos a page + 1
      return lastPage.hasNextPage ? lastPage.page + 1 : undefined;
    },
    staleTime: 1000 * 60 * 5,
  });
}
