import { app } from "../../app.js";

export function generateTestToken(
  userId: string,
  type: "access" | "2fa_partial" = "access",
): string {
  return app.jwt.sign({ sub: userId, type });
}
