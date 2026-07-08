import fastify from "fastify";
import cors from "@fastify/cors"; // 1. Importação adicionada
import { env } from "./env/index.js";
import cookie from "@fastify/cookie";
import fastifyJwt from "@fastify/jwt";
import { usersRoutes } from "./routes/users.js";
import { creditRoutes } from "./routes/credit.js";
import { dashboardRoutes } from "./routes/dashboard.js";
import { accountsRoutes } from "./routes/accounts.js";
import { categoriesRoutes } from "./routes/categories.js";
import { transactionsRoutes } from "./routes/transactions.js";
import { subscriptionsRoutes } from "./routes/subscriptions.js";
import { walletsRoutes } from "./routes/wallets.js";
import { analyticsRoutes } from "./routes/analytics.js";

export const app = fastify();

// 2. Configuração do CORS (Sempre antes das rotas!)
app.register(cors, {
  origin: ["http://localhost:5173"],
  credentials: true,
  methods: ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "Cookie"],
});

app.register(cookie);

app.register(transactionsRoutes, {
  prefix: "transactions",
});

app.register(fastifyJwt, {
  secret: process.env.JWT_SECRET || "supersecret",
});

app.register(usersRoutes, {
  prefix: "users",
});

app.register(creditRoutes, {
  prefix: "credit",
});

app.register(accountsRoutes, {
  prefix: "accounts",
});

app.register(categoriesRoutes, {
  prefix: "categories",
});

app.register(dashboardRoutes, {
  prefix: "/dashboard",
});

app.register(subscriptionsRoutes, {
  prefix: "/subscriptions",
});

app.register(walletsRoutes, {
  prefix: "/wallets",
});

app.register(analyticsRoutes, {
  prefix: "/analytics",
});
