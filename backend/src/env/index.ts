import { config } from "dotenv";
import { z } from "zod";

if (process.env.NODE_ENV === "test") {
  config({ path: ".env.test" });
} else {
  config();
}

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("production"),
  DATABASE_URL: z.string().min(1, "DATABASE_URL is required"),
  FRONTEND_URL: z.string().default("http://localhost:5173"),
  PORT: z.coerce.number().default(3333),
  JWT_SECRET: z.string().min(1, "JWT_SECRET is required"),
  CORS_ORIGIN: z.string().default("http://localhost:5173"),
  DATABASE_CLIENT: z.enum(["pg", "sqlite", "sqlite3"]).default("pg"),
  SMTP_HOST: z.string().default("smtp-relay.brevo.com"),
  SMTP_PORT: z.coerce.number().default(2525),
  SMTP_USER: z.string().default(""),
  SMTP_PASSWORD: z.string().default(""),
  EMAIL_FROM: z.string().default("noreply@fluxa.com"),

  DEMO_CLEANUP_ENABLED: z.coerce.boolean().default(false),
  DEMO_DATA_RETENTION_DAYS: z.coerce.number().default(30),
});

const _env = envSchema.safeParse(process.env);

if (_env.success === false) {
  console.error("Invalid environment variables!", _env.error.format());
  throw new Error("Invalid environment variables.");
}

export const env = _env.data;
