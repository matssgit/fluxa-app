import type { FastifyReply, FastifyRequest } from "fastify";

export async function checkAuth(request: FastifyRequest, reply: FastifyReply) {
  try {
    const payload = await request.jwtVerify<{
      sub: string;
      type?: "access" | "2fa_partial";
    }>();

    if (payload.type === "2fa_partial") {
      return reply.status(401).send({
        error: "Unauthorized. 2FA verification required.",
      });
    }

    request.user = {
      sub: payload.sub,
      id: payload.sub,
    };
  } catch (err) {
    return reply.status(401).send({
      error: "Unauthorized. Token invalid or missing.",
    });
  }
}
