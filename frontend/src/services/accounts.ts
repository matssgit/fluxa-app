import { api } from "../api/client";

export interface Account {
  id: string;
  name: string;
  type: string;
}

interface CreateAccountData {
  name: string;
  type: string;
}

export async function getAccounts(): Promise<Account[]> {
  const response = await api.get("/accounts");
  return response.data.accounts;
}

export async function createAccount(data: CreateAccountData): Promise<void> {
  await api.post("/accounts", data);
}

export async function updateAccount(data: {
  id: string;
  name: string;
  type: string;
}): Promise<void> {
  await api.put(`/accounts/${data.id}`, { name: data.name, type: data.type });
}

export async function deleteAccount(id: string): Promise<void> {
  await api.delete(`/accounts/${id}`);
}
