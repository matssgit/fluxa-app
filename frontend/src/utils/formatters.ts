/**
 * Formata um valor numérico para a moeda brasileira (R$ 0.000,00).
 * Blinda automaticamente contra NaN, null ou undefined, retornando R$ 0,00 como fallback.
 */
export function formatCurrency(value?: number | null): string {
  const safeValue = typeof value === "number" && !isNaN(value) ? value : 0;
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(safeValue);
}

/**
 * Formata um valor numérico para porcentagem (ex: 15% ou 15.5%).
 */
export function formatPercentage(value?: number | null, decimals = 0): string {
  const safeValue = typeof value === "number" && !isNaN(value) ? value : 0;
  return `${safeValue.toFixed(decimals)}%`;
}

/**
 * Formata uma string de data (ISO) ou objeto Date para o padrão brasileiro (DD/MM/AAAA).
 * Lida de forma segura com datas inválidas ou fusos horários UTC.
 */
export function formatDate(dateInput?: string | Date | null): string {
  if (!dateInput) return "Data flexível";
  const date = typeof dateInput === "string" ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return "Data inválida";

  return date.toLocaleDateString("pt-BR", {
    timeZone: "UTC",
  });
}
