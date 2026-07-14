import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DefaultLayout } from "./layouts/DefaultLayout";
import { ThemeProvider } from "./contexts/ThemeProvider";
import { AuthProvider } from "./contexts/AuthProvider";
import { Subscriptions } from "./pages/Subscriptions";
import { Dashboard } from "./pages/Dashboard/index";
import { Transactions } from "./pages/Transactions";
import { CreditCards } from "./pages/CreditCards";
import { Accounts } from "./pages/Accounts/index";
import { Settings } from "./pages/Settings";
import { Register } from "./pages/Register";
import { Wallets } from "./pages/Wallets";
import { useAuth } from "./hooks/useAuth";
import { Toaster } from "react-hot-toast";
import { Login } from "./pages/Login";

const queryClient = new QueryClient();

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Rotas Públicas */}
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

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
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
