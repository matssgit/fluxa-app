export function formatDate(dateString: string): string {
   return new Intl.DateTimeFormat("pt-BR").format(new Date(dateString));
}
