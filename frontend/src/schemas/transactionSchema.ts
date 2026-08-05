import { z } from "zod";

export const transactionSchema = z.object({
  title: z.string().min(1, "O título é obrigatório"),
  amount: z.number().positive("O valor deve ser maior que zero"),
  type: z.enum(["entrada", "saida"]),
  category_id: z.string().uuid("Selecione uma categoria"),
  account_id: z.string().uuid("Selecione uma conta"),
  expected_date: z.string().optional(),
});

export type TransactionFormData = z.infer<typeof transactionSchema>;
