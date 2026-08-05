import { config } from "dotenv";
import { z } from "zod";

if (process.env.NODE_ENV === "test") {
  config({ path: ".env.test" });
} else {
  config();
}

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("production"),
  DATABASE_URL: z.string(),
  FRONTEND_URL: z.string().url(),
  PORT: z.coerce.number().default(3333),
  JWT_SECRET: z.string().min(1, "JWT_SECRET is required"),
  CORS_ORIGIN: z.string().default("http://localhost:5173"),
  DATABASE_CLIENT: z.enum(["pg", "sqlite", "sqlite3"]).default("pg"),
});

const _env = envSchema.safeParse(process.env);

if (_env.success === false) {
  console.error("Invalid environment variables!", _env.error.format());

  throw new Error("Invalid environment variables.");
}

export const env = _env.data;
