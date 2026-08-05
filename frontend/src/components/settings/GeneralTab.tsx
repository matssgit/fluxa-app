import { useState } from "react";
import toast from "react-hot-toast";
import { api } from "../../api/client";
import { useAuth } from "../../hooks/useAuth";
import { EyeOff, Bell, Settings as SettingsIcon, Download } from "lucide-react";

export function GeneralTab() {
  const { user, updatePreferences } = useAuth();
  const [isExporting, setIsExporting] = useState(false);

  const togglePrivacy = async () => {
    const currentHideBalance =
      user?.preferences?.privacy?.hide_balance ?? false;
    try {
      await updatePreferences({
        privacy: { hide_balance: !currentHideBalance },
      });
      toast.success("Privacidade atualizada!");
    } catch (error) {
      toast.error("Erro ao atualizar preferência");
      console.error(error);
    }
  };

  const toggleNotification = async (
    key: "reminders_enabled" | "subscriptions_enabled",
  ) => {
    const currentNotifications = user?.preferences?.notifications || {
      reminders_enabled: false,
      subscriptions_enabled: false,
    };
    const currentStatus = currentNotifications[key];

    try {
      await updatePreferences({
        notifications: {
          reminders_enabled: currentNotifications.reminders_enabled,
          subscriptions_enabled: currentNotifications.subscriptions_enabled,
          [key]: !currentStatus,
        },
      });

      if (!currentStatus)
        toast.success(
          "Notificações ativadas! (O navegador poderá pedir permissão)",
        );
      else toast.success("Notificações desativadas.");
    } catch (error) {
      toast.error("Erro ao atualizar notificações");
      console.error(error);
    }
  };

  const handleExport = async () => {
    try {
      setIsExporting(true);
      toast.loading("A gerar o seu ficheiro CSV...", { id: "export" });
      const response = await api.get("/users/export", { responseType: "blob" });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        `fluxa-transacoes-${new Date().toISOString().split("T")[0]}.csv`,
      );
      document.body.appendChild(link);
      link.click();
      link.remove();

      toast.success("Histórico exportado com sucesso!", { id: "export" });
    } catch (error) {
      toast.error("Erro ao exportar o histórico", { id: "export" });
      console.error(error);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-10 animate-fade-in max-w-2xl">
      <div>
        <h2 className="text-lg font-bold text-primary">Geral</h2>
        <p className="text-xs sm:text-sm text-muted mt-1">
          Gerencie a privacidade, notificações e dados da sua conta.
        </p>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm font-bold text-primary border-b border-subtle/20 pb-2">
          Privacidade
        </h3>
        <div className="flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30">
          <div className="flex items-center gap-3 pr-4">
            <div className="p-2 rounded-xl bg-elevated text-secondary">
              <EyeOff size={18} />
            </div>
            <div>
              <span className="block text-sm font-bold text-primary">
                Ocultar saldos ao abrir o app
              </span>
              <span className="block text-[11px] font-medium text-muted mt-0.5">
                O Dashboard e as contas iniciarão censurados
              </span>
            </div>
          </div>
          <label className="relative inline-flex items-center cursor-pointer shrink-0">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={user?.preferences?.privacy?.hide_balance || false}
              onChange={togglePrivacy}
            />
            <div className="w-11 h-6 bg-subtle/30 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand"></div>
          </label>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm font-bold text-primary border-b border-subtle/20 pb-2">
          Notificações
        </h3>

        <div className="flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30">
          <div className="flex items-center gap-3 pr-4">
            <div className="p-2 rounded-xl bg-elevated text-secondary">
              <Bell size={18} />
            </div>
            <div>
              <span className="block text-sm font-bold text-primary">
                Lembretes de vencimento
              </span>
              <span className="block text-[11px] font-medium text-muted mt-0.5">
                Seja avisado quando faturas ou contas estiverem próximas de
                vencer
              </span>
            </div>
          </div>
          <label className="relative inline-flex items-center cursor-pointer shrink-0">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={
                user?.preferences?.notifications?.reminders_enabled || false
              }
              onChange={() => toggleNotification("reminders_enabled")}
            />
            <div className="w-11 h-6 bg-subtle/30 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand"></div>
          </label>
        </div>

        <div className="flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30">
          <div className="flex items-center gap-3 pr-4">
            <div className="p-2 rounded-xl bg-elevated text-secondary">
              <SettingsIcon size={18} />
            </div>
            <div>
              <span className="block text-sm font-bold text-primary">
                Avisos de assinaturas
              </span>
              <span className="block text-[11px] font-medium text-muted mt-0.5">
                Notificar quando uma assinatura for cobrada amanhã
              </span>
            </div>
          </div>
          <label className="relative inline-flex items-center cursor-pointer shrink-0">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={
                user?.preferences?.notifications?.subscriptions_enabled || false
              }
              onChange={() => toggleNotification("subscriptions_enabled")}
            />
            <div className="w-11 h-6 bg-subtle/30 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand"></div>
          </label>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm font-bold text-primary border-b border-subtle/20 pb-2">
          Seus Dados
        </h3>
        <button
          onClick={handleExport}
          disabled={isExporting}
          className="w-full flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-surface hover:border-subtle/50 transition-all cursor-pointer group disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-elevated text-secondary group-hover:text-primary transition-colors">
              <Download
                size={18}
                className={isExporting ? "animate-bounce text-brand" : ""}
              />
            </div>
            <div className="text-left">
              <span className="block text-sm font-bold text-primary">
                Exportar histórico
              </span>
              <span className="block text-[11px] font-medium text-muted mt-0.5">
                {isExporting
                  ? "A transferir os seus lançamentos..."
                  : "Baixe todas as suas transações em CSV"}
              </span>
            </div>
          </div>
        </button>
      </div>
    </div>
  );
}
