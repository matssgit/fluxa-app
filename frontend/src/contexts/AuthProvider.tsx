import { useState, type ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import {
   login as apiLogin,
   register as apiRegister,
   type User,
   type LoginCredentials,
   type RegisterCredentials,
} from "../services/auth";

export function AuthProvider({ children }: { children: ReactNode }) {
   const [user, setUser] = useState<User | null>(() => {
      const token = localStorage.getItem("@FinanceApp:token");
      const storedUser = localStorage.getItem("@FinanceApp:user");

      if (token && storedUser) {
         return JSON.parse(storedUser);
      }
      return null;
   });

   async function signIn(data: LoginCredentials) {
      const { token, user: loggedUser } = await apiLogin(data);

      localStorage.setItem("@FinanceApp:token", token);
      localStorage.setItem("@FinanceApp:user", JSON.stringify(loggedUser));

      setUser(loggedUser);

      try {
         console.log("Dados antigos sincronizados com sucesso!");
      } catch {
         console.log(
            "Nenhum dado antigo para sincronizar ou erro na sincronização.",
         );
      }
   }

   async function signUp(data: RegisterCredentials) {
      await apiRegister(data);
      await signIn({ email: data.email, password: data.password });
   }

   function signOut() {
      localStorage.removeItem("@FinanceApp:token");
      localStorage.removeItem("@FinanceApp:user");
      setUser(null);
   }

   return (
      <AuthContext.Provider
         value={{ user, isAuthenticated: !!user, signIn, signUp, signOut }}
      >
         {children}
      </AuthContext.Provider>
   );
}
