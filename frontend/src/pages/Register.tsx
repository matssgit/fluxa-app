import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import { toast } from "react-hot-toast";
import { useAuth } from "../hooks/useAuth";
import { useForm, useWatch } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { PasswordRequirements } from "../components/ui/PasswordRequirements";
import {
  PASSWORD_REGEX,
  PASSWORD_ERROR_MESSAGE,
} from "../utils/password.policy";

const registerSchema = z.object({
  name: z.string().min(2, "O nome é obrigatório"),
  email: z.string().email("E-mail inválido"),
  password: z.string().regex(PASSWORD_REGEX, PASSWORD_ERROR_MESSAGE),
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
    control,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    mode: "onChange",
  });

  const currentPassword = useWatch({
    control,
    name: "password",
    defaultValue: "",
  });

  async function onSubmit(data: RegisterForm) {
    try {
      setIsLoading(true);
      setError("");

      await signUp(data);

      toast.success("Conta criada com sucesso! Bem-vindo(a) ao ecossistema.");
      navigate("/verify-email-pending", { state: { email: data.email } });
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.data) {
        const backendMessage =
          err.response.data.error || err.response.data.message;
        setError(
          backendMessage || "Ocorreu um erro inesperado ao criar a conta.",
        );
      } else {
        setError("Não foi possível conectar ao servidor. Tente novamente.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-page flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-300">
      <div className="w-full sm:mx-auto sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-primary">
          Crie sua conta
        </h2>
      </div>

      <div className="mt-8 w-full sm:mx-auto sm:max-w-md">
        <div className="card-default">
          <form
            className="space-y-6 animate-fade-in"
            onSubmit={handleSubmit(onSubmit)}
          >
            {error && (
              <div className="p-3 bg-danger/10 text-danger text-sm rounded-xl text-center">
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
                  disabled={isLoading}
                  className={`block w-full appearance-none rounded-xl border px-4 py-3 text-primary placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-brand sm:text-sm bg-surface transition-colors ${
                    errors.name
                      ? "border-danger focus:ring-danger"
                      : "border-subtle"
                  }`}
                />
                {errors.name && (
                  <span className="text-danger text-xs mt-1 block">
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
                  disabled={isLoading}
                  className={`block w-full appearance-none rounded-xl border px-4 py-3 text-primary placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-brand sm:text-sm bg-surface transition-colors ${
                    errors.email
                      ? "border-danger focus:ring-danger"
                      : "border-subtle"
                  }`}
                />
                {errors.email && (
                  <span className="text-danger text-xs mt-1 block">
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
                  disabled={isLoading}
                  className={`block w-full appearance-none rounded-xl border px-4 py-3 text-primary placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-brand sm:text-sm bg-surface transition-colors ${
                    errors.password
                      ? "border-danger focus:ring-danger"
                      : "border-subtle"
                  }`}
                />
              </div>
              <div className="mt-4">
                <PasswordRequirements password={currentPassword} />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {isLoading ? "Criando conta..." : "Cadastrar"}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link
              to="/login"
              className="text-sm font-medium text-secondary hover:text-primary transition-colors"
            >
              Já tem uma conta? Faça login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
