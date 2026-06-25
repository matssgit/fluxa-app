import axios from "axios";

export const api = axios.create({
   baseURL: "http://localhost:3333",
   withCredentials: true, // Mantém o cookie funcionando
});

// Tratamento de erros global 
api.interceptors.response.use(
   (response) => response,
   (error) => {
      if (error.response) {
         const status = error.response.status;
         if (status === 400)
            console.error("Erro de validação nos dados enviados.");
         if (status === 401) console.error("Não autorizado. Sessão inválida.");
         if (status === 404) console.error("Recurso não encontrado.");
         if (status >= 500) console.error("Erro interno do servidor backend.");
      } else {
         console.error("Erro de conexão com a API.");
      }
      return Promise.reject(error);
   },
);

// Interceptor para injetar o Token em todas as requisições
api.interceptors.request.use((config) => {
   const token = localStorage.getItem("@FinanceApp:token");

   if (token) {
      config.headers.Authorization = `Bearer ${token}`;
   }

   return config;
});
