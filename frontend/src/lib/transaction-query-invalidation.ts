type QueryInvalidator = {
  invalidateQueries: (options: { queryKey: readonly string[] }) => unknown;
};

export const TRANSACTION_CREATION_QUERY_KEYS = [
  ["transactions"],
  ["financial-events"],
  ["summary"],
  ["dashboard"],
  ["accounts"],
] as const;

export function invalidateAfterTransactionCreation(
  queryClient: QueryInvalidator,
): void {
  TRANSACTION_CREATION_QUERY_KEYS.forEach((queryKey) => {
    queryClient.invalidateQueries({ queryKey });
  });
}
