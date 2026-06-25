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
