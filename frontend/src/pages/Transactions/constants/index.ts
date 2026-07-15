export const QUICK_FILTERS = [
  { id: "today", label: "Hoje" },
  { id: "week", label: "Semana" },
  { id: "month", label: "Mês" },
  { id: "income", label: "Receitas" },
  { id: "expense", label: "Despesas" },
  { id: "pending", label: "Pendentes" },
  { id: "completed", label: "Concluídas" },
] as const;

export const SORT_OPTIONS = [
  { id: "date_desc", label: "Mais recentes" },
  { id: "date_asc", label: "Mais antigas" },
  { id: "amount_desc", label: "Maior valor" },
  { id: "amount_asc", label: "Menor valor" },
  { id: "name_asc", label: "Nome A-Z" },
] as const;
