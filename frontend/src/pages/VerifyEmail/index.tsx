import axios from "axios";
import { api } from "../../api/client";
import { useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { useSearchParams, useNavigate } from "react-router-dom";

export function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  const [status, setStatus] = useState<"loading" | "success" | "error">(
    token ? "loading" : "error",
  );
  const [errorMessage, setErrorMessage] = useState(
    token ? "" : "Token de verificação não encontrado na URL.",
  );

  useEffect(() => {
    if (!token) return;

    const verifyToken = async () => {
      try {
        await api.get(`/users/verify-email?token=${token}`);
        setStatus("success");
      } catch (error: unknown) {
        setStatus("error");
        if (axios.isAxiosError(error) && error.response?.data?.message) {
          setErrorMessage(error.response.data.message);
        } else {
          setErrorMessage("Link inválido, expirado ou já utilizado.");
        }
      }
    };

    verifyToken();
  }, [token]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-background p-4">
      <div className="w-full max-w-md p-8 bg-surface rounded-2xl shadow-md border border-subtle text-center">
        {status === "loading" && (
          <div className="flex flex-col items-center gap-4">
            <div className="w-8 h-8 border-4 border-primary rounded-full border-t-transparent animate-spin"></div>
            <p className="text-secondary text-base">
              Verificando seu e-mail...
            </p>
          </div>
        )}

        {status === "success" && (
          <div className="flex flex-col items-center gap-6">
            <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-surface shadow-sm text-secondary mb-2">
              <span className="text-2xl">✓</span>
            </div>
            <h2 className="text-2xl font-bold text-primary">
              E-mail verificado!
            </h2>
            <p className="text-base text-secondary">
              Sua conta foi ativada com sucesso.
            </p>
            <Button
              variant="primary"
              onClick={() => navigate("/login")}
              className="w-full mt-2"
            >
              Ir para Login
            </Button>
          </div>
        )}

        {status === "error" && (
          <div className="flex flex-col items-center gap-6">
            <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-surface shadow-sm text-accent mb-2">
              <span className="text-2xl">✕</span>
            </div>
            <h2 className="text-2xl font-bold text-primary">
              Ops! Algo deu errado.
            </h2>
            <p className="text-sm text-secondary">{errorMessage}</p>

            <div className="w-full space-y-4 mt-2">
              <Button
                variant="primary"
                onClick={() => navigate("/resend-verification")}
                className="w-full"
              >
                Solicitar novo e-mail
              </Button>
              <Button
                variant="ghost"
                onClick={() => navigate("/login")}
                className="w-full"
              >
                Voltar
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
