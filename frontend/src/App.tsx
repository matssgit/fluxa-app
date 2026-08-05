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
import { Transactions } from "./pages/Transactions";
import { Dashboard } from "./pages/Dashboard/index";
import { Subscriptions } from "./pages/Subscriptions";
import { AuthProvider } from "./contexts/auth/AuthProvider";
import { DefaultLayout } from "./layouts/DefaultLayout";
import { ThemeProvider } from "./contexts/theme/ThemeProvider";
import { VerifyEmailPending } from "./pages/VerifyEmailPending";
import { PrivacyProvider } from "./contexts/privacy/PrivacyProvider";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

const queryClient = new QueryClient();

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
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
                  background: "#13312A",
                  color: "#FFFFFF",
                  border: "1px solid rgba(255, 255, 255, 0.15)",
                  borderRadius: "12px",
                  fontSize: "14px",
                  fontWeight: "500",
                  boxShadow:
                    "0 10px 15px -3px rgba(0, 0, 0, 0.3), 0 4px 6px -4px rgba(0, 0, 0, 0.2)",
                },
                success: {
                  iconTheme: {
                    primary: "#10B981",
                    secondary: "#13312A",
                  },
                },
                error: {
                  iconTheme: {
                    primary: "#EF4444",
                    secondary: "#FFFFFF",
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
