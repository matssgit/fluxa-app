import { useState, useRef } from "react";
import {
  User as UserIcon,
  Palette,
  Settings as SettingsIcon,
  Info,
  LogOut,
  Camera,
  Mail,
  UserCircle,
  Sun,
  Moon,
  Monitor,
  EyeOff,
  Bell,
  Download,
  Shield,
  FileText,
} from "lucide-react";
import toast from "react-hot-toast";
import { api } from "../api/client";

import { useAuth } from "../hooks/useAuth";
import { useTheme } from "../hooks/useTheme";

type SettingsTab = "profile" | "appearance" | "general" | "about";

export function Settings() {
  const { user, signOut, updateProfile, updatePreferences } = useAuth();
  const { theme, setTheme } = useTheme();
  const [isExporting, setIsExporting] = useState(false);

  const [activeTab, setActiveTab] = useState<SettingsTab>("profile");

  const [profileName, setProfileName] = useState(user?.name || "");
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(
    user?.avatar_url || null,
  );

  const tabs = [
    {
      id: "profile",
      label: "Perfil",
      icon: UserIcon,
      description: "Seus dados pessoais",
    },
    {
      id: "appearance",
      label: "Aparência",
      icon: Palette,
      description: "Tema da interface",
    },
    {
      id: "general",
      label: "Geral",
      icon: SettingsIcon,
      description: "Privacidade e notificações",
    },
    {
      id: "about",
      label: "Sobre",
      icon: Info,
      description: "Versão e termos legais",
    },
  ] as const;

  const getUserInitials = (name?: string): string => {
    if (!name) return "US";
    const parts = name.trim().split(" ");
    if (parts.length >= 2)
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    return name.substring(0, 2).toUpperCase();
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

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        toast.error("A imagem deve ter no máximo 5MB");
        return;
      }
      setAvatarFile(file);
      setAvatarPreview(URL.createObjectURL(file));
    }
  };

  const handleSaveProfile = async () => {
    if (profileName.trim().length < 2) {
      toast.error("O nome deve ter pelo menos 2 caracteres");
      return;
    }
    try {
      setIsSavingProfile(true);
      let finalAvatarUrl = user?.avatar_url;

      if (avatarFile) {
        finalAvatarUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result as string);
          reader.readAsDataURL(avatarFile);
        });
      }

      await updateProfile({ name: profileName, avatar_url: finalAvatarUrl });
      toast.success("Perfil atualizado com sucesso!");
      setAvatarFile(null);
    } catch (error) {
      toast.error("Erro ao atualizar perfil");
      console.error(error);
    } finally {
      setIsSavingProfile(false);
    }
  };

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

  return (
    <div className="max-w-5xl mx-auto w-full p-0 sm:p-6 lg:p-8 animate-fade-in flex flex-col min-h-screen sm:min-h-0">
      <div className="pt-6 px-4 sm:px-0 sm:pt-0 mb-6">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
          Configurações
        </h1>
        <p className="text-xs sm:text-sm font-medium text-muted mt-1">
          Gerencie sua conta e a sua experiência no Fluxa.
        </p>
      </div>

      <div className="flex flex-col md:flex-row gap-2 sm:gap-6 lg:gap-10 flex-1">
        <aside className="w-full md:w-64 shrink-0 px-4 sm:px-0">
          <nav className="flex md:flex-col gap-2 overflow-x-auto md:overflow-visible pb-3 md:pb-0 snap-x snap-mandatory [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] scrollbar-none">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;

              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as SettingsTab)}
                  className={`snap-start shrink-0 md:w-full flex items-center gap-3 px-4 py-2.5 sm:p-3 rounded-2xl transition-all cursor-pointer text-left ${
                    isActive
                      ? "bg-surface shadow-sm border border-subtle/30"
                      : "bg-transparent hover:bg-elevated/50 border border-transparent"
                  }`}
                >
                  <div
                    className={`p-2 rounded-xl transition-colors ${
                      isActive
                        ? "bg-brand/10 text-brand"
                        : "bg-elevated text-muted"
                    }`}
                  >
                    <Icon size={18} />
                  </div>
                  <div className="whitespace-nowrap">
                    <span
                      className={`text-sm font-bold ${isActive ? "text-brand" : "text-primary"}`}
                    >
                      {tab.label}
                    </span>
                    <span className="hidden md:block text-[11px] font-medium text-muted mt-0.5">
                      {tab.description}
                    </span>
                  </div>
                </button>
              );
            })}

            <div className="hidden md:block h-px w-full bg-subtle/20 my-4" />

            <button
              onClick={signOut}
              className="hidden md:flex items-center gap-3 w-full p-3 rounded-2xl hover:bg-red-500/10 text-red-500 transition-colors text-left cursor-pointer group"
            >
              <div className="p-2 rounded-xl bg-red-500/10 group-hover:bg-red-500/20 transition-colors">
                <LogOut size={18} />
              </div>
              <span className="text-sm font-bold">Sair da conta</span>
            </button>
          </nav>
        </aside>

        <main className="flex-1 bg-surface sm:rounded-3xl sm:shadow-sm sm:border sm:border-subtle/30 border-t border-subtle/20 sm:border-t-0 p-5 sm:p-8 min-h-[50vh]">
          {/* ABA 1: PERFIL */}
          {activeTab === "profile" && (
            <div className="space-y-8 animate-fade-in max-w-2xl">
              <div>
                <h2 className="text-lg font-bold text-primary">Seu Perfil</h2>
                <p className="text-xs sm:text-sm text-muted mt-1">
                  Atualize sua foto e informações pessoais.
                </p>
              </div>

              <div className="flex flex-col sm:flex-row items-center sm:items-center gap-5 sm:gap-6 p-5 sm:p-6 rounded-3xl bg-elevated/40 border border-subtle/30">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleImageChange}
                  accept="image/jpeg, image/png, image/webp"
                  className="hidden"
                />

                <div
                  className="relative group cursor-pointer shrink-0"
                  onClick={() => fileInputRef.current?.click()}
                >
                  {avatarPreview ? (
                    <img
                      src={avatarPreview}
                      alt="Avatar"
                      className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl object-cover shadow-md border-2 border-surface transition-transform group-hover:scale-105"
                    />
                  ) : (
                    <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-linear-to-tr from-brand to-emerald-600 text-white font-extrabold text-2xl sm:text-3xl flex items-center justify-center shadow-md border-2 border-surface transition-transform group-hover:scale-105">
                      {getUserInitials(user?.name)}
                    </div>
                  )}
                  <div className="absolute -bottom-2 -right-2 w-8 h-8 bg-surface rounded-xl border border-subtle/30 shadow-sm flex items-center justify-center text-muted group-hover:text-brand transition-colors">
                    <Camera size={14} />
                  </div>
                </div>

                <div className="text-center sm:text-left">
                  <h3 className="text-base sm:text-lg font-bold text-primary">
                    {user?.name || "Usuário"}
                  </h3>
                  <p className="text-xs sm:text-sm font-medium text-muted mt-0.5">
                    {user?.email}
                  </p>
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="mt-3 text-[11px] font-bold text-brand uppercase tracking-wider hover:underline cursor-pointer"
                  >
                    Trocar Foto
                  </button>
                </div>
              </div>

              <div className="space-y-5">
                <div>
                  <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                    Nome Completo
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <UserCircle size={16} className="text-muted" />
                    </div>
                    <input
                      type="text"
                      value={profileName}
                      onChange={(e) => setProfileName(e.target.value)}
                      className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                    Endereço de E-mail
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Mail size={16} className="text-muted" />
                    </div>
                    <input
                      type="email"
                      value={user?.email || ""}
                      disabled
                      className="w-full rounded-xl border border-subtle/30 pl-11 pr-4 py-3 bg-elevated/20 text-muted outline-none transition-all text-sm font-medium cursor-not-allowed"
                    />
                  </div>
                  <p className="text-[11px] font-medium text-muted mt-1.5 pl-1">
                    O e-mail é utilizado para segurança e login e não pode ser
                    alterado diretamente.
                  </p>
                </div>
              </div>

              <div className="pt-4 flex flex-col sm:flex-row gap-3">
                <button
                  onClick={handleSaveProfile}
                  disabled={
                    isSavingProfile ||
                    (profileName === user?.name && !avatarFile)
                  }
                  className="w-full sm:w-auto px-6 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSavingProfile ? "Salvando..." : "Salvar Alterações"}
                </button>
              </div>
            </div>
          )}

          {/* ABA 2: APARÊNCIA */}
          {activeTab === "appearance" && (
            <div className="space-y-8 animate-fade-in max-w-2xl">
              <div>
                <h2 className="text-lg font-bold text-primary">Aparência</h2>
                <p className="text-xs sm:text-sm text-muted mt-1">
                  Personalize a interface do Fluxa para o seu conforto visual.
                </p>
              </div>

              <div className="space-y-5">
                <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  Tema da Interface
                </label>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
                  <button
                    onClick={() => setTheme("light")}
                    className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
                      theme === "light"
                        ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                        : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
                    }`}
                  >
                    <div
                      className={`p-2 rounded-xl mb-3 transition-colors ${theme === "light" ? "bg-brand text-white" : "bg-elevated text-muted"}`}
                    >
                      <Sun size={20} />
                    </div>
                    <span
                      className={`text-sm font-bold block ${theme === "light" ? "text-primary" : "text-secondary"}`}
                    >
                      Claro
                    </span>
                    <span className="text-[11px] font-medium text-muted mt-0.5 block">
                      Fundo branco e limpo
                    </span>
                  </button>

                  <button
                    onClick={() => setTheme("dark")}
                    className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
                      theme === "dark"
                        ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                        : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
                    }`}
                  >
                    <div
                      className={`p-2 rounded-xl mb-3 transition-colors ${theme === "dark" ? "bg-brand text-white" : "bg-elevated text-muted"}`}
                    >
                      <Moon size={20} />
                    </div>
                    <span
                      className={`text-sm font-bold block ${theme === "dark" ? "text-primary" : "text-secondary"}`}
                    >
                      Escuro
                    </span>
                    <span className="text-[11px] font-medium text-muted mt-0.5 block">
                      Modo Pine & Sage
                    </span>
                  </button>

                  <button
                    onClick={() => setTheme("system")}
                    className={`flex flex-col items-start p-4 rounded-2xl border transition-all cursor-pointer text-left ${
                      theme === "system"
                        ? "bg-brand/5 border-brand ring-1 ring-brand shadow-sm"
                        : "bg-elevated/40 border-subtle/30 hover:bg-surface hover:border-subtle/50"
                    }`}
                  >
                    <div
                      className={`p-2 rounded-xl mb-3 transition-colors ${theme === "system" ? "bg-brand text-white" : "bg-elevated text-muted"}`}
                    >
                      <Monitor size={20} />
                    </div>
                    <div className="flex items-center gap-2">
                      <span
                        className={`text-sm font-bold block ${theme === "system" ? "text-primary" : "text-secondary"}`}
                      >
                        Sistema
                      </span>
                    </div>
                    <span className="text-[11px] font-medium text-muted mt-0.5 block">
                      Segue o dispositivo
                    </span>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ABA 3: GERAL */}
          {activeTab === "general" && (
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
                      checked={
                        user?.preferences?.privacy?.hide_balance || false
                      }
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
                        Seja avisado quando faturas ou contas estiverem próximas
                        de vencer
                      </span>
                    </div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer shrink-0">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={
                        user?.preferences?.notifications?.reminders_enabled ||
                        false
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
                        user?.preferences?.notifications
                          ?.subscriptions_enabled || false
                      }
                      onChange={() =>
                        toggleNotification("subscriptions_enabled")
                      }
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
                        className={
                          isExporting ? "animate-bounce text-brand" : ""
                        }
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
          )}

          {/* ABA 4: SOBRE */}
          {activeTab === "about" && (
            <div className="space-y-8 animate-fade-in max-w-2xl">
              <div>
                <h2 className="text-lg font-bold text-primary">
                  Sobre o Fluxa
                </h2>
                <p className="text-xs sm:text-sm text-muted mt-1">
                  Informações legais e versão do sistema.
                </p>
              </div>

              <div className="flex flex-col items-center justify-center p-8 rounded-3xl bg-elevated/20 border border-subtle/30 text-center">
                <div className="w-16 h-16 rounded-2xl bg-brand text-white flex items-center justify-center shadow-lg mb-4">
                  <span className="text-2xl font-black italic">F</span>
                </div>
                <h3 className="text-xl font-extrabold text-primary tracking-tight">
                  Fluxa
                </h3>
                <p className="text-sm font-medium text-muted mt-1">
                  Versão 1.0.0-beta
                </p>
                <p className="text-[11px] text-muted/80 mt-4">
                  Projetado com excelência no Brasil.
                </p>
              </div>

              <div className="space-y-3">
                <button className="w-full flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-surface hover:border-subtle/50 transition-all cursor-pointer">
                  <div className="flex items-center gap-3">
                    <Shield size={18} className="text-secondary" />
                    <span className="text-sm font-bold text-primary">
                      Política de Privacidade
                    </span>
                  </div>
                </button>
                <button className="w-full flex items-center justify-between p-4 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-surface hover:border-subtle/50 transition-all cursor-pointer">
                  <div className="flex items-center gap-3">
                    <FileText size={18} className="text-secondary" />
                    <span className="text-sm font-bold text-primary">
                      Termos de Serviço
                    </span>
                  </div>
                </button>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
