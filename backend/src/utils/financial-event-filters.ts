import { z } from "zod";

const cardIdSchema = z.string().uuid("cardIds deve conter apenas UUIDs válidos");

export function parseCardIds(value: unknown): string[] | undefined {
  if (value === undefined || value === null || value === "") {
    return undefined;
  }

  if (typeof value !== "string") {
    throw new z.ZodError([
      {
        code: "custom",
        path: ["cardIds"],
        message: "cardIds deve ser uma lista de UUIDs separados por vírgula",
      },
    ]);
  }

  return z.array(cardIdSchema).nonempty().parse(value.split(","));
}
