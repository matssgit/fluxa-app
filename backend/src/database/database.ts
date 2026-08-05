import "dotenv/config";
import knex from "knex";
import type { Knex } from "knex";
import { env } from "../env/index.js";
import { fileURLToPath } from "node:url";
import path from "node:path";

if (!process.env.DATABASE_URL) {
  throw new Error("DATABASE_URL env not found.");
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const isPg = (env.DATABASE_CLIENT || process.env.DATABASE_CLIENT) === "pg";
const isProduction = env.NODE_ENV === "production";

export const config: Knex.Config = {
  client: isPg ? "pg" : "sqlite",
  connection: isPg
    ? {
        connectionString: env.DATABASE_URL,
        ssl: isProduction ? { rejectUnauthorized: false } : false,
      }
    : env.DATABASE_URL,
  migrations: {
    extension: "ts",
    directory: path.resolve(__dirname, "../../db/migrations"),
  },
  useNullAsDefault: !isPg,
};

export const db = knex(config);
export default config;
