import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import { api } from "../api/client";
import toast from "react-hot-toast";
import { useForm } from "react-hook-form";
import { ShieldCheck } from "lucide-react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useSearchParams, useNavigate } from "react-router-dom";

const resetSchema = z
  .object({
    password: z.string().min(6, "A nova senha deve ter no mínimo 6 caracteres"),
    confirmPassword: z.string().min(1, "Confirme sua nova senha"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "As senhas não coincidem",
    path: ["confirmPassword"],
  });

type ResetForm = z.infer<typeof resetSchema>;

export function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetForm>({
    resolver: zodResolver(resetSchema),
  });

  async function onSubmit(data: ResetForm) {
    if (!token) {
      toast.error("Token de recuperação inválido ou ausente.");
      return;
    }
    try {
      setIsLoading(true);
      await api.post("/users/reset-password", {
        token,
        password: data.password,
      });
      toast.success("Senha redefinida com sucesso!");
      navigate("/login");
    } catch (error) {
      if (axios.isAxiosError(error)) {
        toast.error(
          error.response?.data?.message || "Token inválido ou expirado.",
        );
      } else {
        toast.error("Ocorreu um erro inesperado.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen bg-background flex flex-col justify-center items-center">
        <h2 className="text-xl font-bold text-primary mb-4">
          Link inválido ou expirado.
        </h2>
        <Link to="/forgot-password" className="text-brand hover:underline">
          Solicitar novo link
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-300">
      <div className="mt-8 w-full sm:mx-auto sm:max-w-md">
        <div className="bg-surface py-8 px-6 shadow-xl rounded-3xl border border-subtle transition-colors duration-300">
          <div className="flex flex-col items-center mb-6">
            <div className="p-4 bg-brand/10 text-brand rounded-full mb-4">
              <ShieldCheck size={32} />
            </div>
            <h2 className="text-center text-2xl font-bold tracking-tight text-primary">
              Criar Nova Senha
            </h2>
          </div>

          <form
            className="space-y-6 animate-fade-in"
            onSubmit={handleSubmit(onSubmit)}
          >
            <div>
              <label className="block text-sm font-medium text-secondary">
                Nova Senha
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
            <div>
              <label className="block text-sm font-medium text-secondary">
                Confirmar Nova Senha
              </label>
              <div className="mt-1">
                <input
                  type="password"
                  {...register("confirmPassword")}
                  className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
                />
                {errors.confirmPassword && (
                  <span className="text-red-500 text-xs mt-1 block">
                    {errors.confirmPassword.message}
                  </span>
                )}
              </div>
            </div>
            <button
              type="submit"
              disabled={isLoading}
              className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 disabled:opacity-70 transition-colors"
            >
              {isLoading ? "Salvando..." : "Redefinir Senha"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
