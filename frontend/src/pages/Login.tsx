import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAuth } from "../hooks/useAuth";
import { useNavigate } from "react-router-dom";

const loginSchema = z.object({
   email: z.string().email("E-mail inválido"),
   password: z.string().min(6, "A senha deve ter no mínimo 6 caracteres"),
});

type LoginForm = z.infer<typeof loginSchema>;

export function Login() {
   const { signIn } = useAuth();
   const navigate = useNavigate();
   const [isLoading, setIsLoading] = useState(false);
   const [error, setError] = useState("");

   const {
      register,
      handleSubmit,
      formState: { errors },
   } = useForm<LoginForm>({
      resolver: zodResolver(loginSchema),
   });

   async function onSubmit(data: LoginForm) {
      try {
         setIsLoading(true);
         setError("");
         await signIn(data);
         navigate("/"); // Redireciona para o Dashboard após logar
      } catch {
         setError("Credenciais inválidas. Tente novamente.");
      } finally {
         setIsLoading(false);
      }
   }

   return (
      <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
         <div className="sm:mx-auto sm:w-full sm:max-w-md">
            <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-slate-900">
               Acesse sua conta
            </h2>
         </div>

         <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="bg-white py-8 px-4 shadow-sm sm:rounded-2xl sm:px-10 border border-slate-100">
               <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                  {error && (
                     <div className="p-3 bg-red-50 text-red-500 text-sm rounded-xl text-center">
                        {error}
                     </div>
                  )}

                  <div>
                     <label className="block text-sm font-medium text-slate-700">
                        E-mail
                     </label>
                     <div className="mt-1">
                        <input
                           type="email"
                           {...register("email")}
                           className="block w-full appearance-none rounded-xl border border-slate-200 px-4 py-3 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm bg-slate-50 focus:bg-white transition-colors"
                        />
                        {errors.email && (
                           <span className="text-red-500 text-xs mt-1 block">
                              {errors.email.message}
                           </span>
                        )}
                     </div>
                  </div>

                  <div>
                     <label className="block text-sm font-medium text-slate-700">
                        Senha
                     </label>
                     <div className="mt-1">
                        <input
                           type="password"
                           {...register("password")}
                           className="block w-full appearance-none rounded-xl border border-slate-200 px-4 py-3 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm bg-slate-50 focus:bg-white transition-colors"
                        />
                        {errors.password && (
                           <span className="text-red-500 text-xs mt-1 block">
                              {errors.password.message}
                           </span>
                        )}
                     </div>
                  </div>

                  <button
                     type="submit"
                     disabled={isLoading}
                     className="flex w-full justify-center rounded-xl border border-transparent bg-slate-900 py-3 px-4 text-sm font-medium text-white shadow-sm hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2 disabled:opacity-70 transition-colors"
                  >
                     {isLoading ? "Entrando..." : "Entrar"}
                  </button>
               </form>
               <div className="mt-6 text-center">
                  <Link
                     to="/register"
                     className="text-sm font-medium text-emerald-600 hover:text-emerald-500"
                  >
                     Não tem uma conta? Cadastre-se
                  </Link>
               </div>
            </div>
         </div>
      </div>
   );
}
