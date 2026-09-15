import { app } from "../../app.js";

export function generateTestToken(
  userId: string,
  type: "access" | "2fa_partial" = "access",
  tokenVersion = 0,
): string {
  return app.jwt.sign({ sub: userId, type, tokenVersion });
}
