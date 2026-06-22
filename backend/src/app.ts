import fastify from "fastify";
import cookie from "@fastify/cookie";
import cors from "@fastify/cors"; // 1. Importação adicionada
import { env } from "./env/index.js";
import { transactionsRoutes } from "./routes/transactions.js";

export const app = fastify();

// 2. Configuração do CORS (Sempre antes das rotas!)
app.register(cors, {
   origin: [
      "http://localhost:5173",
      "https://finance-app-beta-ijfg.vercel.app",
   ],
   credentials: true,
   methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
   allowedHeaders: ["Content-Type", "Authorization", "Cookie"],
});

app.register(cookie);

// GET, POST, PUT, PATCH, DELETE

app.register(transactionsRoutes, {
   prefix: "transactions",
});
