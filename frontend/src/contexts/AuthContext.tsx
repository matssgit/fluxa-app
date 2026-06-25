import { createContext } from "react";
import type {
   User,
   LoginCredentials,
   RegisterCredentials,
} from "../services/auth";

export interface AuthContextData {
   user: User | null;
   isAuthenticated: boolean;
   signIn: (data: LoginCredentials) => Promise<void>;
   signUp: (data: RegisterCredentials) => Promise<void>;
   signOut: () => void;
}

export const AuthContext = createContext<AuthContextData>(
   {} as AuthContextData,
);
