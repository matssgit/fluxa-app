import type { FastifyReply, FastifyRequest } from "fastify";

export async function checkAuth(request: FastifyRequest, reply: FastifyReply) {
   try {
      // Tenta validar o JWT
      const payload = await request.jwtVerify<{ sub: string }>();

      request.user = {
         sub: payload.sub,
         id: payload.sub,
      };
   } catch (err) {
      // LOG DE ERRO REAL: Isso vai aparecer no seu terminal backend!
      console.error("🔥 ERRO DE AUTENTICAÇÃO:", err);

      const sessionId = request.cookies.sessionId;
      if (!sessionId) {
         return reply.status(401).send({
            error: "Unauthorized. Token or Session missing.",
            details: err,
         });
      }

      // Se tiver sessionId, você poderia implementar a lógica de sessão aqui
      // mas como estamos mudando para JWT, o ideal é focar no erro do Token.
   }
}
