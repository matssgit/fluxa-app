export function formatCurrency(value?: number | null): string {
  const safeValue = typeof value === "number" && !isNaN(value) ? value : 0;
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(safeValue);
}

export function formatPercentage(value?: number | null, decimals = 0): string {
  const safeValue = typeof value === "number" && !isNaN(value) ? value : 0;
  return `${safeValue.toFixed(decimals)}%`;
}

export function formatDate(dateInput?: string | Date | null): string {
  if (!dateInput) return "Data flexível";
  const date = typeof dateInput === "string" ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return "Data inválida";

  return date.toLocaleDateString("pt-BR", {
    timeZone: "UTC",
  });
}
