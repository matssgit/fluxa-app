import type { FastifyInstance } from "fastify";
import { z } from "zod";
import bcrypt from "bcrypt";
import { randomUUID } from "node:crypto";
import { db } from "../database.js";

export async function usersRoutes(app: FastifyInstance) {
   // ****** CADASTRO ******
   app.post("/register", async (request, reply) => {
      const registerSchema = z.object({
         name: z.string(),
         email: z.string().email(),
         password: z.string().min(6),
      });

      const { name, email, password } = registerSchema.parse(request.body);

      const userExists = await db("users").where({ email }).first();
      if (userExists)
         return reply.status(400).send({ error: "User already exists" });

      const password_hash = await bcrypt.hash(password, 8);

      await db("users").insert({
         id: randomUUID(),
         name,
         email,
         password_hash,
      });

      return reply.status(201).send();
   });

   // ****** LOGIN ******
   app.post("/login", async (request, reply) => {
      const loginSchema = z.object({
         email: z.string().email(),
         password: z.string(),
      });

      const { email, password } = loginSchema.parse(request.body);

      const user = await db("users").where({ email }).first();
      if (!user)
         return reply.status(400).send({ error: "Invalid credentials" });

      const isPasswordValid = await bcrypt.compare(
         password,
         user.password_hash,
      );
      if (!isPasswordValid)
         return reply.status(400).send({ error: "Invalid credentials" });

      // CORREÇÃO AQUI: Passamos os dados do usuário no Payload (1º parâmetro)
      // Substitua a linha antiga por esta:
      const token = app.jwt.sign({ sub: user.id }, { expiresIn: "7d" });

      return reply.status(200).send({
         token,
         user: { id: user.id, name: user.name, email: user.email },
      });
   });
}
