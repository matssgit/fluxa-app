import { app } from "../app.js";
import { db } from "../database/database.js";
import { beforeAll, afterAll, beforeEach, vi } from "vitest";
import { env } from "../env/index.js";
import { assertSafeTestDatabase } from "./test-database-guard.js";

assertSafeTestDatabase(env.DATABASE_URL, env.DATABASE_CLIENT);

beforeAll(async () => {
  await app.ready();
});

afterAll(async () => {
  await app.close();
  await db.destroy();
});

beforeEach(async () => {
  await db.raw(`
      TRUNCATE TABLE 
         accounts, 
         transactions, 
         cards, 
         credit_purchases, 
         installments, 
         subscriptions, 
         wallets,
         wallet_history
      RESTART IDENTITY CASCADE;
   `);

  vi.useRealTimers();
});
