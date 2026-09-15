import { Login } from "./pages/Login";
import { useAuth } from "./hooks/useAuth";
import { Toaster } from "react-hot-toast";
import { Wallets } from "./pages/Wallets";
import { Register } from "./pages/Register";
import { Settings } from "./pages/Settings";
import { Categories } from "./pages/Categories";
import { Accounts } from "./pages/Accounts/index";
import { VerifyEmail } from "./pages/VerifyEmail";
import { CreditCards } from "./pages/CreditCards";
import { Dashboard } from "./pages/Dashboard/index";
import { Transactions } from "./pages/Transactions";
import { ResetPassword } from "./pages/ResetPassword";
import { Subscriptions } from "./pages/Subscriptions";
import { ForgotPassword } from "./pages/ForgotPassword";
import { DefaultLayout } from "./layouts/DefaultLayout";
import { AuthProvider } from "./contexts/auth/AuthProvider";
import { ThemeProvider } from "./contexts/theme/ThemeProvider";
import { VerifyEmailPending } from "./pages/VerifyEmailPending";
import { PrivacyProvider } from "./contexts/privacy/PrivacyProvider";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "./lib/query-client";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ThemeProvider>
          <PrivacyProvider>
            <BrowserRouter>
              <Routes>
                {/* Rotas Públicas */}
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/verify-email" element={<VerifyEmail />} />
                <Route
                  path="/verify-email-pending"
                  element={<VerifyEmailPending />}
                />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password" element={<ResetPassword />} />

                {/* Rotas Privadas Agrupadas no Layout */}
                <Route
                  element={
                    <PrivateRoute>
                      <DefaultLayout />
                    </PrivateRoute>
                  }
                >
                  <Route
                    path="/"
                    element={<Navigate to="/dashboard" replace />}
                  />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/transactions" element={<Transactions />} />
                  <Route path="/subscriptions" element={<Subscriptions />} />
                  <Route path="/categories" element={<Categories />} />
                  <Route path="/cards" element={<CreditCards />} />
                  <Route path="/accounts" element={<Accounts />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="/wallets" element={<Wallets />} />
                  <Route
                    path="*"
                    element={<Navigate to="/dashboard" replace />}
                  />
                </Route>
              </Routes>
            </BrowserRouter>

            <Toaster
              position="top-right"
              toastOptions={{
                duration: 3500,
                style: {
                  background: "var(--color-surface)",
                  color: "var(--color-primary)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "18px",
                  fontSize: "14px",
                  fontWeight: "500",
                  boxShadow: "var(--shadow-md)",
                },
                success: {
                  iconTheme: {
                    primary: "var(--color-income)",
                    secondary: "var(--color-surface)",
                  },
                },
                error: {
                  iconTheme: {
                    primary: "var(--color-expense)",
                    secondary: "var(--color-surface)",
                  },
                },
              }}
            />
          </PrivacyProvider>
        </ThemeProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
