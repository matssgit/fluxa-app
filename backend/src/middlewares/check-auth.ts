import type { FastifyReply, FastifyRequest } from "fastify";
import { db } from "../database/database.js";

export async function checkAuth(request: FastifyRequest, reply: FastifyReply) {
  try {
    const payload = await request.jwtVerify<{
      sub: string;
      type?: "access" | "2fa_partial";
      tokenVersion?: number;
    }>();

    if (
      payload.type === "2fa_partial" ||
      typeof payload.sub !== "string" ||
      typeof payload.tokenVersion !== "number" ||
      !Number.isInteger(payload.tokenVersion)
    ) {
      return reply.status(401).send({
        error: "Unauthorized. Token invalid or revoked.",
      });
    }

    const user = await db("users")
      .select("token_version")
      .where({ id: payload.sub })
      .first();

    if (!user || user.token_version !== payload.tokenVersion) {
      return reply.status(401).send({
        error: "Unauthorized. Token invalid or revoked.",
      });
    }

    request.user = {
      sub: payload.sub,
      id: payload.sub,
      tokenVersion: payload.tokenVersion,
    };
  } catch (err) {
    return reply.status(401).send({
      error: "Unauthorized. Token invalid or missing.",
    });
  }
}
