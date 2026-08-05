import { z } from "zod";
import { useState } from "react";
import { toast } from "react-hot-toast";
import { useForm } from "react-hook-form";
import { useAuth } from "../hooks/useAuth";
import { useNavigate, Link } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";

const registerSchema = z.object({
  name: z.string().min(2, "O nome é obrigatório"),
  email: z.string().email("E-mail inválido"),
  password: z.string().min(6, "A senha deve ter no mínimo 6 caracteres"),
});

type RegisterForm = z.infer<typeof registerSchema>;

export function Register() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  async function onSubmit(data: RegisterForm) {
    try {
      setIsLoading(true);
      setError("");
      await signUp(data);

      toast.success("Conta criada com sucesso! Bem-vindo(a) ao ecossistema.");

      navigate("/verify-email-pending", { state: { email: data.email } });
    } catch {
      setError("Erro ao criar conta. Este e-mail já pode estar em uso.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-300">
      <div className="w-full sm:mx-auto sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-primary">
          Crie sua conta
        </h2>
      </div>

      <div className="mt-8 w-full sm:mx-auto sm:max-w-md">
        <div className="bg-surface py-8 px-6 shadow-xl rounded-3xl border border-subtle transition-colors duration-300">
          <form
            className="space-y-6 animate-fade-in"
            onSubmit={handleSubmit(onSubmit)}
          >
            {error && (
              <div className="mb-6 p-3 bg-red-50 text-red-500 text-sm rounded-xl text-center">
                {error}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-secondary">
                Nome
              </label>
              <div className="mt-1">
                <input
                  type="text"
                  {...register("name")}
                  className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
                />
                {errors.name && (
                  <span className="text-red-500 text-xs mt-1 block">
                    {errors.name.message}
                  </span>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary">
                E-mail
              </label>
              <div className="mt-1">
                <input
                  type="email"
                  {...register("email")}
                  className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
                />
                {errors.email && (
                  <span className="text-red-500 text-xs mt-1 block">
                    {errors.email.message}
                  </span>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary">
                Senha
              </label>
              <div className="mt-1">
                <input
                  type="password"
                  {...register("password")}
                  className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
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
              className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
            >
              {isLoading ? "Criando conta..." : "Cadastrar"}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link
              to="/login"
              className="text-sm font-medium text-brand hover:opacity-80 transition-opacity"
            >
              Já tem uma conta? Faça login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
