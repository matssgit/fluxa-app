import "dotenv/config";
import knex from "knex";
import type { Knex } from "knex";
import { env } from "./env/index.js"; 
if (!process.env.DATABASE_URL) {
   throw new Error("DATABASE_URL env not found.");
}

export const config: Knex.Config = {
   client: env.DATABASE_CLIENT,
   connection: env.DATABASE_URL,
   migrations: {
      extension: "ts",
      directory: "./db/migrations",
   },
};

export const db = knex(config);
