import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import toast from "react-hot-toast";
import { api } from "../../api/client";
import { QRCodeSVG } from "qrcode.react";
import { useForm } from "react-hook-form";
import { useAuth } from "../../hooks/useAuth";
import { zodResolver } from "@hookform/resolvers/zod";
import type { User } from "../../contexts/auth/AuthContext";
import { generate2FA, enable2FA, disable2FA } from "../../services/2fa";
import { Key, Shield, ShieldAlert, Copy, AlertTriangle } from "lucide-react";

type TwoFactorStep =
  | "idle"
  | "configuring"
  | "recovery_display"
  | "disable_confirm";

type UserWith2FA = User & { two_factor_enabled?: boolean };

const totpSchema = z.object({
  token: z
    .string()
    .length(6, "O código deve ter exatamente 6 dígitos")
    .regex(/^\d+$/, "Apenas números"),
});

type TotpFormData = z.infer<typeof totpSchema>;

const disableSchema = z.object({
  password: z.string().min(1, "A senha é obrigatória"),
});

type DisableFormData = z.infer<typeof disableSchema>;

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "A senha atual é obrigatória"),
    newPassword: z
      .string()
      .min(6, "A nova senha deve ter no mínimo 6 caracteres"),
    confirmPassword: z.string().min(1, "Confirme a nova senha"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não coincidem",
    path: ["confirmPassword"],
  });

type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

