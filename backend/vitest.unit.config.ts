import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["src/tests/phase0-unit.spec.ts"],
    env: {
      NODE_ENV: "test",
    },
  },
});
