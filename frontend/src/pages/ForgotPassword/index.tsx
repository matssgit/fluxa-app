import { z } from "zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useLocation } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { api } from "../../api/client";

const forgotPasswordSchema = z.object({
  email: z.string().email("Digite um e-mail válido."),
});

type ForgotPasswordData = z.infer<typeof forgotPasswordSchema>;

export function ForgotPassword() {
  const location = useLocation();
  const defaultEmail = (location.state as { email?: string })?.email || "";

  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: defaultEmail,
    },
  });

  const onSubmit = async (data: ForgotPasswordData) => {
    setIsLoading(true);
    try {
      await api.post("/users/forgot-password", data);
    } catch {
      // Fallback silencioso intencional
    } finally {
      setIsLoading(false);
      setIsSubmitted(true);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-background p-4 transition-colors duration-300">
      <div className="w-full max-w-md p-8 bg-surface rounded-2xl shadow-md border border-subtle">
        <h2 className="text-2xl font-bold text-center text-primary mb-6">
          Recuperar Senha
        </h2>

        {isSubmitted ? (
          <div className="text-center flex flex-col items-center gap-6 animate-fade-in">
            <p className="text-secondary text-base">
              Se existir uma conta para este e-mail, enviaremos um link de
              recuperação.
            </p>
            <Link to="/login" className="w-full">
              <button
                type="button"
                className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-secondary focus:ring-offset-2 transition-all"
              >
                Voltar para o Login
              </button>
            </Link>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-6 animate-fade-in"
          >
            <div className="space-y-2">
              <input
                id="email"
                type="email"
                placeholder="seu.email@exemplo.com"
                {...register("email")}
                disabled={isLoading}
                className={`w-full px-4 py-3 bg-surface border rounded-xl text-primary focus:outline-none focus:ring-2 focus:ring-secondary transition-colors ${
                  errors.email
                    ? "border-accent focus:ring-accent"
                    : "border-subtle"
                }`}
              />
              {errors.email && (
                <p className="text-sm text-accent mt-1">
                  {errors.email.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-secondary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {isLoading ? "Enviando..." : "Enviar link de recuperação"}
            </button>

            <div className="text-center mt-4">
              <Link
                to="/login"
                className="text-sm font-medium text-secondary hover:text-primary transition-colors"
              >
                Lembrou sua senha? Voltar
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
