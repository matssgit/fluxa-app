import { api } from "../api/client";

export interface User {
  id: string;
  name: string;
  email: string;
}

export interface LoginCredentials {
  email: string;
  password?: string;

  twoFactorType?: "totp" | "recovery";
  twoFactorCode?: string;
  tempToken?: string;
}

export interface RegisterCredentials {
  name: string;
  email: string;
  password?: string;
}

export type LoginResponse =
  | { token: string; user: User }
  | { requiresTwoFactor: true; tempToken: string };

export async function login(data: LoginCredentials): Promise<LoginResponse> {
  const response = await api.post("/users/login", data);
  return response.data;
}

export async function register(data: RegisterCredentials): Promise<void> {
  await api.post("/users/register", data);
}

export async function verify2FA(
  token: string,
  tempToken: string,
): Promise<{ token: string; user: User }> {
  const response = await api.post(
    "/users/2fa/verify",
    { token },
    { headers: { Authorization: `Bearer ${tempToken}` } },
  );
  return response.data;
}

export async function recovery2FA(
  recoveryCode: string,
  tempToken: string,
): Promise<{ token: string; user: User }> {
  const response = await api.post(
    "/users/2fa/recovery",
    { recoveryCode },
    { headers: { Authorization: `Bearer ${tempToken}` } },
  );
  return response.data;
}
