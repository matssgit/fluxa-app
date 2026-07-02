import { defineConfig } from "vitest/config";

export default defineConfig({
   test: {
      // Define qual arquivo vai rodar antes de todos os testes
      setupFiles: ["./src/tests/setup.ts"],
      // Garante que os testes rodem em sequência para o banco não bugar
      poolOptions: {
         threads: {
            singleThread: true,
         },
      },
   },
});
