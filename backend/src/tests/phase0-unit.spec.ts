import { describe, expect, it } from "vitest";
import { explicitBooleanSchema } from "../env/parse-boolean.js";
import { parseCardIds } from "../utils/financial-event-filters.js";
import { assertSafeTestDatabase } from "./test-database-guard.js";

describe("Phase 0 hardening - unit", () => {
  it("parses explicit boolean environment values safely", () => {
    expect(explicitBooleanSchema.default("false").parse(undefined)).toBe(false);
    expect(explicitBooleanSchema.parse("true")).toBe(true);
    expect(explicitBooleanSchema.parse("false")).toBe(false);
    expect(() => explicitBooleanSchema.parse("1")).toThrow();
  });

  it("accepts one or multiple valid card UUIDs", () => {
    const first = "018f47a8-7b62-7d00-8000-000000000001";
    const second = "018f47a8-7b62-7d00-8000-000000000002";

    expect(parseCardIds(first)).toEqual([first]);
    expect(parseCardIds(`${first},${second}`)).toEqual([first, second]);
  });

  it("rejects malicious cardIds before query construction", () => {
    expect(() => parseCardIds("x') OR TRUE --")).toThrow();
  });

  it("allows only an explicit local test database", () => {
    expect(() =>
      assertSafeTestDatabase(
        "postgresql://user:password@localhost:5432/financeapp_test",
        "pg",
      ),
    ).not.toThrow();

    expect(() =>
      assertSafeTestDatabase(
        "postgresql://user:password@db.example.com:5432/financeapp_test",
        "pg",
      ),
    ).toThrow();
    expect(() =>
      assertSafeTestDatabase(
        "postgresql://user:password@localhost:5432/fluxa",
        "pg",
      ),
    ).toThrow();
  });
});
