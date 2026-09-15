import { z } from "zod";

export const explicitBooleanSchema = z
  .enum(["true", "false"])
  .transform((value) => value === "true");

