import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { randomUUID } from "node:crypto";
import { app } from "../app.js";
import { db } from "../database/database.js";
import { env } from "../env/index.js";
import { DemoCleanupService } from "../services/demo-cleanup.service.js";
import { FinancialEventsService } from "../services/financial-events.services.js";

describe("Phase 0 hardening - integration", () => {
  const createdUserIds = new Set<string>();
  const originalCleanupEnabled = env.DEMO_CLEANUP_ENABLED;
  const originalRetentionDays = env.DEMO_DATA_RETENTION_DAYS;

  async function createUser(options: {
    isDemo: boolean;
    createdAt: Date;
  }): Promise<string> {
    const id = randomUUID();
    createdUserIds.add(id);

    await db("users").insert({
      id,
      name: "Phase 0 test user",
      email: `${id}@phase0.test`,
      password_hash: "not-used-in-this-test",
      is_demo: options.isDemo,
      created_at: options.createdAt,
    });

    return id;
  }

  beforeEach(() => {
    env.DEMO_CLEANUP_ENABLED = true;
    env.DEMO_DATA_RETENTION_DAYS = 30;
  });

  afterEach(async () => {
    env.DEMO_CLEANUP_ENABLED = originalCleanupEnabled;
    env.DEMO_DATA_RETENTION_DAYS = originalRetentionDays;
    vi.restoreAllMocks();

    if (createdUserIds.size > 0) {
      await db("users").whereIn("id", [...createdUserIds]).delete();
      createdUserIds.clear();
    }
  });

  it("removes only an expired demo user and preserves an old real user", async () => {
    const oldDate = new Date();
    oldDate.setDate(oldDate.getDate() - 31);

    const realUserId = await createUser({ isDemo: false, createdAt: oldDate });
    const demoUserId = await createUser({ isDemo: true, createdAt: oldDate });

    await DemoCleanupService.runCleanup();

    expect(await db("users").where({ id: realUserId }).first()).toBeDefined();
    expect(await db("users").where({ id: demoUserId }).first()).toBeUndefined();
  });

  it("preserves a recent demo user", async () => {
    const demoUserId = await createUser({
      isDemo: true,
      createdAt: new Date(),
    });

    await DemoCleanupService.runCleanup();

    expect(await db("users").where({ id: demoUserId }).first()).toBeDefined();
  });

  it("does not remove users while cleanup is disabled", async () => {
    const oldDate = new Date();
    oldDate.setDate(oldDate.getDate() - 31);
    const demoUserId = await createUser({ isDemo: true, createdAt: oldDate });
    env.DEMO_CLEANUP_ENABLED = false;

    await DemoCleanupService.runCleanup();

    expect(await db("users").where({ id: demoUserId }).first()).toBeDefined();
  });

  it("accepts one or multiple valid cardIds", async () => {
    const userId = await createUser({
      isDemo: false,
      createdAt: new Date(),
    });
    const token = app.jwt.sign({
      sub: userId,
      type: "access",
      tokenVersion: 0,
    });
    const first = randomUUID();
    const second = randomUUID();

    for (const cardIds of [first, `${first},${second}`]) {
      const response = await app.inject({
        method: "GET",
        url: `/financial-events?cardIds=${encodeURIComponent(cardIds)}`,
        headers: { authorization: `Bearer ${token}` },
      });

      expect(response.statusCode).toBe(200);
    }
  });

  it("rejects malicious cardIds before calling the service", async () => {
    const userId = await createUser({
      isDemo: false,
      createdAt: new Date(),
    });
    const token = app.jwt.sign({
      sub: userId,
      type: "access",
      tokenVersion: 0,
    });
    const serviceSpy = vi.spyOn(FinancialEventsService.prototype, "getEvents");

    const response = await app.inject({
      method: "GET",
      url: `/financial-events?cardIds=${encodeURIComponent("x') OR TRUE --")}`,
      headers: { authorization: `Bearer ${token}` },
    });

    expect(response.statusCode).toBe(400);
    expect(serviceSpy).not.toHaveBeenCalled();
  });
});
