import toast from "react-hot-toast";
import { useState, useRef } from "react";
import { useAuth } from "../../hooks/useAuth";
import { Camera, Mail, UserCircle } from "lucide-react";

export function ProfileTab() {
  const { user, updateProfile } = useAuth();
  const [profileName, setProfileName] = useState(user?.name || "");
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(
    user?.avatar_url || null,
  );
  const fileInputRef = useRef<HTMLInputElement>(null);

  const getUserInitials = (name?: string): string => {
    if (!name) return "US";
    const parts = name.trim().split(" ");
    if (parts.length >= 2)
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    return name.substring(0, 2).toUpperCase();
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

  return (
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
            O e-mail é utilizado para segurança e login e não pode ser alterado
            diretamente.
          </p>
        </div>
      </div>

      <div className="pt-4 flex flex-col sm:flex-row gap-3">
        <button
          onClick={handleSaveProfile}
          disabled={
            isSavingProfile || (profileName === user?.name && !avatarFile)
          }
          className="w-full sm:w-auto px-6 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSavingProfile ? "Salvando..." : "Salvar Alterações"}
        </button>
      </div>
    </div>
  );
}
