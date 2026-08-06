import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import { api } from "../../api/client";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { PasswordRequirements } from "../../components/ui/PasswordRequirements";
import {
  PASSWORD_REGEX,
  PASSWORD_ERROR_MESSAGE,
} from "../../utils/password.policy";

const resetPasswordSchema = z
  .object({
    password: z.string().regex(PASSWORD_REGEX, PASSWORD_ERROR_MESSAGE),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "As senhas não coincidem.",
    path: ["confirmPassword"],
  });

type ResetPasswordData = z.infer<typeof resetPasswordSchema>;

export function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [apiError, setApiError] = useState("");

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ResetPasswordData>({
    resolver: zodResolver(resetPasswordSchema),
    mode: "onChange",
  });

  const currentPassword = useWatch({
    control,
    name: "password",
    defaultValue: "",
  });

  const onSubmit = async (data: ResetPasswordData) => {
    if (!token) return;
    setIsLoading(true);
    setApiError("");

    try {
      await api.post("/users/reset-password", {
        token,
        newPassword: data.password,
      });
      setSuccess(true);
    } catch (error: unknown) {
      if (axios.isAxiosError(error) && error.response?.data?.message) {
        setApiError(error.response.data.message);
      } else {
        setApiError("Ocorreu um erro ao redefinir a senha.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-page p-4 transition-colors duration-300">
        <div className="w-full max-w-md card-default text-center animate-fade-in">
          <h2 className="text-2xl font-bold text-primary mb-4">
            Link Inválido
          </h2>
          <p className="text-secondary mb-6">
            O token de recuperação não foi encontrado ou é inválido.
          </p>
          <Link to="/forgot-password">
            <button className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 transition-all">
              Solicitar novo link
            </button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-page p-4 transition-colors duration-300">
      <div className="w-full max-w-md card-default animate-fade-in">
        <h2 className="text-2xl font-bold text-center text-primary mb-6">
          Criar Nova Senha
        </h2>

        {success ? (
          <div className="text-center flex flex-col items-center gap-6 animate-fade-in">
            <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-surface shadow-sm text-brand mb-2 border border-subtle">
              <span className="text-2xl">✓</span>
            </div>
            <p className="text-secondary text-base">
              Sua senha foi redefinida com sucesso. Você já pode acessar sua
              conta.
            </p>
            <button
              onClick={() => navigate("/login")}
              className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand transition-all"
            >
              Ir para Login
            </button>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-6 animate-fade-in"
          >
            {apiError && (
              <div className="p-3 bg-danger/10 text-danger text-sm rounded-xl text-center">
                {apiError}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <input
                  type="password"
                  placeholder="Nova senha"
                  {...register("password")}
                  disabled={isLoading}
                  className={`w-full px-4 py-3 bg-surface border rounded-xl text-primary focus:outline-none focus:ring-2 focus:ring-brand transition-colors ${
                    errors.password
                      ? "border-danger focus:ring-danger"
                      : "border-subtle"
                  }`}
                />
              </div>

              {/* Componente injetado alimentado pelo hook isolado */}
              <PasswordRequirements password={currentPassword} />

              <div>
                <input
                  type="password"
                  placeholder="Confirme a nova senha"
                  {...register("confirmPassword")}
                  disabled={isLoading}
                  className={`w-full px-4 py-3 bg-surface border rounded-xl text-primary focus:outline-none focus:ring-2 focus:ring-brand transition-colors ${
                    errors.confirmPassword
                      ? "border-danger focus:ring-danger"
                      : "border-subtle"
                  }`}
                />
                {errors.confirmPassword && (
                  <p className="text-sm text-danger mt-1">
                    {errors.confirmPassword.message}
                  </p>
                )}
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {isLoading ? "Salvando..." : "Redefinir Senha"}
            </button>

            <div className="text-center mt-4">
              <Link
                to="/login"
                className="text-sm font-medium text-secondary hover:text-primary transition-colors"
              >
                Cancelar e voltar ao Login
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
