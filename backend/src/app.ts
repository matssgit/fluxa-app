import fastify from "fastify";
import { ZodError } from "zod";
import cors from "@fastify/cors";
import cookie from "@fastify/cookie";
import helmet from "@fastify/helmet";
import fastifyJwt from "@fastify/jwt";
import rateLimit from "@fastify/rate-limit";

import { env } from "./env/index.js";

import { usersRoutes } from "./routes/users.js";
import { creditRoutes } from "./routes/credit.js";
import { walletsRoutes } from "./routes/wallets.js";
import { accountsRoutes } from "./routes/accounts.js";
import { analyticsRoutes } from "./routes/analytics.js";
import { dashboardRoutes } from "./routes/dashboard.js";
import { categoriesRoutes } from "./routes/categories.js";
import { transactionsRoutes } from "./routes/transactions.js";
import { subscriptionsRoutes } from "./routes/subscriptions.js";
import { financialEventsRoutes } from "./routes/financial-events.js";

export const app = fastify();

app.register(helmet, {
  global: true,
  crossOriginResourcePolicy: false,
});

app.register(rateLimit, {
  max: 100,
  timeWindow: "1 minute",
  errorResponseBuilder: function (_request, _context) {
    return {
      statusCode: 429,
      error: "Too Many Requests",
      message: "Limite de requisições excedido. Tente novamente em 1 minuto.",
    };
  },
});

app.setErrorHandler((error, request, reply) => {
  if (error instanceof ZodError) {
    return reply.status(400).send({
      message: "Erro de validação nos dados fornecidos.",
      issues: error.format(),
    });
  }

  if (error.statusCode) {
    return reply.status(error.statusCode).send({
      message: error.message,
    });
  }

  console.error("[Global Error Handler]:", error);

  return reply.status(500).send({
    message: "Ocorreu um erro interno no servidor.",
  });
});

const rawOrigins = [env.FRONTEND_URL, env.CORS_ORIGIN]
  .filter(Boolean)
  .join(",");

const allowedOrigins = Array.from(
  new Set(
    rawOrigins
      .split(",")
      .map((origin) => origin.trim())
      .filter(Boolean),
  ),
);

app.register(cors, {
  origin: allowedOrigins,
  credentials: true,
  methods: ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "Cookie"],
});

app.register(cookie);

app.register(fastifyJwt, {
  secret: env.JWT_SECRET,
});

app.get("/health", async () => {
  return { status: "ok" };
});

app.register(transactionsRoutes, { prefix: "transactions" });
app.register(usersRoutes, { prefix: "users" });
app.register(creditRoutes, { prefix: "credit" });
app.register(accountsRoutes, { prefix: "accounts" });
app.register(categoriesRoutes, { prefix: "categories" });
app.register(dashboardRoutes, { prefix: "/dashboard" });
app.register(subscriptionsRoutes, { prefix: "subscriptions" });
app.register(walletsRoutes, { prefix: "/wallets" });
app.register(analyticsRoutes, { prefix: "/analytics" });
app.register(financialEventsRoutes, { prefix: "/financial-events" });