export function SecurityTab() {
  const { user } = useAuth();
  const isCurrentlyEnabled = (user as UserWith2FA)?.two_factor_enabled ?? false;

  const [step, setStep] = useState<TwoFactorStep>("idle");
  const [setupData, setSetupData] = useState<{
    qrCodeUrl: string;
    secret: string;
  } | null>(null);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [is2FALoading, setIs2FALoading] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const {
    register: registerTotp,
    handleSubmit: handleTotpSubmit,
    formState: { errors: totpErrors },
    reset: resetTotp,
  } = useForm<TotpFormData>({ resolver: zodResolver(totpSchema) });

  const {
    register: registerDisable,
    handleSubmit: handleDisableSubmit,
    formState: { errors: disableErrors },
    reset: resetDisable,
  } = useForm<DisableFormData>({ resolver: zodResolver(disableSchema) });

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    formState: { errors: passwordErrors },
    reset: resetPasswordForm,
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
  });

  const handleStartSetup = async () => {
    try {
      setIs2FALoading(true);
      const data = await generate2FA();
      setSetupData(data);
      setStep("configuring");
    } catch {
      toast.error(
        "Erro ao gerar configuração da autenticação em dois fatores.",
      );
    } finally {
      setIs2FALoading(false);
    }
  };

  const handleEnable = async (data: TotpFormData) => {
    if (!setupData?.secret) {
      toast.error("Erro interno: Chave secreta não encontrada.");
      return;
    }

    try {
      setIs2FALoading(true);
      const response = await enable2FA(data.token, setupData.secret);
      setRecoveryCodes(response.recoveryCodes);
      setStep("recovery_display");
      toast.success("Autenticação em dois fatores ativada!");
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const serverMessage =
          error.response?.data?.message ||
          error.response?.data?.error ||
          "Código inválido. Tente novamente.";
        toast.error(`Falha: ${serverMessage}`);
      } else {
        toast.error("Falha ao ativar a autenticação. Tente novamente.");
      }
    } finally {
      setIs2FALoading(false);
      resetTotp();
    }
  };

  const handleDisable = async (data: DisableFormData) => {
    try {
      setIs2FALoading(true);
      await disable2FA(data.password);
      setStep("idle");
      toast.success("Autenticação em dois fatores desativada.");
      resetDisable();
    } catch {
      toast.error("Senha incorreta ou erro ao desativar.");
    } finally {
      setIs2FALoading(false);
    }
  };

  const handleCancel2FA = () => {
    setStep("idle");
    setSetupData(null);
    resetTotp();
    resetDisable();
  };

  const closeRecovery = () => {
    setStep("idle");
    setSetupData(null);
    setRecoveryCodes(null);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success("Copiado para a área de transferência!");
  };

  const handleChangePassword = async (data: ChangePasswordFormData) => {
    try {
      setIsChangingPassword(true);
      await api.put("/users/change-password", {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      toast.success("Senha alterada com sucesso!");
      resetPasswordForm();
    } catch (error) {
      if (axios.isAxiosError(error)) {
        toast.error(
          error.response?.data?.message ||
            "Erro ao alterar a senha. Verifique sua senha atual.",
        );
      } else {
        toast.error("Erro inesperado ao alterar a senha.");
      }
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="space-y-8 animate-fade-in max-w-2xl">
      <div>
        <h2 className="text-lg font-bold text-primary">Segurança</h2>
        <p className="text-xs sm:text-sm text-muted mt-1">
          Proteja sua conta e mantenha suas credenciais seguras.
        </p>
      </div>

      <div className="p-6 rounded-3xl bg-elevated/40 border border-subtle/30">
        <h3 className="text-sm font-bold text-primary mb-4 flex items-center gap-2">
          <Key size={18} className="text-brand" /> Alterar Senha
        </h3>
        <form
          onSubmit={handlePasswordSubmit(handleChangePassword)}
          className="space-y-4 max-w-sm"
        >
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Senha Atual
            </label>
            <input
              type="password"
              {...registerPassword("currentPassword")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm"
            />
            {passwordErrors.currentPassword && (
              <span className="text-red-500 text-xs mt-1 block">
                {passwordErrors.currentPassword.message}
              </span>
            )}
          </div>
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Nova Senha
            </label>
            <input
              type="password"
              {...registerPassword("newPassword")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm"
            />
            {passwordErrors.newPassword && (
              <span className="text-red-500 text-xs mt-1 block">
                {passwordErrors.newPassword.message}
              </span>
            )}
          </div>
          <div>
            <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
              Confirmar Nova Senha
            </label>
            <input
              type="password"
              {...registerPassword("confirmPassword")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm"
            />
            {passwordErrors.confirmPassword && (
              <span className="text-red-500 text-xs mt-1 block">
                {passwordErrors.confirmPassword.message}
              </span>
            )}
          </div>
          <button
            type="submit"
            disabled={isChangingPassword}
            className="mt-2 w-full px-5 py-2.5 bg-brand hover:bg-brand-light text-white text-sm font-bold rounded-xl transition-all shadow-sm disabled:opacity-70"
          >
            {isChangingPassword ? "Atualizando..." : "Atualizar Senha"}
          </button>
        </form>
      </div>

      <div className="p-6 rounded-3xl bg-elevated/40 border border-subtle/30">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div
              className={`p-3 rounded-2xl ${
                isCurrentlyEnabled || step === "recovery_display"
                  ? "bg-brand/10 text-brand"
                  : "bg-elevated text-secondary"
              }`}
            >
              {isCurrentlyEnabled || step === "recovery_display" ? (
                <Shield size={24} />
              ) : (
                <ShieldAlert size={24} />
              )}
            </div>
            <div>
              <h3 className="text-sm font-bold text-primary">
                Autenticação em dois fatores
              </h3>
              <p className="text-[11px] sm:text-xs font-medium text-muted mt-0.5 max-w-sm">
                {isCurrentlyEnabled || step === "recovery_display"
                  ? "Sua conta está protegida. Será exigido um código ao realizar o login."
                  : "Adicione uma camada extra de segurança utilizando um aplicativo autenticador."}
              </p>
            </div>
          </div>

          {step === "idle" && !isCurrentlyEnabled && (
            <button
              onClick={handleStartSetup}
              disabled={is2FALoading}
              className="w-full sm:w-auto px-5 py-2.5 bg-brand hover:bg-brand-light text-white text-sm font-bold rounded-xl transition-all shadow-sm disabled:opacity-70 shrink-0"
            >
              {is2FALoading
                ? "Gerando..."
                : "Configurar autenticação em dois fatores"}
            </button>
          )}

          {step === "idle" && isCurrentlyEnabled && (
            <button
              onClick={() => setStep("disable_confirm")}
              className="w-full sm:w-auto px-5 py-2.5 bg-red-500/10 hover:bg-red-500/20 text-red-500 text-sm font-bold rounded-xl transition-all shrink-0"
            >
              Desativar
            </button>
          )}
        </div>

        {step === "configuring" && setupData && (
          <div className="mt-8 pt-8 border-t border-subtle/20 animate-fade-in">
            <h4 className="text-sm font-bold text-primary mb-4">
              Configure seu autenticador
            </h4>
            <div className="flex flex-col sm:flex-row gap-8 items-start">
              <div className="p-4 bg-white border border-subtle/30 rounded-2xl shadow-sm shrink-0">
                {setupData?.qrCodeUrl ? (
                  setupData.qrCodeUrl.startsWith("data:image") ? (
                    <img
                      src={setupData.qrCodeUrl}
                      alt="QR Code do aplicativo autenticador"
                      className="w-150px h-150px object-contain rounded-lg"
                    />
                  ) : (
                    <QRCodeSVG value={setupData.qrCodeUrl} size={150} />
                  )
                ) : (
                  <div className="w-150px h-150px bg-subtle/20 animate-pulse rounded-xl flex items-center justify-center">
                    <span className="text-muted text-xs font-bold">
                      Gerando...
                    </span>
                  </div>
                )}
              </div>
              <div className="flex-1 w-full space-y-4">
                <div>
                  <span className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5">
                    Chave manual
                  </span>
                  <div className="flex items-center gap-2">
                    <code className="flex-1 px-3 py-2 bg-surface text-primary border border-subtle/30 rounded-xl text-sm font-mono tracking-widest truncate">
                      {setupData.secret}
                    </code>
                    <button
                      onClick={() => copyToClipboard(setupData.secret)}
                      className="p-2 text-muted hover:text-brand bg-surface border border-subtle/30 rounded-xl transition-colors shrink-0"
                    >
                      <Copy size={16} />
                    </button>
                  </div>
                </div>
                <form
                  onSubmit={handleTotpSubmit(handleEnable)}
                  className="pt-2"
                >
                  <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5">
                    Código do aplicativo autenticador
                  </label>
                  <input
                    {...registerTotp("token")}
                    maxLength={6}
                    placeholder="000000"
                    className="w-full max-w-200px rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-center font-mono text-lg tracking-[0.5em]"
                  />
                  {totpErrors.token && (
                    <span className="text-red-500 text-xs mt-1 block font-medium">
                      {totpErrors.token.message}
                    </span>
                  )}

                  <div className="flex gap-3 mt-4">
                    <button
                      type="submit"
                      disabled={is2FALoading}
                      className="px-5 py-2.5 bg-brand hover:bg-brand-light text-white text-sm font-bold rounded-xl transition-all shadow-sm disabled:opacity-70"
                    >
                      {is2FALoading ? "Validando..." : "Ativar"}
                    </button>
                    <button
                      type="button"
                      onClick={handleCancel2FA}
                      className="px-5 py-2.5 text-secondary hover:text-primary hover:bg-subtle/20 rounded-xl text-sm font-bold transition-all"
                    >
                      Cancelar
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}

        {step === "recovery_display" && recoveryCodes && (
          <div className="mt-8 pt-8 border-t border-subtle/20 animate-fade-in">
            <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-4 mb-6 flex gap-3">
              <AlertTriangle className="w-6 h-6 text-amber-500 shrink-0" />
              <div>
                <h4 className="text-sm font-bold text-amber-500">
                  Guarde seus códigos de recuperação em um local seguro
                </h4>
                <p className="text-xs font-medium text-amber-500/80 mt-1">
                  Cada código pode ser utilizado apenas uma única vez. Copie-os
                  e guarde em um local seguro. Você precisará deles caso perca o
                  acesso ao aplicativo autenticador.
                </p>
              </div>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-6">
              {recoveryCodes.map((code) => (
                <div
                  key={code}
                  className="p-3 bg-surface border border-subtle/30 rounded-xl text-center font-mono text-sm tracking-wider text-primary shadow-xs"
                >
                  {code}
                </div>
              ))}
            </div>
            <div className="flex flex-col sm:flex-row gap-3">
              <button
                onClick={() => copyToClipboard(recoveryCodes.join("\n"))}
                className="flex-1 sm:flex-none justify-center px-5 py-2.5 bg-primary text-surface text-sm font-bold rounded-xl transition-all flex items-center gap-2 hover:opacity-90"
              >
                <Copy size={16} /> Copiar códigos
              </button>
              <button
                onClick={closeRecovery}
                className="px-5 py-2.5 text-secondary hover:text-primary hover:bg-subtle/20 rounded-xl text-sm font-bold transition-all"
              >
                Estou ciente e já guardei
              </button>
            </div>
          </div>
        )}

        {step === "disable_confirm" && (
          <div className="mt-8 pt-8 border-t border-subtle/20 animate-fade-in">
            <h4 className="text-sm font-bold text-primary mb-2">
              Desativar autenticação em dois fatores
            </h4>
            <p className="text-xs font-medium text-muted mb-5 max-w-sm">
              Confirme sua senha atual para remover essa camada de segurança.
            </p>

            <form
              onSubmit={handleDisableSubmit(handleDisable)}
              className="max-w-xs space-y-4"
            >
              <div>
                <input
                  type="password"
                  {...registerDisable("password")}
                  placeholder="Sua senha atual"
                  className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm"
                />
                {disableErrors.password && (
                  <span className="text-red-500 text-xs mt-1 block font-medium">
                    {disableErrors.password.message}
                  </span>
                )}
              </div>
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={is2FALoading}
                  className="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white text-sm font-bold rounded-xl transition-all shadow-sm disabled:opacity-70"
                >
                  {is2FALoading ? "Aguarde..." : "Desativar"}
                </button>
                <button
                  type="button"
                  onClick={handleCancel2FA}
                  disabled={is2FALoading}
                  className="px-5 py-2.5 text-secondary hover:text-primary hover:bg-subtle/20 rounded-xl text-sm font-bold transition-all"
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
