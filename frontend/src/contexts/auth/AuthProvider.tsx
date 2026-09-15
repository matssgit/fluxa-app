import { useEffect, useState } from "react";
import { api } from "../../api/client";
import type { ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import type { User, UserPreferences } from "./AuthContext";
import { AvatarStorage } from "../../lib/storage/AvatarStorage";
import {
  AUTH_SESSION_CLEARED_EVENT,
  clearClientSession,
} from "../../lib/query-client";
import {
  login as apiLogin,
  register as apiRegister,
  verify2FA,
  recovery2FA,
  type LoginCredentials,
  type RegisterCredentials,
} from "../../services/auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(() =>
    Boolean(localStorage.getItem("@FinanceApp:token")),
  );

  useEffect(() => {
    let active = true;
    const token = localStorage.getItem("@FinanceApp:token");

    const handleSessionCleared = () => {
      if (!active) return;
      setUser(null);
      setIsLoading(false);
    };

    window.addEventListener(AUTH_SESSION_CLEARED_EVENT, handleSessionCleared);

    if (token) {
      void api
        .get<{ user: User }>("/users/me")
        .then(({ data }) => {
          if (!active) return;

          const validatedUser = data.user;
          validatedUser.avatar_url = AvatarStorage.load(
            validatedUser.avatar_url,
          );
          const userToSave = { ...validatedUser };
          if (AvatarStorage.isLocalBase64(userToSave.avatar_url)) {
            userToSave.avatar_url = "local_cache";
          }
          localStorage.setItem("@FinanceApp:user", JSON.stringify(userToSave));
          setUser(validatedUser);
        })
        .catch(() => {
          if (active) setUser(null);
        })
        .finally(() => {
          if (active) setIsLoading(false);
        });
    }

    return () => {
      active = false;
      window.removeEventListener(
        AUTH_SESSION_CLEARED_EVENT,
        handleSessionCleared,
      );
    };
  }, []);

  async function signIn(
    data: LoginCredentials,
  ): Promise<{ requiresTwoFactor: true; tempToken: string } | void> {
    let response;

    if (data.twoFactorType === "totp" && data.twoFactorCode && data.tempToken) {
      response = await verify2FA(data.twoFactorCode, data.tempToken);
    } else if (
      data.twoFactorType === "recovery" &&
      data.twoFactorCode &&
      data.tempToken
    ) {
      response = await recovery2FA(data.twoFactorCode, data.tempToken);
    } else {
      response = await apiLogin(data);
      if ("requiresTwoFactor" in response && response.requiresTwoFactor) {
        return response;
      }
    }

    const { token, user: loggedUser } = response as {
      token: string;
      user: User;
    };

    await clearClientSession();
    loggedUser.avatar_url = AvatarStorage.load(loggedUser.avatar_url);
    localStorage.setItem("@FinanceApp:token", token);

    const userToSave = { ...loggedUser };
    if (AvatarStorage.isLocalBase64(userToSave.avatar_url)) {
      userToSave.avatar_url = "local_cache";
    }
    localStorage.setItem("@FinanceApp:user", JSON.stringify(userToSave));

    setUser(loggedUser);
  }

  async function signUp(data: RegisterCredentials) {
    await apiRegister(data);
  }

  async function signOut() {
    await clearClientSession();
    setUser(null);
  }

  async function updateProfile(data: {
    name: string;
    avatar_url?: string | null;
  }) {
    let dbAvatarUrl = data.avatar_url;

    if (AvatarStorage.isLocalBase64(data.avatar_url)) {
      dbAvatarUrl = AvatarStorage.save(data.avatar_url as string);
    }

    await api.put("/users/profile", {
      name: data.name,
      avatar_url: dbAvatarUrl,
    });

    setUser((prev: User | null) => {
      if (!prev) return prev;

      const updatedUser: User = {
        ...prev,
        ...data,
        avatar_url: data.avatar_url,
      };
      const userToSave = { ...updatedUser, avatar_url: dbAvatarUrl };
      localStorage.setItem("@FinanceApp:user", JSON.stringify(userToSave));

      return updatedUser;
    });
  }

  async function updatePreferences(preferences: Partial<UserPreferences>) {
    const response = await api.put<{
      message: string;
      preferences: UserPreferences;
    }>("/users/preferences", preferences);

    setUser((prev: User | null) => {
      if (!prev) return prev;
      const updatedUser: User = {
        ...prev,
        preferences: response.data.preferences,
      };

      const userToSave = { ...updatedUser };
      if (AvatarStorage.isLocalBase64(userToSave.avatar_url)) {
        userToSave.avatar_url = "local_cache";
      }
      localStorage.setItem("@FinanceApp:user", JSON.stringify(userToSave));

      return updatedUser;
    });
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        signIn,
        signUp,
        signOut,
        updateProfile,
        updatePreferences,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
