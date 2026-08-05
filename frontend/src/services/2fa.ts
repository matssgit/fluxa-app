import { api } from "../api/client";

export const generate2FA = async () => {
  const { data } = await api.post<{ qrCodeUrl: string; secret: string }>(
    "/users/2fa/generate",
  );
  return data;
};

export const enable2FA = async (token: string, secret: string) => {
  const { data } = await api.post<{ message: string; recoveryCodes: string[] }>(
    "/users/2fa/enable",
    {
      token,
      secret,
    },
  );
  return data;
};

export const disable2FA = async (password: string) => {
  const { data } = await api.post<{ message: string }>("/users/2fa/disable", {
    password,
  });
  return data;
};
