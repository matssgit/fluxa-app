import "@fastify/jwt";

declare module "@fastify/jwt" {
  interface FastifyJWT {
    payload: {
      sub: string;
      type?: "access" | "2fa_partial";
    };
    user: {
      sub: string;
      id: string;
    };
  }
}
