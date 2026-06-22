import fastify from "fastify";
import cookie from "@fastify/cookie";
import cors from "@fastify/cors"; // 1. Importação adicionada
import { env } from "./env/index.js";
import { transactionsRoutes } from "./routes/transactions.js";

export const app = fastify();

// 2. Configuração do CORS (Sempre antes das rotas!)
app.register(cors, {
   origin: "http://localhost:5173", // Endereço do seu front-end Vite
   credentials: true, // Permite que os cookies (sessionId) transitem entre as portas
});

app.register(cookie);

// GET, POST, PUT, PATCH, DELETE

app.register(transactionsRoutes, {
   prefix: "transactions",
});
