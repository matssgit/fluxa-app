import axios from "axios";
import {
  AUTH_SESSION_CLEARED_EVENT,
  clearClientSession,
} from "../lib/query-client";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:3333",
  withCredentials: true,
});

let isClearingUnauthorizedSession = false;

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url;

    const publicAuthRoutes = [
      "/users/login",
      "/users/register",
      "/users/2fa/verify",
      "/users/2fa/recovery",
      "/users/forgot-password",
      "/users/reset-password",
      "/users/verify-email",
      "/users/resend-verification",
    ];
    const isPublicAuthRoute = publicAuthRoutes.some((route) =>
      requestUrl?.includes(route),
    );

    if (error.response) {
      const status = error.response.status;

      if (status === 400 && !isPublicAuthRoute)
        console.error("Erro de validação nos dados enviados.");
      if (status === 401 && !isPublicAuthRoute) {
        console.error("Não autorizado. Sessão inválida.");
        const hadSession = Boolean(
          localStorage.getItem("@FinanceApp:token") ||
            localStorage.getItem("@FinanceApp:user"),
        );

        if (hadSession && !isClearingUnauthorizedSession) {
          isClearingUnauthorizedSession = true;
          void clearClientSession().finally(() => {
            window.dispatchEvent(new Event(AUTH_SESSION_CLEARED_EVENT));
            isClearingUnauthorizedSession = false;
          });
        }
      }
      if (status === 404) console.error("Recurso não encontrado.");
      if (status >= 500) console.error("Erro interno do servidor backend.");
    } else {
      console.error("Erro de conexão com a API.");
    }
    return Promise.reject(error);
  },
);

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("@FinanceApp:token");

  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
