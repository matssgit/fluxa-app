import "dotenv/config";
import knex from "knex";
import path from "node:path";
import type { Knex } from "knex";
import { env } from "../env/index.js";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const isPg = env.DATABASE_CLIENT === "pg";
const isProduction = env.NODE_ENV === "production";

export const config: Knex.Config = {
  client: env.DATABASE_CLIENT,
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
