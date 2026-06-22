import { z } from "zod";

export const transactionSchema = z.object({
   title: z.string().min(3, "O título deve ter no mínimo 3 caracteres"),
   amount: z.number().positive("O valor deve ser maior que zero"),
   type: z.enum(["credit", "debit"]),
});

export type TransactionFormData = z.infer<typeof transactionSchema>;
