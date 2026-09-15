export function getInstallmentDueDate(
  purchaseDate: string,
  monthsAhead: number,
  dueDay: number,
): string {
  const [yearText, monthText] = purchaseDate.split("-");
  const year = Number(yearText);
  const monthIndex = Number(monthText) - 1;
  const targetMonth = new Date(Date.UTC(year, monthIndex + monthsAhead, 1));
  const targetYear = targetMonth.getUTCFullYear();
  const targetMonthIndex = targetMonth.getUTCMonth();
  const lastDay = new Date(
    Date.UTC(targetYear, targetMonthIndex + 1, 0),
  ).getUTCDate();
  const validDay = Math.min(dueDay, lastDay);

  return new Date(
    Date.UTC(targetYear, targetMonthIndex, validDay),
  )
    .toISOString()
    .slice(0, 10);
}
