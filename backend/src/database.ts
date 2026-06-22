import "dotenv/config";
import knex from "knex";
import type { Knex } from "knex";
import { env } from "./env/index.js";

if (!process.env.DATABASE_URL) {
   throw new Error("DATABASE_URL env not found.");
}

export const config: Knex.Config = {
   client: env.DATABASE_CLIENT, // <- em vez de "sqlite"
   connection:
      env.DATABASE_CLIENT === "sqlite"
         ? {
              filename: env.DATABASE_URL,
           }
         : env.DATABASE_URL,
   useNullAsDefault: true,
   migrations: {
      extension: "ts",
      directory: "./db/migrations",
   },
};

export const db = knex(config);

type Teste = Parameters<typeof db>[0];
