import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "../hooks/useAuth";
import { Link, useNavigate } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ShieldAlert, KeyRound, MailWarning } from "lucide-react";
import { api } from "../api/client";

type LoginStep = "credentials" | "two_factor" | "recovery" | "unverified";

const loginSchema = z.object({
  email: z.string().email("E-mail inválido"),
  password: z.string().min(6, "A senha deve ter no mínimo 6 caracteres"),
});

const totpSchema = z.object({
  token: z
    .string()
    .length(6, "O código deve ter exatamente 6 dígitos")
    .regex(/^\d+$/, "Apenas números"),
});

const recoverySchema = z.object({
  recoveryCode: z.string().min(1, "O código de recuperação é obrigatório"),
});

type LoginForm = z.infer<typeof loginSchema>;
type TotpForm = z.infer<typeof totpSchema>;
type RecoveryForm = z.infer<typeof recoverySchema>;

export function Login() {
  const { signIn } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<LoginStep>("credentials");
  const [tempToken, setTempToken] = useState<string | null>(null);
  const [savedEmail, setSavedEmail] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [resendSuccess, setResendSuccess] = useState("");

  const {
    register: registerLogin,
    handleSubmit: handleLoginSubmit,
    formState: { errors: loginErrors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const {
    register: registerTotp,
    handleSubmit: handleTotpSubmit,
    formState: { errors: totpErrors },
    reset: resetTotp,
  } = useForm<TotpForm>({ resolver: zodResolver(totpSchema) });

  const {
    register: registerRecovery,
    handleSubmit: handleRecoverySubmit,
    formState: { errors: recoveryErrors },
    reset: resetRecovery,
  } = useForm<RecoveryForm>({ resolver: zodResolver(recoverySchema) });

  const resetFlow = () => {
    setStep("credentials");
    setTempToken(null);
    setError("");
    setResendSuccess("");
    resetTotp();
    resetRecovery();
  };

  const handleAuthError = (err: unknown) => {
    if (
      axios.isAxiosError<{
        error?: string;
        requiresEmailVerification?: boolean;
      }>(err)
    ) {
      const status = err.response?.status;
      const backendError = err.response?.data?.error;
      const requiresEmail = err.response?.data?.requiresEmailVerification;

      if (status === 403 && requiresEmail) {
        setStep("unverified");
        setError("");
        return;
      }

      if (status === 401 && step !== "credentials") {
        if (backendError === "Invalid 2FA code.") {
          setError("Código inválido. Tente novamente.");
          return;
        }

        if (backendError === "Invalid recovery code.") {
          setError("Código de recuperação inválido ou já utilizado.");
          return;
        }

        setError("Sessão expirada. Por favor, faça login novamente.");
        resetFlow();
        return;
      }

      if (status === 400 && step === "two_factor") {
        setError("Código já utilizado. Aguarde um novo código.");
        return;
      }
    }

    if (step === "two_factor") setError("Código inválido. Tente novamente.");
    else if (step === "recovery")
      setError("Código de recuperação inválido ou já utilizado.");
    else setError("Credenciais inválidas. Tente novamente.");
  };

  async function handleResendEmail() {
    try {
      setIsLoading(true);
      setError("");
      setResendSuccess("");
      await api.post("/users/resend-verification", { email: savedEmail });
      setResendSuccess(
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

  async function onLoginSubmit(data: LoginForm) {
    try {
      setIsLoading(true);
      setError("");
      setResendSuccess("");
      setSavedEmail(data.email);

      const response = await signIn(data);

      if (response?.requiresTwoFactor) {
        setTempToken(response.tempToken);
        setStep("two_factor");
      } else {
        navigate("/");
      }
    } catch (err) {
      handleAuthError(err);
    } finally {
      setIsLoading(false);
    }
  }

  async function onTotpSubmit(data: TotpForm) {
    if (!tempToken) return;
    try {
      setIsLoading(true);
      setError("");
      await signIn({
        email: savedEmail,
        twoFactorType: "totp",
        twoFactorCode: data.token,
        tempToken,
      });
      navigate("/");
    } catch (err) {
      handleAuthError(err);
    } finally {
      setIsLoading(false);
    }
  }

  async function onRecoverySubmit(data: RecoveryForm) {
    if (!tempToken) return;
    try {
      setIsLoading(true);
      setError("");
      await signIn({
        email: savedEmail,
        twoFactorType: "recovery",
        twoFactorCode: data.recoveryCode,
        tempToken,
      });
      navigate("/");
    } catch (err) {
      handleAuthError(err);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-300">
      <div className="w-full sm:mx-auto sm:max-w-md">
        {step === "credentials" && (
          <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-primary">
            Acesse sua conta
          </h2>
        )}
        {step === "two_factor" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-brand/10 text-brand rounded-full mb-4">
              <ShieldAlert size={32} />
            </div>
            <h2 className="text-center text-2xl font-bold tracking-tight text-primary">
              Verificação em duas etapas
            </h2>
          </div>
        )}
        {step === "recovery" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-amber-500/10 text-amber-500 rounded-full mb-4">
              <KeyRound size={32} />
            </div>
            <h2 className="text-center text-2xl font-bold tracking-tight text-primary">
              Código de recuperação
            </h2>
          </div>
        )}
        {step === "unverified" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-blue-500/10 text-blue-500 rounded-full mb-4">
              <MailWarning size={32} />
            </div>
            <h2 className="text-center text-2xl font-bold tracking-tight text-primary">
              Verifique seu e-mail
            </h2>
          </div>
        )}
      </div>

      <div className="mt-8 w-full sm:mx-auto sm:max-w-md">
        <div className="bg-surface py-8 px-6 shadow-xl rounded-3xl border border-subtle transition-colors duration-300">
          {error && (
            <div className="mb-6 p-3 bg-red-50 text-red-500 text-sm rounded-xl text-center">
              {error}
            </div>
          )}

          {resendSuccess && (
            <div className="mb-6 p-3 bg-green-50 text-green-600 text-sm rounded-xl text-center">
              {resendSuccess}
            </div>
          )}

          {step === "credentials" && (
            <form
              className="space-y-6 animate-fade-in"
              onSubmit={handleLoginSubmit(onLoginSubmit)}
            >
              <div>
                <label className="block text-sm font-medium text-secondary">
                  E-mail
                </label>
                <div className="mt-1">
                  <input
                    type="email"
                    {...registerLogin("email")}
                    className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
                  />
                  {loginErrors.email && (
                    <span className="text-red-500 text-xs mt-1 block">
                      {loginErrors.email.message}
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
                    {...registerLogin("password")}
                    className="block w-full appearance-none rounded-xl border border-subtle px-4 py-3 text-primary placeholder:text-muted focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand sm:text-sm bg-background transition-colors"
                  />
                  {loginErrors.password && (
                    <span className="text-red-500 text-xs mt-1 block">
                      {loginErrors.password.message}
                    </span>
                  )}
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
              >
                {isLoading ? "Entrando..." : "Entrar"}
              </button>

              <div className="mt-6 text-center">
                <Link
                  to="/register"
                  className="text-sm font-medium text-brand hover:opacity-80 transition-opacity"
                >
                  Não tem uma conta? Cadastre-se
                </Link>
              </div>
            </form>
          )}

          {step === "two_factor" && (
            <form
              className="space-y-6 animate-fade-in"
              onSubmit={handleTotpSubmit(onTotpSubmit)}
            >
              <p className="text-sm text-center text-secondary mb-4">
                Digite o código de 6 dígitos gerado pelo seu aplicativo
                autenticador.
              </p>

              <div>
                <input
                  type="text"
                  maxLength={6}
                  placeholder="000000"
                  {...registerTotp("token")}
                  className="block w-full text-center tracking-[0.5em] font-mono text-xl appearance-none rounded-xl border border-subtle px-4 py-4 text-primary focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand bg-background transition-colors"
                />
                {totpErrors.token && (
                  <span className="text-red-500 text-xs mt-2 block text-center font-medium">
                    {totpErrors.token.message}
                  </span>
                )}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
              >
                {isLoading ? "Verificando..." : "Verificar"}
              </button>

              <div className="mt-6 flex flex-col gap-4 text-center">
                <button
                  type="button"
                  onClick={() => {
                    setStep("recovery");
                    setError("");
                  }}
                  className="text-sm font-medium text-brand hover:opacity-80 transition-opacity"
                >
                  Usar código de recuperação
                </button>
                <button
                  type="button"
                  onClick={resetFlow}
                  className="text-xs font-medium text-muted flex items-center justify-center gap-1 hover:text-primary transition-colors"
                >
                  <ArrowLeft size={14} /> Voltar para login
                </button>
              </div>
            </form>
          )}

          {step === "recovery" && (
            <form
              className="space-y-6 animate-fade-in"
              onSubmit={handleRecoverySubmit(onRecoverySubmit)}
            >
              <p className="text-sm text-center text-secondary mb-4">
                Digite um dos códigos de recuperação fornecidos quando você
                ativou o 2FA.
              </p>

              <div>
                <input
                  type="text"
                  {...registerRecovery("recoveryCode")}
                  placeholder="Seu código de recuperação"
                  className="block w-full text-center font-mono text-sm appearance-none rounded-xl border border-subtle px-4 py-4 text-primary focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand bg-background transition-colors"
                />
                {recoveryErrors.recoveryCode && (
                  <span className="text-red-500 text-xs mt-2 block text-center font-medium">
                    {recoveryErrors.recoveryCode.message}
                  </span>
                )}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
              >
                {isLoading ? "Confirmando..." : "Confirmar"}
              </button>

              <div className="mt-6 flex flex-col gap-4 text-center">
                <button
                  type="button"
                  onClick={() => {
                    setStep("two_factor");
                    setError("");
                  }}
                  className="text-sm font-medium text-brand hover:opacity-80 transition-opacity"
                >
                  Voltar para código autenticador
                </button>
              </div>
            </form>
          )}

          {step === "unverified" && (
            <div className="space-y-6 animate-fade-in">
              <p className="text-sm text-center text-secondary mb-4">
                Enviamos um link de verificação para o endereço{" "}
                <strong>{savedEmail}</strong>. Por favor, verifique sua caixa de
                entrada ou spam.
              </p>

              <button
                type="button"
                onClick={handleResendEmail}
                disabled={isLoading}
                className="flex w-full justify-center rounded-xl border border-transparent bg-brand py-3 px-4 text-sm font-medium text-white shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 disabled:opacity-70 transition-colors"
              >
                {isLoading ? "Enviando..." : "Reenviar e-mail"}
              </button>

              <div className="mt-6 flex flex-col gap-4 text-center">
                <button
                  type="button"
                  onClick={resetFlow}
                  className="text-xs font-medium text-muted flex items-center justify-center gap-1 hover:text-primary transition-colors"
                >
                  <ArrowLeft size={14} /> Voltar para login
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
