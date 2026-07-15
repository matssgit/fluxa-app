import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";

export function useTransactionFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  // ==========================================
  // 1. PESQUISA (q)
  // ==========================================
  const query = searchParams.get("q") || "";
  const [searchInput, setSearchInput] = useState(query);
  const [prevQuery, setPrevQuery] = useState(query);

  if (query !== prevQuery) {
    setPrevQuery(query);
    setSearchInput(query);
  }

  useEffect(() => {
    if (searchInput === query) return;
    const handler = setTimeout(() => {
      setSearchParams(
        (prev) => {
          if (searchInput.trim()) {
            prev.set("q", searchInput.trim());
          } else {
            prev.delete("q");
          }
          return prev;
        },
        { replace: true }
      );
    }, 300);
    return () => clearTimeout(handler);
  }, [searchInput, setSearchParams, query]);

  // ==========================================
  // 2. FILTROS RÁPIDOS (f)
  // ==========================================
  // A URL armazena como string "income,pending", convertemos para Array
  const activeFilters = searchParams.get("f")?.split(",").filter(Boolean) || [];

  const toggleFilter = (filterId: string) => {
    setSearchParams(
      (prev) => {
        const current = prev.get("f")?.split(",").filter(Boolean) || [];
        const isSelected = current.includes(filterId);

        let nextFilters;
        if (isSelected) {
          nextFilters = current.filter((id) => id !== filterId);
        } else {
          nextFilters = [...current, filterId];
        }

        if (nextFilters.length > 0) {
          prev.set("f", nextFilters.join(","));
        } else {
          prev.delete("f");
        }
        return prev;
      },
      { replace: true }
    );
  };

  // ==========================================
  // 3. ORDENAÇÃO (sort)
  // ==========================================
  // "date_desc" é o padrão. Se não houver sort na URL, assumimos "date_desc".
  const activeSort = searchParams.get("sort") || "date_desc";

  const setSort = (sortId: string) => {
    setSearchParams(
      (prev) => {
        if (sortId === "date_desc") {
          prev.delete("sort"); // Mantém a URL limpa se for a ordenação padrão
        } else {
          prev.set("sort", sortId);
        }
        return prev;
      },
      { replace: true }
    );
  };

  // ==========================================
  // 4. LIMPEZA TOTAL
  // ==========================================
  const clearAllFilters = () => {
    setSearchInput("");
    setSearchParams(
      (prev) => {
        prev.delete("q");
        prev.delete("f");
        prev.delete("sort");
        return prev;
      },
      { replace: true }
    );
  };

  return {
    // Search
    searchQuery: query,
    searchInput,
    setSearchInput,
    // Filters
    activeFilters,
    toggleFilter,
    // Sort
    activeSort,
    setSort,
    // Global
    clearAllFilters,
  };
}