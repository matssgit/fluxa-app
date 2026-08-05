import { app } from "./app.js";
import { env } from "./env/index.js";

const start = async () => {
  try {
    await app.listen({
      port: env.PORT,
      host: "0.0.0.0",
    });
    console.log(`Servidor rodando na porta ${env.PORT}`);
  } catch (err) {
    console.error("Erro fatal ao iniciar o servidor:", err);
    process.exit(1);
  }
};

start();
