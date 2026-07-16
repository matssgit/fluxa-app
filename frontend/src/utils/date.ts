export function parseLocalDate(dateString: string): Date {
  if (!dateString) return new Date();
  return new Date(`${dateString}T12:00:00`);
}

export function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function formatDate(dateString: string): string {
  const date = new Date(`${dateString}T12:00:00`);
  return new Intl.DateTimeFormat("pt-BR").format(date);
}
