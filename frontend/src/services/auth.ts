import { api } from "../api/client";

export interface User {
   id: string;
   name: string;
   email: string;
}

export interface LoginCredentials {
   email: string;
   password?: string;
}

export interface RegisterCredentials {
   name: string;
   email: string;
   password?: string;
}

export async function login(
   data: LoginCredentials,
): Promise<{ token: string; user: User }> {
   const response = await api.post("/users/login", data);
   return response.data;
}

export async function register(data: RegisterCredentials): Promise<void> {
   await api.post("/users/register", data);
}

export async function syncData(): Promise<void> {
   await api.post("/transactions/sync");
}
