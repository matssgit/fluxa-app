import { useState, useEffect } from "react";
import type { FinancialEventFilters } from "../types";

export function useTransactionFilters() {
  const [filters, setFilters] = useState<FinancialEventFilters>({});
  const [searchInput, setSearchInput] = useState("");

  useEffect(() => {
    const handler = setTimeout(() => {
      setFilters((prev) => ({
        ...prev,
        query: searchInput.trim() || undefined,
      }));
    }, 500);

    return () => clearTimeout(handler);
  }, [searchInput]);

  const clearAllFilters = () => {
    setFilters({});
    setSearchInput("");
  };

  return {
    filters,
    setFilters,
    searchInput,
    setSearchInput,
    clearAllFilters,
  };
}
