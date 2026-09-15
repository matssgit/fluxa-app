import "@fastify/jwt";

declare module "@fastify/jwt" {
  interface FastifyJWT {
    payload: {
      sub: string;
      type?: "access" | "2fa_partial";
      tokenVersion?: number;
    };
    user: {
      sub: string;
      id: string;
      tokenVersion: number;
    };
  }
}
