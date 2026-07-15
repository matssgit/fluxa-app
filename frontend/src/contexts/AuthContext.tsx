import { createContext } from "react";
import type { LoginCredentials, RegisterCredentials } from "../services/auth";

export interface UserPreferences {
  theme: "light" | "dark" | "system";
  privacy: {
    hide_balance: boolean;
  };
  notifications: {
    reminders_enabled: boolean;
    subscriptions_enabled: boolean;
  };
}

export interface User {
  id: string;
  name: string;
  email: string;
  avatar_url?: string | null;
  preferences: UserPreferences;
}

export interface AuthContextData {
  user: User | null;
  isAuthenticated: boolean;
  signIn: (data: LoginCredentials) => Promise<void>;
  signUp: (data: RegisterCredentials) => Promise<void>;
  signOut: () => void;
  updateProfile: (data: {
    name: string;
    avatar_url?: string | null;
  }) => Promise<void>;
  updatePreferences: (data: Partial<UserPreferences>) => Promise<void>;
}

export const AuthContext = createContext<AuthContextData>(
  {} as AuthContextData,
);
