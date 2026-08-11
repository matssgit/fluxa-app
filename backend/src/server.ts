import { DemoCleanupService } from "./services/demo-cleanup.service.js";
import { app } from "./app.js";
import { env } from "./env/index.js";

const start = async () => {
  try {
    await app.listen({
      port: env.PORT,
      host: "0.0.0.0",
    });
    console.log(`Servidor rodando na porta ${env.PORT}`);

    if (env.DEMO_CLEANUP_ENABLED) {
      console.log(
        `[DEMO CLEANUP] Ativo. Retenção: ${env.DEMO_DATA_RETENTION_DAYS} dias.`,
      );

      setTimeout(() => {
        DemoCleanupService.runCleanup();
      }, 10000);

      setInterval(
        () => {
          DemoCleanupService.runCleanup();
        },
        24 * 60 * 60 * 1000,
      );
    }
  } catch (err) {
    console.error("Erro fatal ao iniciar o servidor:", err);
    process.exit(1);
  }
};

start();
