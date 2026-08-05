import { api } from "../api/client";
import { Button } from "../components/ui/Button";
import { Loading } from "../components/ui/Loading";
import { useEffect, useState, useRef } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";

export function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  const hasCalledAPI = useRef(false);

  const [status, setStatus] = useState<"loading" | "success" | "error">(
    token ? "loading" : "error",
  );

  useEffect(() => {
    if (!token || hasCalledAPI.current) return;

    hasCalledAPI.current = true;

    api
      .get(`/users/verify-email?token=${token}`)
      .then(() => {
        setStatus("success");
      })
      .catch((err) => {
        console.error("Erro ao verificar e-mail:", err);
        setStatus("error");
      });
  }, [token]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-4">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-xl shadow-md text-center">
        {status === "loading" && (
          <>
            <Loading />
            <h2 className="text-xl font-semibold mt-4">
              Verificando seu e-mail...
            </h2>
            <p className="text-gray-500">Por favor, aguarde um momento.</p>
          </>
        )}

        {status === "success" && (
          <>
            <div className="text-green-500 text-5xl mb-4">✅</div>
            <h2 className="text-2xl font-bold">E-mail verificado!</h2>
            <p className="text-gray-500 mb-6">
              Sua conta foi ativada com sucesso. Você já pode acessar o Fluxa.
            </p>
            <Button onClick={() => navigate("/login")} className="w-full">
              Ir para o Login
            </Button>
          </>
        )}

        {status === "error" && (
          <>
            <div className="text-red-500 text-5xl mb-4">❌</div>
            <h2 className="text-2xl font-bold">Link inválido ou expirado</h2>
            <p className="text-gray-500 mb-6">
              Não foi possível verificar seu e-mail. O link pode ter expirado ou
              já foi utilizado.
            </p>
            <Link to="/login" className="text-blue-600 hover:underline">
              Voltar para o Login
            </Link>
          </>
        )}
      </div>
    </div>
  );
}
