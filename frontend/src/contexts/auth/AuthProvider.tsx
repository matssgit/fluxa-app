import { useState } from "react";
import { api } from "../../api/client";
import type { ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import type { User, UserPreferences } from "./AuthContext";
import { AvatarStorage } from "../../lib/storage/AvatarStorage";
import {
  login as apiLogin,
  register as apiRegister,
  verify2FA,
  recovery2FA,
  type LoginCredentials,
  type RegisterCredentials,
} from "../../services/auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const token = localStorage.getItem("@FinanceApp:token");
    const storedUser = localStorage.getItem("@FinanceApp:user");

    if (token && storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser) as User;
        parsedUser.avatar_url = AvatarStorage.load(parsedUser.avatar_url);
        return parsedUser;
      } catch {
        return null;
      }
    }
    return null;
  });

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

  function signOut() {
    localStorage.removeItem("@FinanceApp:token");
    localStorage.removeItem("@FinanceApp:user");
    AvatarStorage.clear();
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
