import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "../hooks/useAuth";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowLeft,
  ArrowLeftRight,
  ShieldAlert,
  ShieldCheck,
  KeyRound,
  MailWarning,
  TrendingUp,
} from "lucide-react";
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
  const location = useLocation();

  const [step, setStep] = useState<LoginStep>("credentials");
  const [tempToken, setTempToken] = useState<string | null>(null);
  const [savedEmail, setSavedEmail] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [resendSuccess, setResendSuccess] = useState("");

  const prefilledEmail = location.state?.email || "";

  const {
    register: registerLogin,
    handleSubmit: handleLoginSubmit,
    getValues,
    formState: { errors: loginErrors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: prefilledEmail,
      password: "",
    },
  });

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

  const handleForgotPassword = (e: React.MouseEvent) => {
    e.preventDefault();
    const currentEmail = getValues("email");
    navigate("/forgot-password", { state: { email: currentEmail } });
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
    <div className="min-h-screen bg-background relative overflow-hidden transition-colors duration-300">
      <div className="absolute -top-48 -right-32 w-112 h-112 rounded-full bg-brand/6 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 left-1/3 w-96 h-96 rounded-full bg-accent/6 blur-3xl pointer-events-none" />

      <div className="relative z-10 min-h-screen grid lg:grid-cols-[1.18fr_0.82fr]">
        <aside className="hidden lg:flex m-4 mr-0 rounded-[2.25rem] bg-[#173f36] text-[#fffdf7] p-12 xl:p-16 relative overflow-hidden flex-col justify-between shadow-md">
          <div className="absolute -right-28 -top-24 w-112 h-112 rounded-full border-[70px] border-white/5" />
          <div className="absolute -left-28 bottom-24 w-80 h-80 rounded-full border-[44px] border-[#829b88]/12" />
          <div className="absolute right-20 bottom-1/3 w-40 h-64 rounded-[50%] bg-[#829b88]/8 rotate-35" />

          <div className="relative flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-white/10 border border-white/10 flex items-center justify-center">
              <ArrowLeftRight size={21} strokeWidth={2} />
            </div>
            <div>
              <div className="text-2xl font-extrabold tracking-[-0.05em]">
                Fluxa
              </div>
              <div className="text-[9px] uppercase tracking-[0.24em] text-[#dfe8de]/70 font-bold">
                vida financeira
              </div>
            </div>
          </div>

          <div className="relative max-w-xl">
            <span className="text-xs uppercase tracking-[0.22em] text-[#b9cdbf] font-bold">
              Clareza em movimento
            </span>
            <h1 className="mt-5 text-5xl xl:text-6xl leading-[1.04] font-extrabold tracking-[-0.06em]">
              Sua vida financeira,
              <span className="block text-[#a9c8b7]">mais leve e fluida.</span>
            </h1>
            <p className="mt-6 text-base text-[#dfe8de]/75 leading-relaxed max-w-md">
              Organize decisões, acompanhe seu fluxo e construa tranquilidade
              com uma visão clara do seu dinheiro.
            </p>
          </div>

          <div className="relative grid grid-cols-[1.2fr_0.8fr] gap-4">
            <div className="rounded-3xl bg-white/8 border border-white/10 p-6 backdrop-blur-sm">
              <div className="flex items-center justify-between text-xs text-[#dfe8de]/70">
                <span>Saldo disponível</span>
                <TrendingUp size={17} />
              </div>
              <div className="mt-5 text-3xl font-extrabold tracking-[-0.04em] tabular-nums">
                R$ 8.420,50
              </div>
              <div className="mt-5 h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full w-3/4 rounded-full bg-[#9fbead]" />
              </div>
            </div>
            <div className="rounded-3xl bg-[#f4f0e6] text-[#173f36] p-6 flex flex-col justify-between">
              <ShieldCheck size={22} />
              <div>
                <div className="text-2xl font-extrabold">Protegido</div>
                <div className="text-xs text-[#52635d] mt-1">
                  Segurança em cada acesso
                </div>
              </div>
            </div>
          </div>
        </aside>

        <section className="flex flex-col justify-center py-10 px-4 sm:px-8 lg:px-12 xl:px-16">
          <div className="lg:hidden flex items-center justify-center gap-3 mb-7">
            <div className="w-10 h-10 rounded-2xl bg-brand text-white dark:text-[#102f29] flex items-center justify-center">
              <ArrowLeftRight size={19} />
            </div>
            <span className="text-2xl font-extrabold tracking-[-0.05em] text-primary">
              Fluxa
            </span>
          </div>

      <div className="w-full sm:mx-auto sm:max-w-md">
        {step === "credentials" && (
          <h2 className="text-center lg:text-left text-3xl sm:text-4xl font-extrabold tracking-[-0.05em] text-primary">
            Que bom ter você aqui.
          </h2>
        )}
        {step === "two_factor" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-brand/10 text-brand rounded-full mb-4">
              <ShieldAlert size={32} />
            </div>
            <h2 className="text-center text-3xl font-extrabold tracking-[-0.04em] text-primary">
              Verificação em duas etapas
            </h2>
          </div>
        )}
        {step === "recovery" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-amber-500/10 text-amber-500 rounded-full mb-4">
              <KeyRound size={32} />
            </div>
            <h2 className="text-center text-3xl font-extrabold tracking-[-0.04em] text-primary">
              Código de recuperação
            </h2>
          </div>
        )}
        {step === "unverified" && (
          <div className="flex flex-col items-center mt-6">
            <div className="p-4 bg-blue-500/10 text-blue-500 rounded-full mb-4">
              <MailWarning size={32} />
            </div>
            <h2 className="text-center text-3xl font-extrabold tracking-[-0.04em] text-primary">
              Verifique seu e-mail
            </h2>
          </div>
        )}
      </div>

      <div className="mt-7 w-full sm:mx-auto sm:max-w-md">
        <div className="bg-surface py-8 sm:py-10 px-6 sm:px-9 shadow-md rounded-tl-[2.75rem] rounded-br-[2.75rem] rounded-tr-xl rounded-bl-xl border border-border transition-colors duration-300 relative overflow-hidden">
          <div className="absolute -right-12 -top-14 w-36 h-36 rounded-full border-[22px] border-brand/4 pointer-events-none" />
          <div className="relative">
          {error && (
            <div className="mb-6 p-3.5 bg-expense-soft text-expense text-sm rounded-2xl text-center font-medium">
              {error}
            </div>
          )}

          {resendSuccess && (
            <div className="mb-6 p-3.5 bg-income-soft text-income text-sm rounded-2xl text-center font-medium">
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
                    className="block w-full min-h-12 appearance-none rounded-2xl border border-border px-4 py-3 text-primary placeholder:text-muted sm:text-sm bg-surface-muted transition-all"
                  />
                  {loginErrors.email && (
                    <span className="text-red-500 text-xs mt-1 block">
                      {loginErrors.email.message}
                    </span>
                  )}
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="block text-sm font-medium text-secondary">
                    Senha
                  </label>
                  <button
                    type="button"
                    onClick={handleForgotPassword}
                    className="text-xs font-medium text-brand hover:opacity-80 transition-opacity cursor-pointer"
                  >
                    Esqueci minha senha?
                  </button>
                </div>
                <div>
                  <input
                    type="password"
                    {...registerLogin("password")}
                    className="block w-full min-h-12 appearance-none rounded-2xl border border-border px-4 py-3 text-primary placeholder:text-muted sm:text-sm bg-surface-muted transition-all"
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
                className="flex w-full min-h-12 items-center justify-center rounded-full bg-brand py-3 px-4 text-sm font-bold text-white dark:text-[#102f29] shadow-sm hover:bg-brand-light focus-visible:ring-2 focus-visible:ring-brand/40 disabled:opacity-70 transition-all"
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
                  className="block w-full text-center tracking-[0.5em] font-mono text-xl appearance-none rounded-2xl border border-border px-4 py-4 text-primary bg-surface-muted transition-all"
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
                className="flex w-full min-h-12 items-center justify-center rounded-full bg-brand py-3 px-4 text-sm font-bold text-white dark:text-[#102f29] shadow-sm hover:bg-brand-light disabled:opacity-70 transition-all"
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
                  className="block w-full text-center font-mono text-sm appearance-none rounded-2xl border border-border px-4 py-4 text-primary bg-surface-muted transition-all"
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
                className="flex w-full min-h-12 items-center justify-center rounded-full bg-brand py-3 px-4 text-sm font-bold text-white dark:text-[#102f29] shadow-sm hover:bg-brand-light disabled:opacity-70 transition-all"
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
                className="flex w-full min-h-12 items-center justify-center rounded-full bg-brand py-3 px-4 text-sm font-bold text-white dark:text-[#102f29] shadow-sm hover:bg-brand-light disabled:opacity-70 transition-all"
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
        </section>
      </div>
    </div>
  );
}
