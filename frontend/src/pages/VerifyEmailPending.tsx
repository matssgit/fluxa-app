import axios from "axios";
import { useState } from "react";
import { api } from "../api/client";
import { MailCheck, ArrowLeft } from "lucide-react";
import { useLocation, Link } from "react-router-dom";

export function VerifyEmailPending() {
  const location = useLocation();

  const email = location.state?.email as string | undefined;

  const [isLoading, setIsLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [error, setError] = useState("");

  async function handleResendEmail() {
    if (!email) return;

    try {
      setIsLoading(true);
      setError("");
      setSuccessMsg("");
      await api.post("/users/resend-verification", { email });
      setSuccessMsg(
        "Um novo link de verificação foi enviado para o seu e-mail.",
      );
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 429) {
        setError("Muitas tentativas. Aguarde 15 minutos e tente novamente.");
      } else {
        setError("Erro ao reenviar o e-mail. Tente novamente.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-300">
      <div className="w-full sm:mx-auto sm:max-w-md">
        <div className="flex flex-col items-center mt-6">
          <div className="p-4 bg-brand/10 text-brand rounded-full mb-4">
            <MailCheck size={32} />
          </div>
          <h2 className="text-center text-2xl font-bold tracking-tight text-primary">
            Cadastro realizado com sucesso!
          </h2>
        </div>
      </div>

      <div className="mt-8 w-full sm:mx-auto sm:max-w-md">
        <div className="bg-surface py-8 px-6 shadow-xl rounded-3xl border border-subtle transition-colors duration-300">
          {error && (
            <div className="mb-6 p-3 bg-red-50 text-red-500 text-sm rounded-xl text-center">
              {error}
            </div>
          )}

          {successMsg && (
            <div className="mb-6 p-3 bg-green-50 text-green-600 text-sm rounded-xl text-center">
              {successMsg}
            </div>
          )}

          <div className="space-y-6 animate-fade-in text-center">
            {email ? (
              <p className="text-sm text-secondary">
                Enviamos um e-mail de confirmação para <br />
                <strong className="text-primary text-base">{email}</strong>
              </p>
            ) : (
              <p className="text-sm text-secondary">
                Enviamos um e-mail de confirmação para o endereço cadastrado.
              </p>
            )}

            <p className="text-sm text-secondary">
              Verifique sua caixa de entrada ou spam. Depois de validar sua
              conta você poderá realizar o login.
            </p>

            {email && (
              <button
                type="button"
                onClick={handleResendEmail}
                disabled={isLoading}
                className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
              >
                {isLoading ? "Enviando..." : "Reenviar e-mail"}
              </button>
            )}

            <div className="mt-6 flex flex-col gap-4 text-center">
              <Link
                to="/login"
                className="text-xs font-medium text-muted flex items-center justify-center gap-1 hover:text-primary transition-colors"
              >
                <ArrowLeft size={14} /> Ir para a página de Login
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
