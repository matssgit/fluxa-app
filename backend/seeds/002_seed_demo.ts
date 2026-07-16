import { Knex } from "knex";
import { randomUUID } from "crypto";

export async function seed(knex: Knex): Promise<void> {
  // 1. Usuário
  const userId = randomUUID();
  await knex("users").insert({
    id: userId,
    name: "Fluxa Demo User",
    email: "demo@fluxa.com",
    password_hash:
      "$2b$12$eX5Y8wU/sQoM7oM.9r/qC.R0N5G7aK2sRjP6a1.9x6sE7tI5bHh7K",
    preferences: JSON.stringify({
      theme: "system",
      privacy: { hide_balance: false },
    }),
  });

  // 2. Conta
  const accountId = randomUUID();
  await knex("accounts").insert({
    id: accountId,
    user_id: userId,
    name: "Nubank",
    type: "checking",
  });

  // 3. Categoria
  const categoryId = randomUUID();
  await knex("categories").insert({
    id: categoryId,
    user_id: userId,
    name: "Mercado",
    icon: "shopping-cart",
    type: "expense",
    is_default: true,
  });

  // 4. Carteira
  const walletId = randomUUID();
  await knex("wallets").insert({
    id: walletId,
    user_id: userId,
    title: "Reserva de Emergência",
    target_amount: 10000.0,
    current_amount: 2500.0,
    status: "active",
    color: "brand",
  });

  // 5. Cartão
  const cardId = randomUUID();
  await knex("cards").insert({
    id: cardId,
    user_id: userId,
    name: "Nubank Platinum",
    brand: "Mastercard",
    due_day: 10,
    total_limit: 5000.0,
    available_limit: 4500.0,
    color: "#820ad1",
  });

  // 6. Transação (Corrigido: session_id como UUID válido)
  await knex("transactions").insert({
    id: randomUUID(),
    user_id: userId,
    account_id: accountId,
    category_id: categoryId,
    title: "Compra Mercado",
    amount: 450.0,
    type: "expense",
    status: "completed",
    session_id: randomUUID(), // <--- CORRIGIDO: Agora é UUID válido
    description: "Compra mensal",
  });

  // 7. Compra Parcelada
  const purchaseId = randomUUID();
  await knex("credit_purchases").insert({
    id: purchaseId,
    user_id: userId,
    card_id: cardId,
    category_id: categoryId,
    title: "Samsung S25",
    store: "Loja Samsung",
    total_amount: 5000.0,
    total_installments: 10,
    purchase_date: new Date(),
    status: "active",
  });

  await knex("installments").insert({
    id: randomUUID(),
    user_id: userId,
    purchase_id: purchaseId,
    installment_number: 1,
    total_installments: 10,
    amount: 500.0,
    expected_date: new Date(),
    status: "pending",
  });

  // 8. Assinatura
  await knex("subscriptions").insert({
    id: randomUUID(),
    user_id: userId,
    category_id: categoryId,
    account_id: accountId,
    title: "Netflix",
    amount: 55.9,
    due_day: 15,
    frequency: "monthly",
    status: "active",
  });

  console.log("✅ Ambiente Demo (Schema-Compliant) criado com sucesso!");
}
