import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    setupFiles: ["./src/tests/setup.ts"],
    poolOptions: {
      threads: {
        singleThread: true,
      },
    },
    env: {
      NODE_ENV: "test",
    },
  },
});
