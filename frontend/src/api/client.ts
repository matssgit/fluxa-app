import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:3333",
  withCredentials: true,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url;

    const isAuthRoute =
      requestUrl?.includes("/login") ||
      requestUrl?.includes("/register") ||
      requestUrl?.includes("/2fa/");

    if (error.response) {
      const status = error.response.status;

      if (status === 400 && !isAuthRoute)
        console.error("Erro de validação nos dados enviados.");
      if (status === 401 && !isAuthRoute)
        console.error("Não autorizado. Sessão inválida.");
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

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
