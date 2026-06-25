import type { FastifyReply, FastifyRequest } from "fastify";

export async function checkAuth(request: FastifyRequest, reply: FastifyReply) {
   try {
      // O TypeScript agora sabe que o retorno tem um 'sub'
      const payload = await request.jwtVerify<{ sub: string }>();

      // Injetando no request.user para as rotas usarem
      request.user = { sub: payload.sub };
      return;
   } catch {
      const sessionId = request.cookies.sessionId;
      if (!sessionId) {
         return reply
            .status(401)
            .send({ error: "Unauthorized. Token or Session missing." });
      }
   }
}
