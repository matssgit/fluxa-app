import fastify from "fastify";
import cookie from "@fastify/cookie";
import cors from "@fastify/cors"; // 1. Importação adicionada
import { env } from "./env/index.js";
import { transactionsRoutes } from "./routes/transactions.js";

export const app = fastify();

// 2. Configuração do CORS (Sempre antes das rotas!)
app.register(cors, {
   origin: (origin, cb) => {
      // Lista de domínios permitidos
      const allowedOrigins = [
         "http://localhost:5173",
         "https://finance-app-beta-ijfg.vercel.app", // URL atual do seu deploy da Vercel
      ];

      // Se a requisição não tiver origin (ex: Postman/Insomnia) ou estiver na lista, permite
      if (!origin || allowedOrigins.includes(origin)) {
         cb(null, true);
         return;
      }

      cb(new Error("Not allowed by CORS"), false);
   },
   credentials: true,
});

app.register(cookie);

// GET, POST, PUT, PATCH, DELETE

app.register(transactionsRoutes, {
   prefix: "transactions",
});
