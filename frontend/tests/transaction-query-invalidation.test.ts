import assert from "node:assert/strict";
import test from "node:test";

import { invalidateAfterTransactionCreation } from "../src/lib/transaction-query-invalidation.ts";

test("invalidates the cash history after creating a transaction", () => {
  const invalidatedKeys: string[] = [];

  invalidateAfterTransactionCreation({
    invalidateQueries: ({ queryKey }) => {
      invalidatedKeys.push(queryKey.join("/"));
    },
  });

  assert.ok(invalidatedKeys.includes("financial-events"));
});
