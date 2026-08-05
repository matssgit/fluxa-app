import { useState } from "react";
import { useAuth } from "../hooks/useAuth";
import { AboutTab } from "../components/settings/AboutTab";
import { ProfileTab } from "../components/settings/ProfileTab";
import { GeneralTab } from "../components/settings/GeneralTab";
import { SecurityTab } from "../components/settings/SecurityTab";
import { AppearanceTab } from "../components/settings/AppearanceTab";
import {
  User as UserIcon,
  Palette,
  Settings as SettingsIcon,
  Info,
  LogOut,
  Key,
} from "lucide-react";

type SettingsTab = "profile" | "security" | "appearance" | "general" | "about";

export function Settings() {
  const { signOut } = useAuth();
  const [activeTab, setActiveTab] = useState<SettingsTab>("profile");

  const tabs = [
    {
      id: "profile",
      label: "Perfil",
      icon: UserIcon,
      description: "Seus dados pessoais",
    },
    {
      id: "security",
      label: "Segurança",
      icon: Key,
      description: "Senha e autenticação em duas etapas",
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
      description: "Versão e detalhes do projeto",
    },
  ] as const;

  const renderActiveTab = () => {
    switch (activeTab) {
      case "profile":
        return <ProfileTab />;
      case "security":
        return <SecurityTab />;
      case "appearance":
        return <AppearanceTab />;
      case "general":
        return <GeneralTab />;
      case "about":
        return <AboutTab />;
      default:
        return <ProfileTab />;
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
                      className={`text-sm font-bold ${
                        isActive ? "text-brand" : "text-primary"
                      }`}
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
          {renderActiveTab()}
        </main>
      </div>
    </div>
  );
}
